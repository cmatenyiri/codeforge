package com.codeforge.web.dto.admin;

import com.codeforge.domain.DataType;

/**
 * One argument of the function a solver implements.
 *
 * <p>Position in the list is meaningful twice over: it is the argument order in
 * every generated stub, and it is the order of the input lines in a test case.
 */
public record ProblemParameterPayload(String name, DataType type) {}
