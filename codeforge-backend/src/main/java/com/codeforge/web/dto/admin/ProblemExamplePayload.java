package com.codeforge.web.dto.admin;

/**
 * A worked example as the author typed it.
 *
 * @param id the stored example this replaces, or null for a newly added one —
 *     sending it back is what lets an edit keep the row rather than delete and
 *     recreate every example on each save
 */
public record ProblemExamplePayload(Long id, String input, String output, String explanation) {}
