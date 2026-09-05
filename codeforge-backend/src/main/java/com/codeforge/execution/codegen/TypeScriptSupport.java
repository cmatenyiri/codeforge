package com.codeforge.execution.codegen;

import com.codeforge.domain.Language;
import java.util.StringJoiner;
import org.springframework.stereotype.Component;

/**
 * TypeScript 3.7 (the compiler Judge0 1.13.1 ships).
 *
 * <p>Two things have to be set up or nothing compiles. The judge invokes
 * {@code tsc} with no project file, which defaults to an ES3 target whose
 * library declarations have no {@code Map}, {@code Set} or {@code Promise} —
 * hence {@link #compilerOptions()}. And there are no {@code @types/node}, so
 * {@code require} is undeclared; the prologue declares it rather than pulling in
 * a dependency the sandbox has no network to fetch.
 *
 * <p>The compiler is old enough to reject genuinely modern syntax
 * ({@code satisfies}, template literal types). That is a limitation of the
 * judge image, not of this code.
 */
@Component
public class TypeScriptSupport extends EcmaScriptSupport {

    @Override
    public Language language() {
        return Language.TYPESCRIPT;
    }

    @Override
    public String compilerOptions() {
        return "--target es2017 --lib es2017,dom";
    }

    @Override
    protected String prologue() {
        return "declare const require: any;\n\n";
    }

    @Override
    public String starterCode(ProblemSignature signature) {
        StringJoiner arguments = new StringJoiner(", ");
        for (ProblemSignature.Parameter parameter : signature.parameters()) {
            arguments.add(parameter.name() + ": " + JavaScriptSupport.typeName(parameter.type()));
        }

        String returnType = JavaScriptSupport.typeName(signature.returnType());

        return """
                function %s(%s): %s {
                  // Write your solution here
                  %s
                }
                """
                .formatted(signature.functionName(), arguments.toString(), returnType, placeholderReturn(returnType));
    }

    /**
     * Without this the untouched stub does not compile: TS2355 rejects a
     * non-void function that never returns. Java's stub carries one for the same
     * reason; JavaScript and Python need none.
     */
    private static String placeholderReturn(String returnType) {
        return switch (returnType) {
            case "number" -> "return 0;";
            case "boolean" -> "return false;";
            case "string" -> "return '';";
            default -> "return [];";
        };
    }
}
