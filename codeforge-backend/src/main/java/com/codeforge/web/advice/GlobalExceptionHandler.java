package com.codeforge.web.advice;

import com.codeforge.exception.BusinessRuleException;
import com.codeforge.exception.InvalidCredentialsException;
import com.codeforge.exception.NotFoundException;
import com.codeforge.exception.ValidationException;
import com.codeforge.web.dto.error.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.jspecify.annotations.Nullable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Renders every failure as an {@link ApiError}.
 *
 * <p>Extends {@link ResponseEntityExceptionHandler} so all of Spring MVC's
 * built-in failures (unreadable body, wrong method, unknown route, …) are
 * already handled; overriding {@link #handleExceptionInternal} is enough to put
 * them into our envelope rather than re-declaring a handler for each one.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * The form-shaped failure: a 400 carrying {@code fieldErrors}, which is the
     * frontend's cue to render messages under the inputs instead of a toast.
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiError> handleValidation(ValidationException ex, HttpServletRequest request) {
        return ResponseEntity.badRequest()
                .body(ApiError.withFieldErrors(
                        HttpStatus.BAD_REQUEST.value(),
                        "error.validation.failed",
                        ex.getMessage(),
                        request.getRequestURI(),
                        ex.getFieldErrors()));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiError> handleInvalidCredentials(
            InvalidCredentialsException ex, HttpServletRequest request) {
        // Not a field error on purpose: pinning the failure to `username` or
        // `password` would tell an attacker which half they got right.
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiError.of(
                        HttpStatus.UNAUTHORIZED.value(),
                        "error.auth.invalidCredentials",
                        ex.getMessage(),
                        request.getRequestURI()));
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(NotFoundException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiError.of(
                        HttpStatus.NOT_FOUND.value(), ex.getCode(), ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ApiError> handleBusinessRule(BusinessRuleException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiError.of(
                        HttpStatus.CONFLICT.value(), ex.getCode(), ex.getMessage(), request.getRequestURI()));
    }

    /** Reaches here from the filter chain via {@code RestAuthenticationEntryPoint}. */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiError.of(
                        HttpStatus.UNAUTHORIZED.value(),
                        "error.auth.unauthenticated",
                        "Authentication is required to access this resource",
                        request.getRequestURI()));
    }

    /** Raised by {@code @PreAuthorize} in the service layer, and by the filter chain. */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiError.of(
                        HttpStatus.FORBIDDEN.value(),
                        "error.auth.forbidden",
                        "You do not have permission to perform this action",
                        request.getRequestURI()));
    }

    /** Last resort. The cause is logged in full but never returned to the client. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception on {} {}", request.getMethod(), request.getRequestURI(), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiError.of(
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        "error.internal",
                        "Something went wrong. Please try again.",
                        request.getRequestURI()));
    }

    /**
     * Re-bodies everything {@link ResponseEntityExceptionHandler} handles, so
     * built-in MVC failures share the one envelope.
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex,
            @Nullable Object body,
            HttpHeaders headers,
            HttpStatusCode statusCode,
            WebRequest request) {

        ApiError error = ApiError.of(
                statusCode.value(), "error.request.invalid", ex.getMessage(), requestPath(request));

        return ResponseEntity.status(statusCode).headers(headers).body(error);
    }

    private String requestPath(WebRequest request) {
        return request instanceof ServletWebRequest servletRequest
                ? servletRequest.getRequest().getRequestURI()
                : request.getDescription(false);
    }
}
