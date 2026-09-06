package com.codeforge.web.dto.execution;

import com.codeforge.domain.SubmissionStatus;

/**
 * The outcome for one judged case.
 *
 * <p>A hidden case arrives with everything but its verdict stripped: the whole
 * point of judging against cases the solver cannot see is that seeing them —
 * even one at a time, one submission at a time — would give the answers away.
 *
 * @param hidden true when this case is one of the graded, unpublished ones
 * @param input what was fed to the program, echoed so the console needs no second call
 * @param actualOutput the answer line — the last non-blank line the program printed
 * @param stdout everything printed, so a solver's own debugging survives
 * @param stderr the crash text, when there was one
 * @param runtimeMs wall time, absent when the program never ran
 * @param memoryKb peak memory, absent when the program never ran
 */
public record CaseResultResponse(
        Long testCaseId,
        SubmissionStatus status,
        boolean hidden,
        String input,
        String expectedOutput,
        String actualOutput,
        String stdout,
        String stderr,
        Integer runtimeMs,
        Integer memoryKb) {

    public boolean passed() {
        return status == SubmissionStatus.ACCEPTED;
    }

    /** The same verdict and timings, with everything that could leak the case removed. */
    public CaseResultResponse redacted() {
        return new CaseResultResponse(null, status, true, null, null, null, null, null, runtimeMs, memoryKb);
    }
}
