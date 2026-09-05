package com.codeforge.execution.codegen;

import com.codeforge.domain.DataType;
import com.codeforge.domain.Language;
import java.util.StringJoiner;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Java 13 (the runtime Judge0 1.13.1 ships).
 *
 * <p>The judge compiles a single file whose public class must be {@code Main},
 * so the assembled program is the solver's {@code class Solution} followed by a
 * generated {@code public class Main}. Java allows several top-level classes in
 * one file as long as only one is public, which is exactly the shape needed —
 * with the caveat handled by {@link #PUBLIC_SOLUTION} below.
 */
@Component
public class JavaSupport implements LanguageSupport {

    /**
     * A solver who types {@code public class Solution} would make the file have
     * two public classes and fail to compile, with a message about the file name
     * that explains nothing about this system. Dropping the modifier is
     * invisible and always safe: {@code Main} sits in the same file, so
     * package-private access is enough.
     */
    private static final Pattern PUBLIC_SOLUTION = Pattern.compile("\\bpublic\\s+(?=(final\\s+|abstract\\s+)*class\\s+Solution\\b)");

    @Override
    public Language language() {
        return Language.JAVA;
    }

    @Override
    public String starterCode(ProblemSignature signature) {
        StringJoiner arguments = new StringJoiner(", ");
        for (ProblemSignature.Parameter parameter : signature.parameters()) {
            arguments.add(typeName(parameter.type()) + " " + parameter.name());
        }

        return """
                import java.util.*;

                class Solution {
                    public %s %s(%s) {
                        // Write your solution here
                        %s
                    }
                }
                """
                .formatted(
                        typeName(signature.returnType()),
                        signature.functionName(),
                        arguments.toString(),
                        placeholderReturn(signature.returnType()));
    }

    @Override
    public String buildProgram(String sourceCode, ProblemSignature signature) {
        return PUBLIC_SOLUTION.matcher(sourceCode).replaceAll("") + "\n\n" + harness(signature);
    }

    private String harness(ProblemSignature signature) {
        StringBuilder body = new StringBuilder();
        StringJoiner arguments = new StringJoiner(", ");

        for (int i = 0; i < signature.parameters().size(); i++) {
            ProblemSignature.Parameter parameter = signature.parameters().get(i);
            body.append("            %s %s = Io.%s(line(lines, %d));%n"
                    .formatted(typeName(parameter.type()), parameter.name(), parseMethod(parameter.type()), i));
            arguments.add(parameter.name());
        }

        String call = "new Solution().%s(%s)".formatted(signature.functionName(), arguments.toString());

        return """
                // ── CodeForge harness (generated) ─────────────────────────────────────
                // Reads one line per parameter from stdin, calls Solution, and prints the
                // answer last so your own output stays visible above it.
                public class Main {

                    public static void main(String[] args) throws Exception {
                        java.util.List<String> lines = new java.util.ArrayList<>();
                        java.io.BufferedReader reader =
                                new java.io.BufferedReader(new java.io.InputStreamReader(System.in));
                        for (String read = reader.readLine(); read != null; read = reader.readLine()) {
                            lines.add(read);
                        }

                %s
                        %s answer = %s;
                        System.out.println(Io.format(answer));
                    }

                    private static String line(java.util.List<String> lines, int index) {
                        return index < lines.size() ? lines.get(index) : "";
                    }

                    /** Parsing and printing for the canonical wire format. */
                    static final class Io {

                        private static final int SCALE = %d;

                        static int intValue(String s) {
                            return Integer.parseInt(s.trim());
                        }

                        static long longValue(String s) {
                            return Long.parseLong(s.trim());
                        }

                        static double doubleValue(String s) {
                            return Double.parseDouble(s.trim());
                        }

                        static boolean booleanValue(String s) {
                            return "true".equalsIgnoreCase(s.trim());
                        }

                        static String stringValue(String s) {
                            return s;
                        }

                        static int[] intArray(String s) {
                            String[] parts = elements(s);
                            int[] out = new int[parts.length];
                            for (int i = 0; i < parts.length; i++) {
                                out[i] = Integer.parseInt(parts[i].trim());
                            }
                            return out;
                        }

                        static long[] longArray(String s) {
                            String[] parts = elements(s);
                            long[] out = new long[parts.length];
                            for (int i = 0; i < parts.length; i++) {
                                out[i] = Long.parseLong(parts[i].trim());
                            }
                            return out;
                        }

                        static double[] doubleArray(String s) {
                            String[] parts = elements(s);
                            double[] out = new double[parts.length];
                            for (int i = 0; i < parts.length; i++) {
                                out[i] = Double.parseDouble(parts[i].trim());
                            }
                            return out;
                        }

                        static String[] stringArray(String s) {
                            String[] parts = elements(s);
                            String[] out = new String[parts.length];
                            for (int i = 0; i < parts.length; i++) {
                                out[i] = unquote(parts[i].trim());
                            }
                            return out;
                        }

                        static int[][] intMatrix(String s) {
                            String inner = strip(s);
                            java.util.List<int[]> rows = new java.util.ArrayList<>();
                            int depth = 0;
                            int start = -1;
                            for (int i = 0; i < inner.length(); i++) {
                                char c = inner.charAt(i);
                                if (c == '[') {
                                    if (depth == 0) {
                                        start = i;
                                    }
                                    depth++;
                                } else if (c == ']') {
                                    depth--;
                                    if (depth == 0) {
                                        rows.add(intArray(inner.substring(start, i + 1)));
                                    }
                                }
                            }
                            return rows.toArray(new int[0][]);
                        }

                        /** Splits the top level of a bracketed list; empty list gives no elements. */
                        private static String[] elements(String s) {
                            String inner = strip(s);
                            return inner.isEmpty() ? new String[0] : inner.split(",");
                        }

                        private static String strip(String s) {
                            String t = s.trim();
                            if (t.startsWith("[") && t.endsWith("]")) {
                                t = t.substring(1, t.length() - 1);
                            }
                            return t.trim();
                        }

                        private static String unquote(String s) {
                            return s.length() >= 2 && s.startsWith("\\"") && s.endsWith("\\"")
                                    ? s.substring(1, s.length() - 1)
                                    : s;
                        }

                        static String format(int v) {
                            return String.valueOf(v);
                        }

                        static String format(long v) {
                            return String.valueOf(v);
                        }

                        static String format(boolean v) {
                            return v ? "true" : "false";
                        }

                        static String format(double v) {
                            return java.math.BigDecimal.valueOf(v)
                                    .setScale(SCALE, java.math.RoundingMode.HALF_UP)
                                    .toPlainString();
                        }

                        static String format(String v) {
                            return v == null ? "null" : v;
                        }

                        static String format(int[] v) {
                            StringBuilder sb = new StringBuilder("[");
                            for (int i = 0; i < v.length; i++) {
                                sb.append(i > 0 ? "," : "").append(v[i]);
                            }
                            return sb.append(']').toString();
                        }

                        static String format(long[] v) {
                            StringBuilder sb = new StringBuilder("[");
                            for (int i = 0; i < v.length; i++) {
                                sb.append(i > 0 ? "," : "").append(v[i]);
                            }
                            return sb.append(']').toString();
                        }

                        static String format(double[] v) {
                            StringBuilder sb = new StringBuilder("[");
                            for (int i = 0; i < v.length; i++) {
                                sb.append(i > 0 ? "," : "").append(format(v[i]));
                            }
                            return sb.append(']').toString();
                        }

                        static String format(String[] v) {
                            StringBuilder sb = new StringBuilder("[");
                            for (int i = 0; i < v.length; i++) {
                                sb.append(i > 0 ? "," : "").append('"').append(v[i]).append('"');
                            }
                            return sb.append(']').toString();
                        }

                        static String format(int[][] v) {
                            StringBuilder sb = new StringBuilder("[");
                            for (int i = 0; i < v.length; i++) {
                                sb.append(i > 0 ? "," : "").append(format(v[i]));
                            }
                            return sb.append(']').toString();
                        }

                        private Io() {}
                    }
                }
                """
                .formatted(
                        body.toString().stripTrailing(),
                        typeName(signature.returnType()),
                        call,
                        DataFormat.DOUBLE_SCALE);
    }

    private static String typeName(DataType type) {
        return switch (type) {
            case INT -> "int";
            case LONG -> "long";
            case DOUBLE -> "double";
            case BOOLEAN -> "boolean";
            case STRING -> "String";
            case INT_ARRAY -> "int[]";
            case LONG_ARRAY -> "long[]";
            case DOUBLE_ARRAY -> "double[]";
            case STRING_ARRAY -> "String[]";
            case INT_MATRIX -> "int[][]";
        };
    }

    private static String parseMethod(DataType type) {
        return switch (type) {
            case INT -> "intValue";
            case LONG -> "longValue";
            case DOUBLE -> "doubleValue";
            case BOOLEAN -> "booleanValue";
            case STRING -> "stringValue";
            case INT_ARRAY -> "intArray";
            case LONG_ARRAY -> "longArray";
            case DOUBLE_ARRAY -> "doubleArray";
            case STRING_ARRAY -> "stringArray";
            case INT_MATRIX -> "intMatrix";
        };
    }

    /** A compiling stub: the solver replaces it, but an untouched file still runs. */
    private static String placeholderReturn(DataType type) {
        return switch (type) {
            case INT, LONG -> "return 0;";
            case DOUBLE -> "return 0.0;";
            case BOOLEAN -> "return false;";
            case STRING -> "return \"\";";
            case INT_ARRAY -> "return new int[0];";
            case LONG_ARRAY -> "return new long[0];";
            case DOUBLE_ARRAY -> "return new double[0];";
            case STRING_ARRAY -> "return new String[0];";
            case INT_MATRIX -> "return new int[0][0];";
        };
    }
}
