package com.codeforge.execution;

import java.util.List;

/**
 * Runs a program against a set of inputs, somewhere it cannot do harm.
 *
 * <p>The interface exists so the rest of the application never learns what the
 * sandbox is. Judge0 is the implementation today; it is amd64-only and needs a
 * privileged container, so a deployment that cannot meet those terms should be
 * able to swap the engine without the service layer noticing.
 *
 * <p>Implementations run every input of a request as one unit of work — the
 * inputs of a single "Run" are independent, and submitting them together is
 * what keeps a three-case run from paying three compile costs in series.
 */
public interface ExecutionEngine {

    /**
     * @return results in the same order as {@link ExecutionRequest#stdins()},
     *     one per entry
     */
    List<ExecutionResult> execute(ExecutionRequest request);
}
