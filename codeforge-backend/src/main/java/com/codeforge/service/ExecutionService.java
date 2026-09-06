package com.codeforge.service;

import com.codeforge.domain.Language;
import com.codeforge.domain.Problem;
import com.codeforge.domain.SubmissionStatus;
import com.codeforge.domain.TestCase;
import com.codeforge.exception.BusinessRuleException;
import com.codeforge.execution.ExecutionEngine;
import com.codeforge.execution.ExecutionException;
import com.codeforge.execution.ExecutionRequest;
import com.codeforge.execution.ExecutionResult;
import com.codeforge.execution.codegen.CodeTemplateService;
import com.codeforge.execution.codegen.DataFormat;
import com.codeforge.service.SubmissionService.NewSubmission;
import com.codeforge.service.SubmissionService.Recorded;
import com.codeforge.web.dto.execution.CaseResultResponse;
import com.codeforge.web.dto.execution.RunResponse;
import com.codeforge.web.dto.submission.SubmissionResultResponse;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

/**
 * Judges editor code against a problem's cases.
 *
 * <p>Two operations, deliberately different in what they risk:
 *
 * <ul>
 *   <li><b>Run</b> is the fast feedback loop. Only the <em>sample</em> cases are
 *       judged, nothing is written down, and the solver sees everything —
 *       input, expected, actual.
 *   <li><b>Submit</b> is the verdict. Every case is judged, including the hidden
 *       ones, the attempt is recorded, and hidden cases come back carrying a
 *       status and nothing else. Passing the samples is therefore not the bar,
 *       which is exactly the point: a solution that special-cases the two
 *       examples fails here.
 * </ul>
 *
 * <p>Both load the problem through {@link ProblemService}, whose transaction has
 * committed by the time the judge is called. Holding a database connection open
 * for the seconds a sandbox takes would exhaust the pool under load.
 */
@Service
@RequiredArgsConstructor
public class ExecutionService {

    private static final Logger log = LoggerFactory.getLogger(ExecutionService.class);

    /** Enough for any honest solution, small enough that nothing pathological is stored or sent. */
    private static final int MAX_SOURCE_LENGTH = 64 * 1024;

    /** Long enough to name the fault, short enough to sit in a table cell. */
    private static final int MAX_FAILURE_MESSAGE_LENGTH = 2000;

    private final ProblemService problemService;
    private final SubmissionService submissionService;
    private final CodeTemplateService codeTemplateService;
    private final ExecutionEngine executionEngine;

    /** Runs against the sample cases only, and records nothing. */
    @PreAuthorize("isAuthenticated()")
    public RunResponse run(String slug, Language language, String sourceCode) {
        validate(sourceCode);

        Plan plan = plan(slug, language, sourceCode, false);
        if (plan.cases().isEmpty()) {
            return new RunResponse(SubmissionStatus.ACCEPTED, null, 0, 0, null, null, List.of());
        }

        Judged judged = judge(plan, execute(plan, slug, language));

        return new RunResponse(
                judged.status(),
                judged.compileOutput(),
                judged.passed(),
                judged.total(),
                judged.runtimeMs(),
                judged.memoryKb(),
                judged.results());
    }

    /**
     * Judges every case and records the verdict.
     *
     * <p>A problem with no cases at all cannot be accepted: there is nothing to
     * be right about, and calling that a solve would put a green tick against a
     * problem nobody has solved.
     */
    @PreAuthorize("isAuthenticated()")
    public SubmissionResultResponse submit(String slug, Language language, String sourceCode) {
        validate(sourceCode);

        Plan plan = plan(slug, language, sourceCode, true);
        if (plan.cases().isEmpty()) {
            throw new BusinessRuleException(
                    "error.execution.noTestCases", "This problem has no test cases to judge against");
        }

        Judged judged = judge(plan, execute(plan, slug, language));
        String failureMessage = failureMessage(judged);

        Recorded recorded = submissionService.record(new NewSubmission(
                plan.problemId(),
                language,
                sourceCode,
                judged.status(),
                judged.passed(),
                judged.total(),
                judged.runtimeMs(),
                judged.memoryKb(),
                failureMessage));

        // Hidden cases are redacted on the way out, not on the way in: they still
        // have to be judged, and their timings still count towards the reported
        // runtime — only their contents must not leave the server.
        List<CaseResultResponse> visible = judged.results().stream()
                .map(result -> result.hidden() ? result.redacted() : result)
                .toList();

        return new SubmissionResultResponse(
                recorded.submissionId(),
                judged.status(),
                judged.compileOutput(),
                failureMessage,
                judged.passed(),
                judged.total(),
                (int) plan.cases().stream().filter(JudgedCase::hidden).count(),
                judged.runtimeMs(),
                judged.memoryKb(),
                recorded.firstAccepted(),
                visible);
    }

    private List<ExecutionResult> execute(Plan plan, String slug, Language language) {
        try {
            return executionEngine.execute(new ExecutionRequest(
                    language,
                    plan.program(),
                    plan.compilerOptions(),
                    plan.cases().stream().map(JudgedCase::input).toList()));
        } catch (ExecutionException e) {
            log.error("Execution failed for problem {} in {}", slug, language, e);
            throw new BusinessRuleException("error.execution.unavailable", e.getMessage());
        }
    }

