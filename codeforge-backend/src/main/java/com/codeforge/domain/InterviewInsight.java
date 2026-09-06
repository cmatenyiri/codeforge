package com.codeforge.domain;

/**
 * A single observation about how an interview went.
 *
 * <p>Codes rather than sentences: the report is translated on the client like
 * every other string in the application, and a server that returned English
 * prose would be the one place that is not.
 *
 * <p>They are about <em>process</em>, not about the answer. Whether the problem
 * was solved is already the score; what a candidate cannot see for themselves is
 * that they spent half the round on the warm-up, or reached for a hint before
 * they had written anything.
 */
public enum InterviewInsight {

    /** Nothing was ever submitted — the session was opened and left. */
    NO_SUBMISSION,

    /** Every problem solved. */
    ALL_SOLVED,

    /** Every problem solved, unaided, first time each. */
    CLEAN_RUN,

    /** Solved the set with a good part of the clock unspent. */
    FINISHED_EARLY,

    /** The buzzer went with problems still open. */
    RAN_OUT_OF_TIME,

    /** The opener ate a disproportionate share of the round. */
    SLOW_WARM_UP,

    /** Hints were revealed; in a real round those are the interviewer bailing you out. */
    HINTS_USED,

    /** Many submissions per solve — compiling in the judge rather than reading the code. */
    MANY_ATTEMPTS,

    /** A problem was passed over. */
    SKIPPED_PROBLEM
}
