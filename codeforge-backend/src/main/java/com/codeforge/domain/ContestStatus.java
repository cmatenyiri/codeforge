package com.codeforge.domain;

/**
 * Where a contest is in its life, as one value.
 *
 * <p>Derived rather than stored — from {@code published}, the start time, the
 * duration and whether ratings have been applied — for the same reason
 * {@link ProblemState} is: a stored status is a second copy of the truth, and
 * the moment a contest becomes RUNNING is the passing of a timestamp, not an
 * event anybody writes down. Nothing has to run on a schedule for a contest to
 * start on time.
 */
public enum ContestStatus {

    /** Being written. Invisible to everyone but its author, and freely editable. */
    DRAFT,

    /** Announced and open for registration; the problems are not readable yet. */
    SCHEDULED,

    /** In progress. Problems are readable, submissions are judged and scored. */
    RUNNING,

    /**
     * Over, but not settled: standings are final, ratings are not applied yet.
     *
     * <p>The window in which a rejudge is cheap. It exists because the whole
     * point of a rejudge is to fix a wrong test case <em>before</em> anyone's
     * rating has moved because of it.
     */
    ENDED,

    /** Settled: ratings have been applied, or the contest was declared unrated. */
    FINALIZED
}
