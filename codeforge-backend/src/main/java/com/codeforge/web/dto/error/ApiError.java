package com.codeforge.web.dto.error;

import java.time.Instant;
import java.util.List;

/**
 * The single error envelope every failed request comes back in.
 *
 * <p>{@code fieldErrors} is populated only for form validation failures. The
 * frontend uses its presence as the signal to render messages under the inputs
 * rather than raising a toast.
 *
 * @param status HTTP status code
 * @param code stable i18n key for the overall failure, e.g. {@code error.auth.invalidCredentials}
 * @param message English fallback text
 * @param path the request URI that failed
 * @param timestamp when the failure was rendered
 * @param fieldErrors per-field problems, or null when the failure is not form-shaped
 */
public record ApiError(
        int status,
        String code,
        String message,
        String path,
        Instant timestamp,
        List<ApiFieldError> fieldErrors) {

    public static ApiError of(int status, String code, String message, String path) {
        return new ApiError(status, code, message, path, Instant.now(), null);
    }

    public static ApiError withFieldErrors(
            int status, String code, String message, String path, List<ApiFieldError> fieldErrors) {
        return new ApiError(status, code, message, path, Instant.now(), fieldErrors);
    }
}
