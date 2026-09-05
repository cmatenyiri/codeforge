package com.codeforge.execution.codegen;

/**
 * The canonical text form of every {@link com.codeforge.domain.DataType}.
 *
 * <p>This is the contract that lets a judged run be decided by string equality:
 * all four harnesses print a return value exactly this way, and problem authors
 * write expected outputs the same way.
 *
 * <table>
 *   <caption>Canonical forms</caption>
 *   <tr><th>Type</th><th>On stdin</th><th>On stdout</th></tr>
 *   <tr><td>INT, LONG</td><td>{@code 42}</td><td>{@code 42}</td></tr>
 *   <tr><td>DOUBLE</td><td>{@code 2.5}</td><td>{@code 2.50000} (5 decimals)</td></tr>
 *   <tr><td>BOOLEAN</td><td>{@code true}</td><td>{@code true}</td></tr>
 *   <tr><td>STRING</td><td>the raw line, unquoted</td><td>the raw value</td></tr>
 *   <tr><td>*_ARRAY</td><td>{@code [1,2,3]}</td><td>{@code [1,2,3]} (no spaces)</td></tr>
 *   <tr><td>STRING_ARRAY</td><td>{@code ["a","b"]}</td><td>{@code ["a","b"]}</td></tr>
 *   <tr><td>INT_MATRIX</td><td>{@code [[1,2],[3,4]]}</td><td>{@code [[1,2],[3,4]]}</td></tr>
 * </table>
 *
 * <p>Strings are deliberately unquoted on stdin: a test case's input line is the
 * value itself, so {@code ()[]{}} stays readable rather than becoming
 * {@code "()[]{}"}.
 */
public final class DataFormat {

    /** Decimals printed for a DOUBLE result. */
    public static final int DOUBLE_SCALE = 5;

    private DataFormat() {}

    /**
     * Trims trailing whitespace from every line and drops trailing blank lines.
     *
     * <p>Applied to both sides before comparing, so a missing or extra final
     * newline — the single most common accidental mismatch — is not a wrong
     * answer.
     */
    public static String normalize(String output) {
        if (output == null) {
            return "";
        }

        String[] lines = output.split("\n", -1);
        StringBuilder builder = new StringBuilder(output.length());
        int lastMeaningful = -1;

        for (int i = 0; i < lines.length; i++) {
            if (!lines[i].isBlank()) {
                lastMeaningful = i;
            }
        }
        for (int i = 0; i <= lastMeaningful; i++) {
            if (i > 0) {
                builder.append('\n');
            }
            builder.append(lines[i].stripTrailing());
        }
        return builder.toString();
    }

    /**
     * The line a harness printed the answer on: the last non-blank line.
     *
     * <p>Taking the last line rather than the whole of stdout is what lets a
     * solver leave their own print statements in and still be judged correctly.
     */
    public static String answerLine(String stdout) {
        String normalized = normalize(stdout);
        if (normalized.isEmpty()) {
            return "";
        }

        int lastBreak = normalized.lastIndexOf('\n');
        return lastBreak < 0 ? normalized : normalized.substring(lastBreak + 1);
    }
}
