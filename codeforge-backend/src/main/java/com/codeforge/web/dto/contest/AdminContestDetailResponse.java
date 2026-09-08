package com.codeforge.web.dto.contest;

import com.codeforge.domain.ContestStatus;
import com.codeforge.domain.ContestType;
import com.codeforge.domain.RejudgeState;
import java.time.Instant;
import java.util.List;

/**
 * A contest in full, for the authoring form.
 *
 * @param sealed once true, the questions and the start time are settled and the
 *     form disables them — the frozen problems are what a field is being judged
 *     against, and changing them from under a live contest is precisely what the
 *     snapshots exist to prevent
 * @param rejudgeProgress how far the last rejudge got, so the screen can show a
 *     bar rather than a spinner that never resolves
 */
public record AdminContestDetailResponse(
        Long id,
        String slug,
        String title,
        String description,
        ContestType type,
        ContestStatus status,
        Instant startsAt,
        Instant endsAt,
        int durationMinutes,
        boolean published,
        boolean rated,
        String unratedReason,
        boolean sealed,
        Instant sealedAt,
        Instant ratingsAppliedAt,
        long registrationCount,
        long participantCount,
        List<AdminContestProblemResponse> problems,
        RejudgeState rejudgeState,
        Instant rejudgeStartedAt,
        Instant rejudgeFinishedAt,
        Integer rejudgeTotal,
        Integer rejudgeDone,
        String rejudgeError,
        Instant createdAt,
        Instant updatedAt) {}
