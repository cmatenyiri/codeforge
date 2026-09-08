package com.codeforge.web.dto.contest;

import com.codeforge.domain.ContestType;
import java.time.Instant;
import java.util.List;

/**
 * A whole contest, as one write.
 *
 * <p>Create and edit send the same shape, and the shape is the complete document
 * rather than a patch — the same choice the problem form makes, for the same
 * reason: a patch makes "I removed the last question" indistinguishable from "I
 * did not touch the questions".
 *
 * <p>{@code published} and {@code rated} travel with it rather than being
 * separate endpoints, so that writing a contest and announcing it is one save.
 * Withdrawing the rating after the fact is <em>not</em> here: that has
 * consequences reaching every rating since, and it belongs behind its own
 * deliberate action.
 */
public record ContestUpsertRequest(
        String title,
        String slug,
        String description,
        ContestType type,
        Instant startsAt,
        int durationMinutes,
        boolean published,
        boolean rated,
        List<ContestProblemPayload> problems) {}
