package com.codeforge.domain;

/** Narrows the catalogue by what the calling user has already done with a problem. */
public enum ProblemStatusFilter {
    /** At least one accepted submission. */
    SOLVED,
    /** Submitted at least once, never accepted. */
    ATTEMPTED,
    /** Never submitted. */
    TODO
}
