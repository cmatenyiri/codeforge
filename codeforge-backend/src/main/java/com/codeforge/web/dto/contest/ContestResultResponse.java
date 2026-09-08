package com.codeforge.web.dto.contest;

import java.util.List;

/**
 * How one person did: the banner above the standings, and their line in a
 * profile's contest history.
 *
 * @param finishSeconds the last accepted submission, measured from the start
 * @param penaltySeconds five minutes per rejected attempt on a problem they went
 *     on to solve, and nothing at all for the ones they did not
 * @param ratingDelta absent until the contest's ratings have been applied, and
 *     for an unrated contest for good
 */
public record ContestResultResponse(
        Long userId,
        String username,
        String avatar,
        Integer rank,
        int score,
        long finishSeconds,
        long penaltySeconds,
        long totalTimeSeconds,
        int submissionCount,
        List<ContestProblemResultResponse> problems,
        Double ratingBefore,
        Double ratingAfter,
        Double ratingDelta) {}
