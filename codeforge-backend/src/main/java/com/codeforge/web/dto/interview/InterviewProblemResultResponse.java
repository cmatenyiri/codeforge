package com.codeforge.web.dto.interview;

import com.codeforge.domain.Difficulty;

/**
 * How one problem went, for the report.
 *
 * @param timeToSolveSeconds measured from when the problem was first opened, not
 *     from the start of the round — otherwise the second problem is charged for
 *     the first one's time. Null when it was never opened or never solved
 * @param attempts submissions against this slot. The ratio to a solve is the
 *     interesting part: four attempts is a different round from one
 * @param submissionId the accepted submission, so the report can link to the
 *     code that passed
 */
public record InterviewProblemResultResponse(
        int position,
        Long problemId,
        String slug,
        String title,
        Difficulty difficulty,
        boolean warmUp,
        boolean solved,
        boolean skipped,
        Integer timeToSolveSeconds,
        int attempts,
        int hintsRevealed,
        Long submissionId) {}
