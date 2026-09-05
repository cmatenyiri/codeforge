package com.codeforge.validation;

import com.codeforge.web.dto.auth.LoginRequest;
import org.springframework.stereotype.Component;

/**
 * Validates the login form.
 *
 * <p>Only checks that the fields were filled in. Whether the credentials are
 * correct is deliberately not a field error — see
 * {@link com.codeforge.exception.InvalidCredentialsException}.
 */
@Component
public class LoginRequestValidator {

    public void validate(LoginRequest request) {
        ValidationErrors errors = new ValidationErrors();

        errors.addIf(
                ValidationRules.isBlank(request.username()),
                "username",
                "validation.username.required",
                "Username is required");
        errors.addIf(
                ValidationRules.isBlank(request.password()),
                "password",
                "validation.password.required",
                "Password is required");

        errors.throwIfAny();
    }
}
