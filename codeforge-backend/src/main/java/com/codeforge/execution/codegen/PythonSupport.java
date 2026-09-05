package com.codeforge.execution.codegen;

import com.codeforge.domain.DataType;
import com.codeforge.domain.Language;
import java.util.StringJoiner;
import org.springframework.stereotype.Component;

/**
 * Python 3.8 (the runtime Judge0 1.13.1 ships).
 *
 * <p>3.8 predates PEP 585, so annotations use {@code typing.List} rather than
 * the builtin generics — the starter code has to compile on the judge, not on a
 * modern interpreter.
 */
@Component
public class PythonSupport implements LanguageSupport {

    @Override
    public Language language() {
        return Language.PYTHON;
    }

    @Override
    public String starterCode(ProblemSignature signature) {
        StringJoiner arguments = new StringJoiner(", ", "self, ", "");
        for (ProblemSignature.Parameter parameter : signature.parameters()) {
            arguments.add(parameter.name() + ": " + typeName(parameter.type()));
        }

        return """
                from typing import List


                class Solution:
                    def %s(%s) -> %s:
                        # Write your solution here
                        pass
                """
                .formatted(signature.functionName(), arguments.toString(), typeName(signature.returnType()));
    }

    @Override
    public String buildProgram(String sourceCode, ProblemSignature signature) {
        return sourceCode + "\n\n" + harness(signature);
    }

    private String harness(ProblemSignature signature) {
        StringBuilder body = new StringBuilder();
        StringJoiner arguments = new StringJoiner(", ");

        for (int i = 0; i < signature.parameters().size(); i++) {
            ProblemSignature.Parameter parameter = signature.parameters().get(i);
            body.append("    %s = _cf_%s(_cf_line(_cf_lines, %d))%n"
                    .formatted(parameter.name(), parseSuffix(parameter.type()), i));
            arguments.add(parameter.name());
        }

        return """
                # ── CodeForge harness (generated) ─────────────────────────────────────
                # Reads one line per parameter from stdin, calls Solution, and prints the
                # answer last so your own output stays visible above it.
                import json
                import sys


                def _cf_line(lines, index):
                    return lines[index] if index < len(lines) else ""


                def _cf_int(text):
                    return int(text.strip())


                def _cf_float(text):
                    return float(text.strip())


                def _cf_bool(text):
                    return text.strip().lower() == "true"


                def _cf_str(text):
                    return text


                def _cf_json(text):
                    return json.loads(text.strip() or "[]")


                # Formatting is chosen by the problem's declared return type, not by
                # inspecting the value: a DOUBLE answer that happens to be whole still
                # has to print as 2.00000 rather than 2.
                def _cf_fmt_int(value):
                    return str(int(value))


                def _cf_fmt_double(value):
                    return "%%.%df" %% float(value)


                def _cf_fmt_bool(value):
                    return "true" if value else "false"


                def _cf_fmt_str(value):
                    return "" if value is None else str(value)


                def _cf_fmt_list(value):
                    return json.dumps(list(value), separators=(",", ":"))


                def _cf_fmt_double_list(value):
                    return "[" + ",".join(_cf_fmt_double(v) for v in value) + "]"


                def _cf_main():
                    _cf_lines = sys.stdin.read().split("\\n")
                %s
                    print(%s(Solution().%s(%s)))


                _cf_main()
                """
                .formatted(
                        DataFormat.DOUBLE_SCALE,
                        body.toString().stripTrailing(),
                        formatFunction(signature.returnType()),
                        signature.functionName(),
                        arguments.toString());
    }

    private static String typeName(DataType type) {
        return switch (type) {
            case INT, LONG -> "int";
            case DOUBLE -> "float";
            case BOOLEAN -> "bool";
            case STRING -> "str";
            case INT_ARRAY, LONG_ARRAY -> "List[int]";
            case DOUBLE_ARRAY -> "List[float]";
            case STRING_ARRAY -> "List[str]";
            case INT_MATRIX -> "List[List[int]]";
        };
    }

    private static String formatFunction(DataType type) {
        return switch (type) {
            case INT, LONG -> "_cf_fmt_int";
            case DOUBLE -> "_cf_fmt_double";
            case BOOLEAN -> "_cf_fmt_bool";
            case STRING -> "_cf_fmt_str";
            case DOUBLE_ARRAY -> "_cf_fmt_double_list";
            case INT_ARRAY, LONG_ARRAY, STRING_ARRAY, INT_MATRIX -> "_cf_fmt_list";
        };
    }

    private static String parseSuffix(DataType type) {
        return switch (type) {
            case INT, LONG -> "int";
            case DOUBLE -> "float";
            case BOOLEAN -> "bool";
            case STRING -> "str";
            case INT_ARRAY, LONG_ARRAY, DOUBLE_ARRAY, STRING_ARRAY, INT_MATRIX -> "json";
        };
    }
}
