package com.codeforge.web.dto.problem;

import com.codeforge.domain.Difficulty;
import java.util.List;

/**
 * One row in the problem catalogue.
 *
 * @param solved whether the caller has an accepted submission for it
 * @param attempted whether the caller has submitted to it at all — a solved
 *     problem is also an attempted one, and the client shows the stronger of the
 *     two
 * @param acceptanceRate accepted submissions over all submissions, absent until
 *     somebody has submitted
 */
public record ProblemSummaryResponse(
        Long id,
        String slug,
        String title,
        Difficulty difficulty,
        List<TagResponse> tags,
        boolean solved,
        boolean attempted,
        Double acceptanceRate,
        long totalSubmissions) {}
