package com.codeforge.web.dto.profile;

import java.time.Instant;

/**
 * One point on a profile's rating graph, and one row of its contest history.
 *
 * <p>Carries the contest that caused it rather than just a number and a date:
 * the useful question a rating graph provokes is "what happened there?", and the
 * answer has to be one click away.
 */
public record RatingPointResponse(
        String contestSlug,
        String contestTitle,
        Instant startsAt,
        int rank,
        int participantCount,
        double ratingBefore,
        double ratingAfter,
        double delta) {}
