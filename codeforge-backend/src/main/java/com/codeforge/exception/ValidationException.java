package com.codeforge.exception;

import com.codeforge.web.dto.error.ApiFieldError;
import java.util.List;

/**
 * Raised by a validator when one or more request fields are unacceptable.
 *
 * <p>Always carries at least one {@link ApiFieldError}; that is what lets the
 * advice render a form-shaped 400 the frontend can bind to its inputs.
 */
public class ValidationException extends RuntimeException {

    private final transient List<ApiFieldError> fieldErrors;

    public ValidationException(List<ApiFieldError> fieldErrors) {
        super("Request validation failed");
        this.fieldErrors = List.copyOf(fieldErrors);
    }

    public List<ApiFieldError> getFieldErrors() {
        return fieldErrors;
    }
}
