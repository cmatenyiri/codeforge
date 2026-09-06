package com.codeforge.domain;

/**
 * The band a finished interview lands in.
 *
 * <p>Deliberately four coarse bands rather than a percentage. What a candidate
 * can act on is "you solved it, but only with hints and with two minutes to
 * spare"; a score of 74% invites them to optimise the number instead of the
 * skill. It is also why nothing here is comparable between users — this is
 * feedback, not a ranking.
 */
public enum InterviewOutcome {

    /** Nothing was solved inside the time. */
    NO_SOLVE,

    /** Some of the set went in — the common outcome, and the useful one to read. */
    PARTIAL,

    /** Everything solved, but with hints or with the clock closing in. */
    SOLID,

    /** Everything solved, unaided, with time left over. */
    STRONG
}
