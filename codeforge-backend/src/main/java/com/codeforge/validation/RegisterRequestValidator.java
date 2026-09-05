package com.codeforge.validation;

import com.codeforge.repository.UserRepository;
import com.codeforge.web.dto.auth.RegisterRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Validates the registration form.
 *
 * <p>Shape checks and uniqueness checks live together here on purpose: the
 * uniqueness rules need the repository, so splitting the cheap rules out into
 * annotations would only spread one form's rules across two mechanisms. Cheap
 * checks still run first, and an expensive {@code exists} query is skipped for
 * any field that already failed.
 */
@Component
@RequiredArgsConstructor
public class RegisterRequestValidator {

    private final UserRepository userRepository;

    public void validate(RegisterRequest request) {
        ValidationErrors errors = new ValidationErrors();

        validateUsername(request.username(), errors);
        validateEmail(request.email(), errors);
        validatePassword(request.password(), request.confirmPassword(), errors);
        validateAvatar(request.avatar(), errors);

        errors.throwIfAny();
    }

    private void validateUsername(String rawUsername, ValidationErrors errors) {
        String username = ValidationRules.trimToNull(rawUsername);

        if (username == null) {
            errors.add("username", "validation.username.required", "Username is required");
            return;
        }
        if (username.length() < ValidationRules.USERNAME_MIN_LENGTH
                || username.length() > ValidationRules.USERNAME_MAX_LENGTH) {
            errors.add(
                    "username",
                    "validation.username.length",
                    "Username must be between %d and %d characters"
                            .formatted(ValidationRules.USERNAME_MIN_LENGTH, ValidationRules.USERNAME_MAX_LENGTH));
            return;
        }
        if (!ValidationRules.USERNAME_PATTERN.matcher(username).matches()) {
            errors.add(
                    "username",
                    "validation.username.format",
                    "Username may only contain letters, digits, underscores and hyphens");
            return;
        }
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            errors.add("username", "validation.username.taken", "That username is already taken");
        }
    }

    private void validateAvatar(String avatar, ValidationErrors errors) {
        if (ValidationRules.isBlank(avatar)) {
            errors.add("avatar", "validation.avatar.required", "Choose an avatar");
        } else if (ValidationRules.parseAvatar(avatar).isEmpty()) {
            errors.add("avatar", "validation.avatar.unknown", "That avatar is not available");
        }
    }

    private void validateEmail(String rawEmail, ValidationErrors errors) {
        String email = ValidationRules.trimToNull(rawEmail);

        if (email == null) {
            errors.add("email", "validation.email.required", "Email is required");
            return;
        }
        if (email.length() > ValidationRules.EMAIL_MAX_LENGTH) {
            errors.add("email", "validation.email.length", "Email is too long");
            return;
        }
        if (!ValidationRules.EMAIL_PATTERN.matcher(email).matches()) {
            errors.add("email", "validation.email.format", "Enter a valid email address");
            return;
        }
        if (userRepository.existsByEmailIgnoreCase(email)) {
            errors.add("email", "validation.email.taken", "That email is already registered");
        }
    }

    private void validatePassword(String password, String confirmPassword, ValidationErrors errors) {
        if (ValidationRules.isBlank(password)) {
            errors.add("password", "validation.password.required", "Password is required");
        } else if (password.length() < ValidationRules.PASSWORD_MIN_LENGTH
                || password.length() > ValidationRules.PASSWORD_MAX_LENGTH) {
            errors.add(
                    "password",
                    "validation.password.length",
                    "Password must be between %d and %d characters"
                            .formatted(ValidationRules.PASSWORD_MIN_LENGTH, ValidationRules.PASSWORD_MAX_LENGTH));
        }

        if (ValidationRules.isBlank(confirmPassword)) {
            errors.add("confirmPassword", "validation.confirmPassword.required", "Please confirm your password");
        } else if (!errors.hasErrorOn("password") && !confirmPassword.equals(password)) {
            errors.add("confirmPassword", "validation.confirmPassword.mismatch", "Passwords do not match");
        }
    }
}
