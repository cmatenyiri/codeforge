package com.codeforge.web.dto.contest;

import com.codeforge.domain.ContestStatus;
import com.codeforge.domain.ContestType;
import com.codeforge.domain.RejudgeState;
import java.time.Instant;

/** One row in the authoring list: every contest, drafts included. */
public record AdminContestSummaryResponse(
        Long id,
        String slug,
        String title,
        ContestType type,
        ContestStatus status,
        Instant startsAt,
        int durationMinutes,
        int problemCount,
        boolean published,
        boolean rated,
        boolean sealed,
        boolean ratingsApplied,
        long registrationCount,
        long participantCount,
        RejudgeState rejudgeState,
        Instant updatedAt) {}
