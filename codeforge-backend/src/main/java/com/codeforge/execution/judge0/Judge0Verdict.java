package com.codeforge.execution.judge0;

import com.codeforge.domain.SubmissionStatus;

/**
 * Judge0's numeric status ids, translated into our verdict vocabulary.
 *
 * <p>Judge0 splits runtime failures across ids 7–12 by signal (SIGSEGV, SIGXFSZ,
 * SIGFPE, SIGABRT, non-zero exit, other). The distinction is real but it is not
 * one a solver can act on differently, so they all collapse to
 * {@link SubmissionStatus#RUNTIME_ERROR} and the detail survives in stderr.
 */
final class Judge0Verdict {

    private static final int IN_QUEUE = 1;
    private static final int PROCESSING = 2;
    private static final int ACCEPTED = 3;
    private static final int WRONG_ANSWER = 4;
    private static final int TIME_LIMIT_EXCEEDED = 5;
    private static final int COMPILATION_ERROR = 6;
    private static final int RUNTIME_ERROR_SIGSEGV = 7;
    private static final int EXEC_FORMAT_ERROR = 13;
    private static final int INTERNAL_ERROR = 14;

    private Judge0Verdict() {}

    /** True while Judge0 has accepted the submission but not yet finished it. */
    static boolean isPending(int statusId) {
        return statusId == IN_QUEUE || statusId == PROCESSING;
    }

    static SubmissionStatus toStatus(int statusId) {
        return switch (statusId) {
            case IN_QUEUE, PROCESSING -> SubmissionStatus.RUNNING;
            case ACCEPTED -> SubmissionStatus.ACCEPTED;
            case WRONG_ANSWER -> SubmissionStatus.WRONG_ANSWER;
            case TIME_LIMIT_EXCEEDED -> SubmissionStatus.TIME_LIMIT_EXCEEDED;
            case COMPILATION_ERROR -> SubmissionStatus.COMPILE_ERROR;
            case EXEC_FORMAT_ERROR, INTERNAL_ERROR -> SubmissionStatus.INTERNAL_ERROR;
            default -> statusId >= RUNTIME_ERROR_SIGSEGV && statusId < EXEC_FORMAT_ERROR
                    ? SubmissionStatus.RUNTIME_ERROR
                    : SubmissionStatus.INTERNAL_ERROR;
        };
    }
}
