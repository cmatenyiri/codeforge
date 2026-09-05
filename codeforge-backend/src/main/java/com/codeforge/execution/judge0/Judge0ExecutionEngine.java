package com.codeforge.execution.judge0;

import com.codeforge.domain.Language;
import com.codeforge.domain.SubmissionStatus;
import com.codeforge.execution.ExecutionEngine;
import com.codeforge.execution.ExecutionException;
import com.codeforge.execution.ExecutionRequest;
import com.codeforge.execution.ExecutionResult;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Runs code on a <a href="https://judge0.com">Judge0</a> instance.
 *
 * <p>All the cases of one run go out as a single batch, then the whole batch is
 * polled until every entry has left the queue. Judge0 does offer a synchronous
 * {@code wait=true} mode, but it is unavailable for batches and its own docs
 * discourage it — it holds an HTTP connection open for the length of the run.
 */
@Component
public class Judge0ExecutionEngine implements ExecutionEngine {

    private static final Logger log = LoggerFactory.getLogger(Judge0ExecutionEngine.class);

    /** Asking for only what is used keeps a 5MB program out of every poll response. */
    private static final String RESULT_FIELDS = "token,status,stdout,stderr,compile_output,message,time,memory";

    /**
     * Judge0 rejects a larger batch outright ({@code MAX_SUBMISSION_BATCH_SIZE},
     * 20 by default). A run against sample cases is nowhere near it, but the cap
     * is a property of the API rather than of this caller, so it is enforced here
     * instead of being assumed away.
     */
    private static final int MAX_BATCH_SIZE = 20;

    private final RestClient restClient;
    private final Judge0Properties properties;

    public Judge0ExecutionEngine(Judge0Properties properties) {
        this.properties = properties;

        // Built here rather than injected from an auto-configured builder: the
        // timeouts that matter for a judge are not the ones an application-wide
        // default would give it, and they belong next to the polling settings
        // they have to stay consistent with.
        //
        // Two deliberate choices here, both learned from Judge0 rejecting
        // perfectly good batches as empty.
        //
        // HTTP/1.1 is pinned because the JDK client otherwise opens with an h2c
        // upgrade that Judge0's Rails 5 server does not speak.
        //
        // BufferingClientHttpRequestFactory is what puts a Content-Length on the
        // request. Spring's request factories stream the body by default, which
        // means `Transfer-Encoding: chunked`, and Judge0's Rack stack reads no
        // body at all from a chunked POST — the submissions array arrives empty
        // and the batch is refused. Buffering costs nothing at this size.
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(properties.connectTimeout())
                .build();
        JdkClientHttpRequestFactory jdkRequestFactory = new JdkClientHttpRequestFactory(httpClient);
        jdkRequestFactory.setReadTimeout(properties.readTimeout());
        BufferingClientHttpRequestFactory requestFactory =
                new BufferingClientHttpRequestFactory(jdkRequestFactory);

        RestClient.Builder builder =
                RestClient.builder().baseUrl(properties.baseUrl()).requestFactory(requestFactory);
        if (properties.authToken() != null && !properties.authToken().isBlank()) {
            builder = builder.defaultHeader("X-Auth-Token", properties.authToken());
        }
        this.restClient = builder.build();
    }

    @Override
    public List<ExecutionResult> execute(ExecutionRequest request) {
        if (request.stdins().isEmpty()) {
            return List.of();
        }

        int languageId = languageId(request.language());
        String encodedProgram = encode(request.program());

        List<Judge0Api.Submission> submissions = request.stdins().stream()
                .map(stdin -> new Judge0Api.Submission(
                        languageId,
                        encodedProgram,
                        encode(stdin),
                        request.compilerOptions(),
                        properties.cpuTimeLimitSeconds(),
                        properties.wallTimeLimitSeconds(),
                        properties.memoryLimitKb()))
                .toList();

        List<String> tokens = new ArrayList<>(submissions.size());
        for (int from = 0; from < submissions.size(); from += MAX_BATCH_SIZE) {
            tokens.addAll(createBatch(submissions.subList(from, Math.min(from + MAX_BATCH_SIZE, submissions.size()))));
        }

        // Polled as one set: they were all queued before the first poll, so
        // waiting on them together costs no more than waiting on them in turn.
        return awaitBatch(tokens).stream()
                .map(Judge0ExecutionEngine::toExecutionResult)
                .toList();
    }

