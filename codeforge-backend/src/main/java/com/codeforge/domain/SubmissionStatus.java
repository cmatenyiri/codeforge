package com.codeforge.domain;

/** Judge outcome for a single submission. Mirrors the Judge0 verdict set we care about. */
public enum SubmissionStatus {
    PENDING,
    RUNNING,
    ACCEPTED,
    WRONG_ANSWER,
    TIME_LIMIT_EXCEEDED,
    MEMORY_LIMIT_EXCEEDED,
    RUNTIME_ERROR,
    COMPILE_ERROR,
    INTERNAL_ERROR
}
