package com.codeforge.web.dto.user;

/**
 * Password change for a signed-in user.
 *
 * <p>Knowledge of the current password is the only proof of identity required —
 * this product sends no email, so there is no reset-link flow to fall back on.
 */
public record ChangePasswordRequest(String currentPassword, String newPassword, String confirmPassword) {}
