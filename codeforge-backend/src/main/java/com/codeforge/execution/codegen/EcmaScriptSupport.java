package com.codeforge.execution.codegen;

import com.codeforge.domain.DataType;
import java.util.StringJoiner;

/**
 * The half of code generation JavaScript and TypeScript have in common.
 *
 * <p>The harness is emitted as plain JavaScript for both. That is valid
 * TypeScript too, because {@code noImplicitAny} is off by default — so the
 * helper functions type-check with implicit {@code any} parameters and the two
 * languages need only differ in their starter stub and their prologue.
 */
abstract class EcmaScriptSupport implements LanguageSupport {

    @Override
    public String buildProgram(String sourceCode, ProblemSignature signature) {
        return prologue() + sourceCode + "\n\n" + harness(signature);
    }

    /** Emitted above the solver's code; empty for JavaScript. */
    protected String prologue() {
        return "";
    }

    private String harness(ProblemSignature signature) {
        StringBuilder body = new StringBuilder();
        StringJoiner arguments = new StringJoiner(", ");

        for (int i = 0; i < signature.parameters().size(); i++) {
            ProblemSignature.Parameter parameter = signature.parameters().get(i);
            body.append("const %s = _cf%s(_cfLine(%d));%n"
                    .formatted(parameter.name(), parseSuffix(parameter.type()), i));
            arguments.add(parameter.name());
        }

        return """
                // ── CodeForge harness (generated) ─────────────────────────────────────
                // Reads one line per parameter from stdin, calls your function, and prints
                // the answer last so your own output stays visible above it.
                const _cfLines = require('fs').readFileSync(0, 'utf8').split('\\n');

                function _cfLine(index) {
                  return index < _cfLines.length ? _cfLines[index] : '';
                }

                function _cfInt(text) {
                  return parseInt(text.trim(), 10);
                }

                function _cfFloat(text) {
                  return parseFloat(text.trim());
                }

                function _cfBool(text) {
                  return text.trim().toLowerCase() === 'true';
                }

                function _cfStr(text) {
                  return text;
                }

                function _cfJson(text) {
                  return JSON.parse(text.trim() || '[]');
                }

                // Formatting follows the problem's declared return type, not the runtime
                // value: a DOUBLE answer that happens to be whole must still print as
                // 2.00000 rather than 2.
                function _cfFmtInt(value) {
                  return String(value);
                }

                function _cfFmtDouble(value) {
                  return Number(value).toFixed(%d);
                }

                function _cfFmtBool(value) {
                  return value ? 'true' : 'false';
                }

                function _cfFmtStr(value) {
                  return value === null || value === undefined ? '' : String(value);
                }

                function _cfFmtList(value) {
                  return JSON.stringify(value);
                }

                function _cfFmtDoubleList(value) {
                  return '[' + value.map(_cfFmtDouble).join(',') + ']';
                }

                %s
                console.log(%s(%s(%s)));
                """
                .formatted(
                        DataFormat.DOUBLE_SCALE,
                        body.toString().stripTrailing(),
                        formatFunction(signature.returnType()),
                        signature.functionName(),
                        arguments.toString());
    }

    protected static String formatFunction(DataType type) {
        return switch (type) {
            case INT, LONG -> "_cfFmtInt";
            case DOUBLE -> "_cfFmtDouble";
            case BOOLEAN -> "_cfFmtBool";
            case STRING -> "_cfFmtStr";
            case DOUBLE_ARRAY -> "_cfFmtDoubleList";
            case INT_ARRAY, LONG_ARRAY, STRING_ARRAY, INT_MATRIX -> "_cfFmtList";
        };
    }

    private static String parseSuffix(DataType type) {
        return switch (type) {
            case INT, LONG -> "Int";
            case DOUBLE -> "Float";
            case BOOLEAN -> "Bool";
            case STRING -> "Str";
            case INT_ARRAY, LONG_ARRAY, DOUBLE_ARRAY, STRING_ARRAY, INT_MATRIX -> "Json";
        };
    }
}
