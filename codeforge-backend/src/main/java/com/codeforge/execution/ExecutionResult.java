package com.codeforge.execution;

import com.codeforge.domain.SubmissionStatus;

/**
 * What the sandbox observed for one input.
 *
 * <p>Deliberately says nothing about correctness: comparing {@code stdout} to an
 * expected value is the judge's job, not the sandbox's, so a well-behaved
 * program that printed the wrong answer arrives here as
 * {@link SubmissionStatus#ACCEPTED} and is downgraded later.
 *
 * @param status how the process ended
 * @param stdout everything the program printed, including its own debug output
 * @param stderr the failure text when it crashed, else null
 * @param compileOutput the compiler's complaint when it never ran, else null
 * @param runtimeMs wall time, absent when the program did not run
 * @param memoryKb peak resident memory, absent when the program did not run
 */
public record ExecutionResult(
        SubmissionStatus status,
        String stdout,
        String stderr,
        String compileOutput,
        Integer runtimeMs,
        Integer memoryKb) {

    /** The sandbox itself failed — unreachable, timed out polling, malformed reply. */
    public static ExecutionResult internalError(String message) {
        return new ExecutionResult(SubmissionStatus.INTERNAL_ERROR, null, message, null, null, null);
    }
}
