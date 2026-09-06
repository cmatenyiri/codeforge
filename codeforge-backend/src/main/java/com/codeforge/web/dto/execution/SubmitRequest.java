package com.codeforge.web.dto.execution;

import com.codeforge.domain.Language;

/**
 * A "Submit" from the editor: judge this code against every case, including the
 * hidden ones, and record the verdict.
 *
 * <p>Same shape as {@link RunRequest} and deliberately a separate type: the two
 * are different operations, and one of them writes.
 */
public record SubmitRequest(Language language, String sourceCode) {}
