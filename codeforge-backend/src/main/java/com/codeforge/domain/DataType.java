package com.codeforge.domain;

/**
 * The value shapes a problem's function signature can be built from.
 *
 * <p>This is deliberately a small, closed set rather than a general type system.
 * Every member has to be expressible in all four supported languages, parseable
 * from one line of a test case's input, and printable in a single canonical form
 * that string-compares equal to the authored expected output — see
 * {@code com.codeforge.execution.codegen}.
 */
public enum DataType {
    INT,
    LONG,
    DOUBLE,
    BOOLEAN,
    STRING,
    INT_ARRAY,
    LONG_ARRAY,
    DOUBLE_ARRAY,
    STRING_ARRAY,
    INT_MATRIX
}
