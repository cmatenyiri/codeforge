package com.codeforge.web.dto.contest;

/**
 * The unrated switch.
 *
 * @param reason why, shown to every participant on the standings. Silently
 *     withdrawing a field's rating change is the fastest way to lose their
 *     trust; "test case 7 on Q3 rejected correct answers" is the whole
 *     difference between an admission and a shrug
 */
public record ContestRatedRequest(boolean rated, String reason) {}
