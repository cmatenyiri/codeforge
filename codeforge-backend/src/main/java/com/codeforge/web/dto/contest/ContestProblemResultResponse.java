package com.codeforge.web.dto.contest;

/**
 * One cell of the standings grid.
 *
 * @param solvedAtSeconds null while unsolved; otherwise the moment it was
 *     cracked, measured from the start of the contest rather than as a wall
 *     clock, so two people in different time zones read the same number
 * @param wrongAttempts only those before the solve — attempts afterwards cost
 *     nothing and are not shown as if they had
 */
public record ContestProblemResultResponse(
        int position, String label, boolean solved, Long solvedAtSeconds, int wrongAttempts, int attempts) {}