    private List<String> createBatch(List<Judge0Api.Submission> submissions) {
        URI uri = UriComponentsBuilder.fromPath("/submissions/batch")
                .queryParam("base64_encoded", "true")
                .build()
                .toUri();

        Judge0Api.CreatedToken[] created;
        try {
            created = restClient
                    .post()
                    .uri(uri)
                    .body(new Judge0Api.BatchRequest(submissions))
                    .retrieve()
                    .body(Judge0Api.CreatedToken[].class);
        } catch (RestClientException e) {
            throw new ExecutionException("Judge0 rejected the batch: " + e.getMessage(), e);
        }

        if (created == null || created.length != submissions.size()) {
            throw new ExecutionException("Judge0 returned " + (created == null ? 0 : created.length)
                    + " tokens for " + submissions.size() + " submissions");
        }

        List<String> tokens = new ArrayList<>(created.length);
        for (Judge0Api.CreatedToken token : created) {
            if (token.token() == null) {
                // Judge0 answers a rejected entry with a field-keyed error object
                // in place of a token, so a null here means it refused the
                // submission outright — bad language id, limit above the max.
                throw new ExecutionException("Judge0 refused a submission in the batch");
            }
            tokens.add(token.token());
        }
        return tokens;
    }

    /** Polls until nothing is left in the queue, or the deadline passes. */
    private List<Judge0Api.Result> awaitBatch(List<String> tokens) {
        Instant deadline = Instant.now().plus(properties.pollTimeout());
        long intervalMillis = Math.max(properties.pollInterval().toMillis(), 50);

        while (true) {
            List<Judge0Api.Result> results = new ArrayList<>(tokens.size());
            for (int from = 0; from < tokens.size(); from += MAX_BATCH_SIZE) {
                results.addAll(fetch(tokens.subList(from, Math.min(from + MAX_BATCH_SIZE, tokens.size()))));
            }

            boolean settled = results.stream()
                    .allMatch(result -> result.status() != null && !Judge0Verdict.isPending(result.status().id()));
            if (settled) {
                return results;
            }

            if (Instant.now().isAfter(deadline)) {
                log.warn("Judge0 batch {} still queued after {}", tokens, properties.pollTimeout());
                throw new ExecutionException("The judge did not finish in time");
            }

            sleep(intervalMillis);
        }
    }

    private List<Judge0Api.Result> fetch(List<String> tokens) {
        URI uri = UriComponentsBuilder.fromPath("/submissions/batch")
                .queryParam("tokens", String.join(",", tokens))
                .queryParam("base64_encoded", "true")
                .queryParam("fields", RESULT_FIELDS)
                .build()
                .toUri();

        Judge0Api.BatchResults batch;
        try {
            batch = restClient.get().uri(uri).retrieve().body(Judge0Api.BatchResults.class);
        } catch (RestClientException e) {
            throw new ExecutionException("Judge0 poll failed: " + e.getMessage(), e);
        }

        if (batch == null || batch.submissions() == null) {
            throw new ExecutionException("Judge0 returned an empty batch result");
        }
        return batch.submissions();
    }

    private static ExecutionResult toExecutionResult(Judge0Api.Result result) {
        int statusId = result.status() == null ? 0 : result.status().id();
        SubmissionStatus status = Judge0Verdict.toStatus(statusId);

        // `message` carries the sandbox's own note (e.g. why isolate killed the
        // process) and is the only explanation for some failures, so it is worth
        // showing when the program left no stderr of its own.
        String stderr = decode(result.stderr());
        if ((stderr == null || stderr.isBlank()) && result.message() != null) {
            stderr = decode(result.message());
        }

        return new ExecutionResult(
                status,
                decode(result.stdout()),
                stderr,
                decode(result.compileOutput()),
                toMillis(result.time()),
                result.memory());
    }

    private int languageId(Language language) {
        Integer id = properties.languageIds().get(language);
        if (id == null) {
            throw new ExecutionException("No Judge0 language id configured for " + language);
        }
        return id;
    }

    /** Judge0 reports seconds as a decimal string; null when the program never ran. */
    private static Integer toMillis(String seconds) {
        if (seconds == null || seconds.isBlank()) {
            return null;
        }
        try {
            return (int) Math.round(Double.parseDouble(seconds) * 1000);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String encode(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * The MIME decoder, not the basic one.
     *
     * <p>Judge0 wraps its base64 in line separators, which
     * {@code Base64.getDecoder()} rejects outright — every field then falls
     * through undecoded and every case reads as a wrong answer whose "actual
     * output" is a base64 blob.
     */
    private static String decode(String value) {
        if (value == null) {
            return null;
        }
        try {
            return new String(Base64.getMimeDecoder().decode(value), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            // Not fatal: this is program output on its way to a console panel,
            // and showing it undecoded beats failing the whole run.
            log.warn("Judge0 returned a field that was not valid base64");
            return value;
        }
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(Duration.ofMillis(millis));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ExecutionException("Interrupted while waiting for the judge", e);
        }
    }
}
