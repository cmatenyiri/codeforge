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
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Runs code on a <a href="https://judge0.com">Judge0</a> instance.
 *
 * <p>All the cases of one run go out as a single batch, and then nothing asks
 * Judge0 anything: every submission carries a callback URL, Judge0 calls it the
 * moment that submission finishes, and the batch completes when the last one
 * has reported — see {@link Judge0Batches}. No thread waits in between, and
 * Judge0 is not asked the same question every few hundred milliseconds by
 * every run in flight.
 *
 * <p>The alternatives were both worse at scale. Polling costs a request per
 * batch per interval whether anything has changed or not, and Judge0's
 * synchronous {@code wait=true} is unavailable for batches and holds a
 * connection open for the length of the run.
 *
 * <p>A callback can still be lost — Judge0 gives up on one after a few failed
 * tries — so a batch that has not completed by
 * {@link Judge0Properties#resultTimeout()} is read back from Judge0 once, and
 * only fails if it is genuinely still unfinished.
 */
@Component
public class Judge0ExecutionEngine implements ExecutionEngine {

    private static final Logger log = LoggerFactory.getLogger(Judge0ExecutionEngine.class);

    /** Asking for only what is used keeps a 5MB program out of the read-back response. */
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
    private final Judge0Batches batches;
    private final Judge0CallbackRouter router;

    /** Only for the rare read after a lost callback, which blocks on Judge0. */
    private final Executor reconciler;

    public Judge0ExecutionEngine(
            Judge0Properties properties, Judge0Batches batches, Judge0CallbackRouter router) {
        this.properties = properties;
        this.batches = batches;
        this.router = router;

        SimpleAsyncTaskExecutor reconcilerExecutor = new SimpleAsyncTaskExecutor("judge0-reconcile-");
        reconcilerExecutor.setVirtualThreads(true);
        this.reconciler = reconcilerExecutor;

        // Built here rather than injected from an auto-configured builder: the
        // timeouts that matter for a judge are not the ones an application-wide
        // default would give it, and they belong next to the result timeout
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

    /**
     * Sends the batch and returns without waiting for it.
     *
     * <p>Sending is synchronous — Judge0 answers the create call as soon as the
     * batch is queued — so a judge that is down or refuses the batch fails the
     * call here and now. Everything after that arrives through the future.
     */
    @Override
    public CompletableFuture<List<ExecutionResult>> execute(ExecutionRequest request) {
        if (request.stdins().isEmpty()) {
            return CompletableFuture.completedFuture(List.of());
        }

        int languageId = languageId(request.language());
        String encodedProgram = encode(request.program());

        Judge0Batches.Batch batch = batches.open();
        String callbackUrl = UriComponentsBuilder.fromUriString(properties.callbackUrl())
                .pathSegment("api", "judge0", "callbacks", router.instanceId(), batch.key())
                .toUriString();

        List<Judge0Api.Submission> submissions = request.stdins().stream()
                .map(stdin -> new Judge0Api.Submission(
                        languageId,
                        encodedProgram,
                        encode(stdin),
                        request.compilerOptions(),
                        properties.cpuTimeLimitSeconds(),
                        properties.wallTimeLimitSeconds(),
                        properties.memoryLimitKb(),
                        callbackUrl))
                .toList();

        List<String> tokens = new ArrayList<>(submissions.size());
        try {
            for (int from = 0; from < submissions.size(); from += MAX_BATCH_SIZE) {
                tokens.addAll(
                        createBatch(submissions.subList(from, Math.min(from + MAX_BATCH_SIZE, submissions.size()))));
            }
        } catch (ExecutionException e) {
            // Stop waiting on callbacks for a batch that was never (fully) sent.
            batch.future().completeExceptionally(e);
            throw e;
        }
        batch.expect(tokens);

        return batch.future()
                .orTimeout(properties.resultTimeout().toMillis(), TimeUnit.MILLISECONDS)
                .exceptionallyCompose(failure -> unwrap(failure) instanceof TimeoutException
                        ? CompletableFuture.supplyAsync(() -> reconcile(batch.tokens()), reconciler)
                        : CompletableFuture.failedFuture(unwrap(failure)))
                .thenApply(results -> results.stream()
                        .map(Judge0ExecutionEngine::toExecutionResult)
                        .toList());
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

    /**
     * Reads a batch whose callbacks did not all arrive in time, once.
     *
     * <p>The safety net under the callbacks, not a way of waiting: it runs only
     * after {@link Judge0Properties#resultTimeout()}, and a batch that Judge0
     * still reports as queued at that point has failed.
     */
    private List<Judge0Api.Result> reconcile(List<String> tokens) {
        List<Judge0Api.Result> results = new ArrayList<>(tokens.size());
        for (int from = 0; from < tokens.size(); from += MAX_BATCH_SIZE) {
            results.addAll(fetch(tokens.subList(from, Math.min(from + MAX_BATCH_SIZE, tokens.size()))));
        }

        boolean settled = results.size() == tokens.size()
                && results.stream()
                        .allMatch(result -> result.status() != null && !Judge0Verdict.isPending(result.status().id()));
        if (!settled) {
            log.warn("Judge0 batch {} still queued after {}", tokens, properties.resultTimeout());
            throw new ExecutionException("The judge did not finish in time");
        }

        log.warn("Judge0 batch {} finished but not every callback arrived; read it back instead", tokens);
        return results;
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
            throw new ExecutionException("Judge0 could not be read: " + e.getMessage(), e);
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

    private static Throwable unwrap(Throwable failure) {
        return failure instanceof CompletionException && failure.getCause() != null ? failure.getCause() : failure;
    }
}
