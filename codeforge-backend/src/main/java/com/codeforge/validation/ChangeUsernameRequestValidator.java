package com.codeforge.validation;

import com.codeforge.repository.UserRepository;
import com.codeforge.security.SecurityUtils;
import com.codeforge.web.dto.user.ChangeUsernameRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Validates a rename.
 *
 * <p>Same shape rules as registration, but the uniqueness check excludes the
 * caller's own row — otherwise re-saving your existing name would report it as
 * taken by you.
 */
@Component
@RequiredArgsConstructor
public class ChangeUsernameRequestValidator {

    private final UserRepository userRepository;

    public void validate(ChangeUsernameRequest request) {
        ValidationErrors errors = new ValidationErrors();
        String username = ValidationRules.trimToNull(request.username());

        if (username == null) {
            errors.add("username", "validation.username.required", "Username is required");
        } else if (username.length() < ValidationRules.USERNAME_MIN_LENGTH
                || username.length() > ValidationRules.USERNAME_MAX_LENGTH) {
            errors.add(
                    "username",
                    "validation.username.length",
                    "Username must be between %d and %d characters"
                            .formatted(ValidationRules.USERNAME_MIN_LENGTH, ValidationRules.USERNAME_MAX_LENGTH));
        } else if (!ValidationRules.USERNAME_PATTERN.matcher(username).matches()) {
            errors.add(
                    "username",
                    "validation.username.format",
                    "Username may only contain letters, digits, underscores and hyphens");
        } else if (userRepository.existsByUsernameIgnoreCaseAndIdNot(username, SecurityUtils.requireCurrentUserId())) {
            errors.add("username", "validation.username.taken", "That username is already taken");
        }

        errors.throwIfAny();
    }
}
