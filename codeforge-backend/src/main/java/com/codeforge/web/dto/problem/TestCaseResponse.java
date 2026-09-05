package com.codeforge.web.dto.problem;

/** A sample (non-hidden) case. Hidden cases never leave the server. */
public record TestCaseResponse(Long id, String input, String expectedOutput) {}
