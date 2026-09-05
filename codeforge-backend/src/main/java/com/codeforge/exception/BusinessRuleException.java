package com.codeforge.exception;

/**
 * A request that is well-formed but conflicts with the current state of the
 * system (archived problem, interview already finished, …). Rendered as 409.
 */
public class BusinessRuleException extends RuntimeException {

    private final String code;

    public BusinessRuleException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
