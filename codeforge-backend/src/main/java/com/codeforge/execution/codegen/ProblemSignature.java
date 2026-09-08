package com.codeforge.execution.codegen;

import com.codeforge.domain.DataType;
import com.codeforge.domain.Problem;
import com.codeforge.domain.ProblemParameter;
import com.codeforge.domain.ProblemSnapshot;
import java.util.List;

/**
 * A problem's solution signature, detached from JPA.
 *
 * <p>Code generation runs outside the persistence context, so it takes this
 * rather than a {@link Problem} — that keeps the generators free of lazy-loading
 * concerns and trivially unit-testable.
 *
 * @param functionName the function a solver implements
 * @param parameters in signature order, one input line per parameter
 * @param returnType shape of the value the function returns
 */
public record ProblemSignature(String functionName, List<Parameter> parameters, DataType returnType) {

    /** @param name argument name, used verbatim in every generated language */
    public record Parameter(String name, DataType type) {}

    public ProblemSignature {
        parameters = List.copyOf(parameters);
    }

    /**
     * Reads the signature off an entity.
     *
     * @return null when the problem has no signature authored yet, which is the
     *     one state in which it cannot be solved in the editor
     */
    public static ProblemSignature from(Problem problem) {
        if (problem.getFunctionName() == null || problem.getReturnType() == null) {
            return null;
        }

        List<Parameter> parameters = problem.getParameters().stream()
                .map(parameter -> new Parameter(parameter.getName(), parameter.getType()))
                .toList();

        return new ProblemSignature(problem.getFunctionName(), parameters, problem.getReturnType());
    }

    /**
     * Reads the signature off a frozen snapshot rather than the live problem.
     *
     * <p>The whole point of the snapshot: an interview generates its starter code
     * and builds its harness from the same declaration, taken once, so the two
     * cannot drift apart underneath a candidate mid-round.
     *
     * @return null when the problem had no signature when the round began
     */
    public static ProblemSignature from(ProblemSnapshot snapshot) {
        if (snapshot.functionName() == null || snapshot.returnType() == null) {
            return null;
        }

        List<Parameter> parameters = snapshot.parameters().stream()
                .map(parameter -> new Parameter(parameter.name(), parameter.type()))
                .toList();

        return new ProblemSignature(snapshot.functionName(), parameters, snapshot.returnType());
    }
}
