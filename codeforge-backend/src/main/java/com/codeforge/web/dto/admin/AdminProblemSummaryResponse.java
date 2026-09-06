package com.codeforge.web.dto.admin;

import com.codeforge.domain.Difficulty;
import com.codeforge.domain.ProblemState;
import com.codeforge.web.dto.problem.TagResponse;
import java.time.Instant;
import java.util.List;

/**
 * One row in the authoring catalogue.
 *
 * <p>Carries what an author decides by — state, how much of the problem is
 * actually there, and how much traffic it has — rather than what a solver needs.
 * Notably it has no solved/attempted flags: whether the author has personally
 * solved a problem says nothing about whether it is ready.
 *
 * @param testCaseCount every case, samples and hidden together
 * @param sampleTestCaseCount how many of those a solver can see; a published
 *     problem with none has a "Run" button that judges nothing
 * @param solvable whether a signature has been authored, so the row can flag a
 *     problem that cannot be opened in the editor at all
 */
public record AdminProblemSummaryResponse(
        Long id,
        String slug,
        String title,
        Difficulty difficulty,
        ProblemState state,
        List<TagResponse> tags,
        int testCaseCount,
        int sampleTestCaseCount,
        boolean hasEditorial,
        boolean solvable,
        long totalSubmissions,
        Double acceptanceRate,
        Instant updatedAt) {}
