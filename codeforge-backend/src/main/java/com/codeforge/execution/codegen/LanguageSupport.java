package com.codeforge.execution.codegen;

import com.codeforge.domain.Language;

/**
 * Everything language-specific about running a solution.
 *
 * <p>An implementation owns two halves of the same contract: the stub a solver
 * starts from, and the harness that calls what they wrote. Both are derived from
 * the same {@link ProblemSignature}, so they cannot drift out of step.
 *
 * <p>The harness contract, identical in all four languages:
 *
 * <ol>
 *   <li>read stdin, split into lines;
 *   <li>parse line <i>i</i> as the declared type of parameter <i>i</i>;
 *   <li>call the solution;
 *   <li>print the return value in the canonical form described on {@link DataFormat}.
 * </ol>
 *
 * <p>The answer is printed <em>last</em>, so a solver's own debug output stays
 * visible in the console without breaking the comparison.
 */
public interface LanguageSupport {

    Language language();

    /** The editable stub: imports, the class or function shell, and a TODO body. */
    String starterCode(ProblemSignature signature);

    /**
     * Wraps a solver's source into a complete, runnable program.
     *
     * @param sourceCode exactly what the editor sent, untouched apart from any
     *     language-specific fixups the compiler would otherwise reject
     */
    String buildProgram(String sourceCode, ProblemSignature signature);

    /**
     * Compiler flags for this language, or null to use the judge's defaults.
     *
     * <p>Only TypeScript needs them, but the hook belongs on the interface
     * rather than as a special case in the engine.
     */
    default String compilerOptions() {
        return null;
    }
}
