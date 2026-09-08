package com.codeforge.web.dto.profile;

import com.codeforge.domain.Difficulty;
import java.time.Instant;

/**
 * A problem somebody solved, on their public profile.
 *
 * <p>One row per problem rather than per submission, and accepted ones only: a
 * public profile is a highlights reel, not an audit log.
 */
public record RecentSolveResponse(String slug, String title, Difficulty difficulty, Instant solvedAt) {}
