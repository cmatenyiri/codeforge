package com.codeforge.web.dto.profile;

import com.codeforge.domain.Language;

/**
 * How many distinct problems somebody has solved in one language.
 *
 * <p>A problem solved in two languages counts for both. The list answers "what
 * do they write in?", not "how do their solves divide up", and the second
 * reading would make the numbers sum to the solved count while telling you
 * less.
 */
public record LanguageStatResponse(Language language, long solved) {}