    /**
     * Prepares everything the judge needs from an already-initialised entity.
     *
     * @param includeHidden false for a run, true for a submission
     */
    private Plan plan(String slug, Language language, String sourceCode, boolean includeHidden) {
        Problem problem = problemService.getForJudging(slug);

        List<JudgedCase> cases = problem.getTestCases().stream()
                .filter(testCase -> includeHidden || !testCase.isHidden())
                // Samples first, then hidden: a failure on a case the solver can
                // actually read is the more useful one to report, and the
                // reported case numbers then line up with the Testcase tab.
                .sorted(Comparator.comparing(TestCase::isHidden).thenComparingInt(TestCase::getDisplayOrder))
                .map(testCase -> new JudgedCase(
                        testCase.getId(), testCase.getInput(), testCase.getExpectedOutput(), testCase.isHidden()))
                .toList();

        return new Plan(
                problem.getId(),
                codeTemplateService.buildProgram(problem, language, sourceCode),
                codeTemplateService.compilerOptions(language),
                cases);
    }

    /** Turns raw sandbox observations into verdicts by comparing against the expected output. */
    private Judged judge(Plan plan, List<ExecutionResult> results) {
        List<JudgedCase> cases = plan.cases();
        List<CaseResultResponse> caseResults = new ArrayList<>(cases.size());
        String compileOutput = null;
        int passed = 0;
        Integer slowest = null;
        Integer peakMemory = null;

        for (int i = 0; i < cases.size(); i++) {
            JudgedCase sample = cases.get(i);
            ExecutionResult result = i < results.size()
                    ? results.get(i)
                    : ExecutionResult.internalError("The judge returned no result for this case");

            String answer = DataFormat.answerLine(result.stdout());
            SubmissionStatus status = verdict(result, answer, sample.expectedOutput());

            if (status == SubmissionStatus.ACCEPTED) {
                passed++;
            }
            if (compileOutput == null && result.compileOutput() != null && !result.compileOutput().isBlank()) {
                compileOutput = result.compileOutput();
            }
            slowest = max(slowest, result.runtimeMs());
            peakMemory = max(peakMemory, result.memoryKb());

            caseResults.add(new CaseResultResponse(
                    sample.id(),
                    status,
                    sample.hidden(),
                    sample.input(),
                    sample.expectedOutput(),
                    answer,
                    DataFormat.normalize(result.stdout()),
                    result.stderr(),
                    result.runtimeMs(),
                    result.memoryKb()));
        }

        // The overall verdict is the first thing that went wrong, not a tally:
        // "Wrong Answer" on case 2 is what a solver needs to see, and averaging
        // or last-writer-wins would bury it.
        SubmissionStatus overall = caseResults.stream()
                .map(CaseResultResponse::status)
                .filter(status -> status != SubmissionStatus.ACCEPTED)
                .findFirst()
                .orElse(SubmissionStatus.ACCEPTED);

        return new Judged(
                overall, compileOutput, passed, cases.size(), slowest, peakMemory, List.copyOf(caseResults));
    }

    /**
     * One line describing why a submission was not accepted, stored on the row so
     * a history is readable without re-running anything.
     *
     * <p>Names the failing case by number only. That number is public — it is on
     * the screen either way — while the case's contents are not.
     */
    private static String failureMessage(Judged judged) {
        if (judged.status() == SubmissionStatus.ACCEPTED) {
            return null;
        }
        if (judged.compileOutput() != null) {
            return truncate(judged.compileOutput());
        }

        int index = 0;
        for (CaseResultResponse result : judged.results()) {
            index++;
            if (result.status() == SubmissionStatus.ACCEPTED) {
                continue;
            }
            String detail = result.stderr() == null || result.stderr().isBlank() ? "" : ": " + result.stderr();
            return truncate(judged.status() + " on test case " + index + detail);
        }

        return judged.status().toString();
    }

    private static String truncate(String value) {
        return value.length() <= MAX_FAILURE_MESSAGE_LENGTH ? value : value.substring(0, MAX_FAILURE_MESSAGE_LENGTH);
    }

    /**
     * The sandbox only reports how the process ended; correctness is decided here.
     *
     * <p>A clean exit therefore still has to be checked against the expected
     * output before it can be called accepted.
     */
    private static SubmissionStatus verdict(ExecutionResult result, String answer, String expected) {
        if (result.status() != SubmissionStatus.ACCEPTED) {
            return result.status();
        }
        return DataFormat.normalize(answer).equals(DataFormat.normalize(expected))
                ? SubmissionStatus.ACCEPTED
                : SubmissionStatus.WRONG_ANSWER;
    }

    private static void validate(String sourceCode) {
        if (sourceCode == null || sourceCode.isBlank()) {
            throw new BusinessRuleException("error.execution.emptySource", "There is no code to run");
        }
        if (sourceCode.length() > MAX_SOURCE_LENGTH) {
            throw new BusinessRuleException(
                    "error.execution.sourceTooLong", "Source exceeds " + MAX_SOURCE_LENGTH + " characters");
        }
    }

    private static Integer max(Integer current, Integer candidate) {
        if (candidate == null) {
            return current;
        }
        return current == null ? candidate : Math.max(current, candidate);
    }

    /** Everything needed to run, gathered while the entity was still attached. */
    private record Plan(Long problemId, String program, String compilerOptions, List<JudgedCase> cases) {}

    private record JudgedCase(Long id, String input, String expectedOutput, boolean hidden) {}

    /** The verdict on a set of cases, before it is shaped for either caller. */
    private record Judged(
            SubmissionStatus status,
            String compileOutput,
            int passed,
            int total,
            Integer runtimeMs,
            Integer memoryKb,
            List<CaseResultResponse> results) {}
}
