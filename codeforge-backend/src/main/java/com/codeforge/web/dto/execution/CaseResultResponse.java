package com.codeforge.web.dto.execution;

import com.codeforge.domain.SubmissionStatus;

/**
 * The outcome for one sample case.
 *
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
}
