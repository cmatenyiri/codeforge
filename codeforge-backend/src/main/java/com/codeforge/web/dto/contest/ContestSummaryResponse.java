package com.codeforge.web.dto.contest;

import com.codeforge.domain.ContestStatus;
import com.codeforge.domain.ContestType;
import java.time.Instant;

/**
 * One row in the contest list.
 *
 * @param secondsUntilStart counted down by the client between polls; the server
 *     recomputes it on every call and is the only clock that decides anything
 * @param registered whether the caller has signed up — the difference between a
 *     "Register" button and a "Registered" tick
 * @param myRank absent unless the caller sat this one, which is what turns the
 *     past-contests list into a personal history
 */
public record ContestSummaryResponse(
        Long id,
        String slug,
        String title,
        ContestType type,
        ContestStatus status,
        Instant startsAt,
        Instant endsAt,
        int durationMinutes,
        int problemCount,
        boolean rated,
        String unratedReason,
        long registrationCount,
        long participantCount,
        long secondsUntilStart,
        long remainingSeconds,
        boolean registered,
        Integer myRank,
        Integer myScore,
        Double myRatingDelta) {}
