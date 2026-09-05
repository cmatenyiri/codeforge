package com.codeforge.web.dto.auth;

/**
 * Registration form payload.
 *
 * <p>No Bean Validation annotations by design — see
 * {@link com.codeforge.validation.RegisterRequestValidator}.
 */
public record RegisterRequest(
        String username, String email, String password, String confirmPassword, String avatar) {}
