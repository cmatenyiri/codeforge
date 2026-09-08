package com.codeforge.web.dto.contest;

/**
 * One question of a contest, as the authoring form sends it.
 *
 * @param points null asks for the default for this position — 3, 4, 5, 6 — which
 *     is what an author wants until they have a reason to want otherwise
 */
public record ContestProblemPayload(Long problemId, Integer points) {}
