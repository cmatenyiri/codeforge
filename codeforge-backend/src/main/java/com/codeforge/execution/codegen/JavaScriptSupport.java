package com.codeforge.execution.codegen;

import com.codeforge.domain.DataType;
import com.codeforge.domain.Language;
import java.util.StringJoiner;
import org.springframework.stereotype.Component;

/**
 * JavaScript on Node 12 (the runtime Judge0 1.13.1 ships).
 *
 * <p>The stub carries JSDoc types rather than bare parameters. That is not
 * decoration: Monaco runs the TypeScript language service over JavaScript too,
 * so the annotations are what turn {@code nums.} into a real completion list in
 * the editor. Node 12 also predates optional chaining, which is worth
 * remembering when a modern-looking solution fails to parse.
 */
@Component
public class JavaScriptSupport extends EcmaScriptSupport {

    @Override
    public Language language() {
        return Language.JAVASCRIPT;
    }

    @Override
    public String starterCode(ProblemSignature signature) {
        StringBuilder jsdoc = new StringBuilder();
        StringJoiner arguments = new StringJoiner(", ");

        for (ProblemSignature.Parameter parameter : signature.parameters()) {
            jsdoc.append(" * @param {%s} %s%n".formatted(typeName(parameter.type()), parameter.name()));
            arguments.add(parameter.name());
        }

        return """
                /**
                %s * @return {%s}
                 */
                function %s(%s) {
                  // Write your solution here
                }
                """
                .formatted(
                        jsdoc.toString(),
                        typeName(signature.returnType()),
                        signature.functionName(),
                        arguments.toString());
    }

    static String typeName(DataType type) {
        return switch (type) {
            case INT, LONG, DOUBLE -> "number";
            case BOOLEAN -> "boolean";
            case STRING -> "string";
            case INT_ARRAY, LONG_ARRAY, DOUBLE_ARRAY -> "number[]";
            case STRING_ARRAY -> "string[]";
            case INT_MATRIX -> "number[][]";
        };
    }
}
