package com.codeforge.web.dto.admin;

/**
 * One judged case as the author typed it.
 *
 * @param id the stored case this replaces, or null for a newly added one
 * @param input one line per parameter, in signature order
 * @param expectedOutput the canonical printed form of the expected return value
 * @param hidden false for a sample the solver can see and "Run" against, true
 *     for a graded case only a submission is judged on
 */
public record TestCasePayload(Long id, String input, String expectedOutput, boolean hidden) {}
