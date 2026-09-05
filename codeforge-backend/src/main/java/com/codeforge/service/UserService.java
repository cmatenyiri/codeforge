package com.codeforge.service;

import com.codeforge.domain.Avatar;
import com.codeforge.domain.User;
import com.codeforge.exception.NotFoundException;
import com.codeforge.repository.UserRepository;
import com.codeforge.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reads and edits of the caller's own account.
 *
 * <p>Every method here acts on the current user, so authorization is
 * {@code isAuthenticated()} and the identity comes from the token rather than a
 * path parameter — there is no id to tamper with.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Loads the caller's own record.
     *
     * <p>Only for endpoints that genuinely need stored state; anything that just
     * needs the id, username or role should read {@link SecurityUtils} instead
     * and stay off the database.
     */
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public User getCurrentUser() {
        Long userId = SecurityUtils.requireCurrentUserId();
        return userRepository.findById(userId).orElseThrow(() -> NotFoundException.of("user", userId));
    }

    @Transactional
    @PreAuthorize("isAuthenticated()")
    public User changeAvatar(Avatar avatar) {
        User user = getCurrentUser();
        user.setAvatar(avatar);
        return userRepository.save(user);
    }

    /**
     * Renames the caller.
     *
     * <p>Safe precisely because the JWT subject is the user id: existing tokens
     * keep resolving to the right account. The caller re-issues the cookie so
     * the {@code username} claim does not go stale.
     */
    @Transactional
    @PreAuthorize("isAuthenticated()")
    public User changeUsername(String username) {
        User user = getCurrentUser();
        user.setUsername(username.trim());
        return userRepository.save(user);
    }

    /**
     * Sets a new password.
     *
     * <p>The current password was already verified by the validator, which is
     * the only identity proof in play — this product sends no email, so there is
     * no reset link to fall back on.
     */
    @Transactional
    @PreAuthorize("isAuthenticated()")
    public User changePassword(String newPassword) {
        User user = getCurrentUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public User getById(Long id) {
        return userRepository.findById(id).orElseThrow(() -> NotFoundException.of("user", id));
    }
}
