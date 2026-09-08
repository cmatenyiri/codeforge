package com.codeforge.web.dto.contest;

import com.codeforge.web.dto.common.PageResponse;
import java.util.List;

/**
 * The scoreboard.
 *
 * @param me the caller's own row, sent alongside the page whatever page they are
 *     looking at. Somebody in 812th place should not have to page through eight
 *     screens to find themselves, and it is the first thing they open the
 *     standings to see
 * @param solveCounts how many people solved each question, in position order —
 *     the number that says whether Q4 was hard or broken
 * @param finalised whether the ranks are settled. While a contest is live they
 *     are a snapshot of a moving target, and saying so is the difference between
 *     a leaderboard and a promise
 */
public record ContestStandingsResponse(
        PageResponse<ContestResultResponse> page,
        ContestResultResponse me,
        List<Integer> solveCounts,
        boolean finalised,
        boolean rated,
        String unratedReason) {}
