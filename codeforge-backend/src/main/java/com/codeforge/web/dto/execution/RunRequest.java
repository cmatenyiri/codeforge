package com.codeforge.web.dto.execution;

import com.codeforge.domain.Language;

/**
 * A "Run" from the editor: try this code against the visible sample cases.
 *
 * <p>Nothing is persisted — that is what separates a run from a submission.
 */
public record RunRequest(Language language, String sourceCode) {}
