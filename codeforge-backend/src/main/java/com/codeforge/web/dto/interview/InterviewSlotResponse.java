package com.codeforge.web.dto.interview;

import com.codeforge.domain.Difficulty;

/**
 * One problem's place in a running interview — enough for the tab strip, and no
 * more.
 *
 * <p>Carries no description and no starter code: the problem itself is fetched a
 * slot at a time, which is also what stamps the clock on it.
 *
 * @param warmUp true for the gentler opener of a two-problem round
 * @param locked not reached yet — the round is sequential, so a problem only
 *     opens once the one before it has been solved or skipped
 * @param resolved solved or skipped, and so read-only from here on
 */
public record InterviewSlotResponse(
        int position,
        Long problemId,
        String slug,
        String title,
        Difficulty difficulty,
        boolean warmUp,
        boolean locked,
        boolean resolved,
        boolean solved,
        boolean skipped,
        int attempts,
        int hintsRevealed) {}
