package com.codeforge.web.dto.problem;

import com.codeforge.domain.Difficulty;
import java.util.List;

/**
 * One row in the problem catalogue.
 *
 * @param solved whether the caller has an accepted submission for it
 */
public record ProblemSummaryResponse(
        Long id, String slug, String title, Difficulty difficulty, List<TagResponse> tags, boolean solved) {}
