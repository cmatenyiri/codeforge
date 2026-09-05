package com.codeforge.web.dto.error;

/**
 * A validation failure attached to one form field.
 *
 * @param field the request field name, matching the frontend form field exactly
 * @param code a stable i18n key, e.g. {@code validation.username.taken}; the
 *     frontend translates this and only falls back to {@code message}
 * @param message English fallback text, for logs and non-UI clients
 */
public record ApiFieldError(String field, String code, String message) {}
