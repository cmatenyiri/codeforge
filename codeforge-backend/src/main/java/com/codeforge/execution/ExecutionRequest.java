package com.codeforge.execution;

import com.codeforge.domain.Language;
import java.util.List;

/**
 * One unit of sandbox work: a program, and the inputs to run it against.
 *
 * @param language which runtime to use
 * @param program the complete, harnessed source
 * @param compilerOptions extra compiler flags, or null for the judge's defaults.
 *     Carried here rather than derived by the engine so that knowing TypeScript
 *     needs an explicit target stays with the TypeScript generator
 * @param stdins one entry per case, each run independently
 */
public record ExecutionRequest(Language language, String program, String compilerOptions, List<String> stdins) {

    public ExecutionRequest {
        stdins = List.copyOf(stdins);
    }
}
