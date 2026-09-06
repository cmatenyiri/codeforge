package com.codeforge.domain;

/**
 * Where a problem is in its authoring life, as one value.
 *
 * <p>Stored as two booleans on {@link Problem} — {@code published} and
 * {@code archived} — because that is what the catalogue queries filter on;
 * this is the projection of them the authoring screens read and filter by.
 */
public enum ProblemState {

    /** Written but not released: invisible to solvers, editable without consequence. */
    DRAFT,

    /** Live in the catalogue. */
    PUBLISHED,

    /** Retired from the catalogue, kept so past submissions still resolve. */
    ARCHIVED;

    public static ProblemState of(Problem problem) {
        if (problem.isArchived()) {
            return ARCHIVED;
        }
        return problem.isPublished() ? PUBLISHED : DRAFT;
    }
}
