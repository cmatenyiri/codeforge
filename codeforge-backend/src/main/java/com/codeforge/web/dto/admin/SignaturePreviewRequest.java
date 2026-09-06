package com.codeforge.web.dto.admin;

import com.codeforge.domain.DataType;
import java.util.List;

/**
 * A signature to render starter code for, before it has been saved.
 *
 * <p>The point is the feedback loop: a return type or an argument order is far
 * easier to judge from the four stubs it produces than from the form that
 * declares it, and an author should not have to save a problem to find out.
 */
public record SignaturePreviewRequest(
        String functionName, DataType returnType, List<ProblemParameterPayload> parameters) {}
