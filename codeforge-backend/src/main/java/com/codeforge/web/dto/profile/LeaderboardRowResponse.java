package com.codeforge.web.dto.profile;

/**
 * One row of a global table.
 *
 * <p>Serves both leaderboards, which is why the two halves are nullable: the
 * rating table has no use for a solved count and the solved table has no rating
 * to show. One shape means one component renders both, and the two tables cannot
 * drift apart visually for no reason.
 */
public record LeaderboardRowResponse(
        long rank,
        Long userId,
        String username,
        String avatar,
        Double rating,
        Integer contestsAttended,
        Long solved,
        Long points) {}
