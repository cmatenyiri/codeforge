package com.codeforge.web.dto.daily;

import com.codeforge.domain.Difficulty;
import java.time.LocalDate;
import java.util.List;

/**
 * Today's problem, and where the caller stands with it.
 *
 * @param solved whether they solved it <em>today</em>. Solving it tomorrow still
 *     solves the problem, but it does not light this up, because the streak it
 *     feeds only counts same-day solves
 * @param streak consecutive days ending today or yesterday; a run that reaches
 *     yesterday is still alive, because today is not over
 * @param secondsUntilRollover how long is left to keep the streak going, which
 *     is the number that actually makes somebody open the tab
 */
public record DailyChallengeResponse(
        LocalDate date,
        String slug,
        String title,
        Difficulty difficulty,
        List<String> tags,
        boolean solved,
        int streak,
        int maxStreak,
        long secondsUntilRollover) {}
