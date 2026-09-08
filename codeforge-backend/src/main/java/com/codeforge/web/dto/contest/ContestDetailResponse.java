package com.codeforge.web.dto.contest;

import com.codeforge.domain.ContestStatus;
import com.codeforge.domain.ContestType;
import java.time.Instant;
import java.util.List;

/**
 * A contest's own page: what it is, when it runs, and where the caller stands.
 *
 * @param problems titles blanked until the contest starts — the page needs to
 *     know there are four of them and what each is worth, and nothing more
 * @param myResult absent for somebody who has not submitted; registering alone
 *     does not put anyone on the standings
 */
public record ContestDetailResponse(
        Long id,
        String slug,
        String title,
        String description,
        ContestType type,
        ContestStatus status,
        Instant startsAt,
        Instant endsAt,
        int durationMinutes,
        boolean rated,
        String unratedReason,
        boolean registered,
        long registrationCount,
        long participantCount,
        long secondsUntilStart,
        long remainingSeconds,
        int totalPoints,
        List<ContestProblemSummaryResponse> problems,
        ContestResultResponse myResult) {}
