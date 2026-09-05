package com.codeforge.exception;

/** The addressed resource does not exist. Rendered as 404. */
public class NotFoundException extends RuntimeException {

    private final String code;

    public NotFoundException(String code, String message) {
        super(message);
        this.code = code;
    }

    public static NotFoundException of(String resource, Object id) {
        return new NotFoundException("error." + resource + ".notFound", resource + " not found: " + id);
    }

    public String getCode() {
        return code;
    }
}
