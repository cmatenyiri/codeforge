package com.codeforge.validation;

import com.codeforge.domain.User;
import com.codeforge.repository.UserRepository;
import com.codeforge.security.SecurityUtils;
import com.codeforge.web.dto.user.ChangePasswordRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Validates a password change.
 *
 * <p>A wrong current password is reported as a field error on
 * {@code currentPassword}: unlike login, the caller is already authenticated, so
 * saying which field is wrong reveals nothing they do not already know.
 */
@Component
@RequiredArgsConstructor
public class ChangePasswordRequestValidator {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public void validate(ChangePasswordRequest request) {
        ValidationErrors errors = new ValidationErrors();

        validateCurrentPassword(request.currentPassword(), errors);
        validateNewPassword(request, errors);

        errors.throwIfAny();
    }

    private void validateCurrentPassword(String currentPassword, ValidationErrors errors) {
        if (ValidationRules.isBlank(currentPassword)) {
            errors.add("currentPassword", "validation.currentPassword.required", "Enter your current password");
            return;
        }

        User user = userRepository.findById(SecurityUtils.requireCurrentUserId()).orElseThrow();
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            errors.add("currentPassword", "validation.currentPassword.incorrect", "That is not your current password");
        }
    }

    private void validateNewPassword(ChangePasswordRequest request, ValidationErrors errors) {
        String newPassword = request.newPassword();

        if (ValidationRules.isBlank(newPassword)) {
            errors.add("newPassword", "validation.password.required", "Password is required");
        } else if (newPassword.length() < ValidationRules.PASSWORD_MIN_LENGTH
                || newPassword.length() > ValidationRules.PASSWORD_MAX_LENGTH) {
            errors.add(
                    "newPassword",
                    "validation.password.length",
                    "Password must be between %d and %d characters"
                            .formatted(ValidationRules.PASSWORD_MIN_LENGTH, ValidationRules.PASSWORD_MAX_LENGTH));
        } else if (newPassword.equals(request.currentPassword())) {
            errors.add("newPassword", "validation.newPassword.unchanged", "Choose a password you have not used here");
        }

        if (ValidationRules.isBlank(request.confirmPassword())) {
            errors.add("confirmPassword", "validation.confirmPassword.required", "Please confirm your password");
        } else if (!errors.hasErrorOn("newPassword") && !request.confirmPassword().equals(newPassword)) {
            errors.add("confirmPassword", "validation.confirmPassword.mismatch", "Passwords do not match");
        }
    }
}
