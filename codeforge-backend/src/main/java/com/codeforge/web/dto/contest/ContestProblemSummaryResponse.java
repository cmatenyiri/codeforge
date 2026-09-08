package com.codeforge.web.dto.contest;

import com.codeforge.domain.Difficulty;

/**
 * One question as it appears on the contest overview and in the arena's tab
 * strip.
 *
 * <p>Carries no statement: the problem itself is fetched one at a time, and
 * before the contest starts there is nothing to fetch at all — a scheduled
 * contest sends this list with the titles blanked, so the page can lay out four
 * numbered placeholders without giving away what they are.
 *
 * @param title null until the contest starts
 * @param solveCount how many competitors have solved it; the "how hard was that
 *     one really" number under the standings
 */
public record ContestProblemSummaryResponse(
        int position,
        String label,
        String title,
        String slug,
        Difficulty difficulty,
        int points,
        boolean solved,
        int attempts,
        Integer solveCount) {}
