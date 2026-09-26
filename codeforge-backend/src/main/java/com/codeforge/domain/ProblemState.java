package com.codeforge.domain;

/**
 * Where a problem is in its authoring life, as one value.
 *
 * <p>Stored as two booleans on {@link Problem} — {@code published} and
 * {@code archived} — because that is what the catalogue queries filter on;
 * this is the projection of them the authoring screens read and filter by,
 * plus the one state that belongs to a contest rather than to the problem.
 */
public enum ProblemState {

    /** Written but not released: invisible to solvers, editable without consequence. */
    DRAFT,

    /**
     * Finished and waiting in an announced contest: invisible to solvers until
     * the contest ends, then published by it.
     *
     * <p>Derived from the contest, never stored — announcing it is what puts a
     * problem here, and withdrawing it is what takes the problem back out. See
     * {@link Contest#isHoldingProblems()}.
     */
    IN_CONTEST,

    /** Live in the catalogue. */
    PUBLISHED,

    /** Retired from the catalogue, kept so past submissions still resolve. */
    ARCHIVED;

    /**
     * @param held whether an announced contest is holding this problem back
     *     from the catalogue
     */
    public static ProblemState of(Problem problem, boolean held) {
        if (problem.isArchived()) {
            return ARCHIVED;
        }
        if (problem.isPublished()) {
            return PUBLISHED;
        }
        return held ? IN_CONTEST : DRAFT;
    }
}
