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
import com.codeforge.web.dto.execution.CaseResultResponse;
import com.codeforge.web.dto.execution.RunResponse;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

/**
 * Runs editor code against a problem's <em>sample</em> cases.
 *
 * <p>This is the "Run" button and nothing more: no submission row is written, no
 * hidden case is touched, and the caller's solved set is unaffected. Submitting
 * — which judges the hidden cases and records a verdict — is a separate
 * operation that does not exist yet.
 */
@Service
@RequiredArgsConstructor
public class ExecutionService {

    private static final Logger log = LoggerFactory.getLogger(ExecutionService.class);

    /** Enough for any honest solution, small enough that nothing pathological is stored or sent. */
    private static final int MAX_SOURCE_LENGTH = 64 * 1024;

    private final ProblemService problemService;
    private final CodeTemplateService codeTemplateService;
    private final ExecutionEngine executionEngine;

    /**
     * The problem is loaded through {@link ProblemService}, whose transaction has
     * committed by the time the judge is called. Holding a database connection
     * open for the seconds a sandbox takes would exhaust the pool under load.
     */
    @PreAuthorize("isAuthenticated()")
    public RunResponse run(String slug, Language language, String sourceCode) {
        validate(sourceCode);

        RunPlan plan = plan(slug, language, sourceCode);
        if (plan.cases().isEmpty()) {
            return new RunResponse(SubmissionStatus.ACCEPTED, null, 0, 0, null, null, List.of());
        }

        List<ExecutionResult> results;
        try {
            results = executionEngine.execute(new ExecutionRequest(
                    language, plan.program(), plan.compilerOptions(), plan.cases().stream().map(SampleCase::input).toList()));
        } catch (ExecutionException e) {
            log.error("Execution failed for problem {} in {}", slug, language, e);
            throw new BusinessRuleException("error.execution.unavailable", e.getMessage());
        }

        return judge(plan.cases(), results);
    }

    /** Prepares everything the judge needs from an already-initialised entity. */
    private RunPlan plan(String slug, Language language, String sourceCode) {
        Problem problem = problemService.getBySlug(slug);

        List<SampleCase> cases = problem.getTestCases().stream()
                .filter(testCase -> !testCase.isHidden())
                .sorted(Comparator.comparingInt(TestCase::getDisplayOrder))
                .map(testCase ->
                        new SampleCase(testCase.getId(), testCase.getInput(), testCase.getExpectedOutput()))
                .toList();

        return new RunPlan(
                codeTemplateService.buildProgram(problem, language, sourceCode),
                codeTemplateService.compilerOptions(language),
                cases);
    }

    /** Turns raw sandbox observations into verdicts by comparing against the expected output. */
    private RunResponse judge(List<SampleCase> cases, List<ExecutionResult> results) {
        List<CaseResultResponse> caseResults = new ArrayList<>(cases.size());
        String compileOutput = null;
        int passed = 0;
        Integer slowest = null;
        Integer peakMemory = null;

        for (int i = 0; i < cases.size(); i++) {
            SampleCase sample = cases.get(i);
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

        return new RunResponse(
                overall, compileOutput, passed, cases.size(), slowest, peakMemory, List.copyOf(caseResults));
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
    private record RunPlan(String program, String compilerOptions, List<SampleCase> cases) {}

    private record SampleCase(Long id, String input, String expectedOutput) {}
}
