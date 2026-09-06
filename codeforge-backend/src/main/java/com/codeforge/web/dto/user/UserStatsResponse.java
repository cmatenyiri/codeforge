package com.codeforge.web.dto.user;

import com.codeforge.domain.Difficulty;
import java.util.List;

/**
 * What the caller has achieved so far, for the dashboard and profile.
 *
 * @param progress one entry per difficulty, in easiest-first order, each with
 *     the number solved out of how many exist — enough to draw a progress bar
 *     without the client knowing the size of the catalogue
 * @param acceptanceRate accepted submissions over all submissions, or null when
 *     nothing has been submitted yet
 */
public record UserStatsResponse(
        long solved,
        long totalProblems,
        long submissions,
        long acceptedSubmissions,
        Double acceptanceRate,
        List<DifficultyProgress> progress) {

    public record DifficultyProgress(Difficulty difficulty, long solved, long total) {}
}
