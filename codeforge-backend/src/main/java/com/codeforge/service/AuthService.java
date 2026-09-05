package com.codeforge.service;

import com.codeforge.domain.Role;
import com.codeforge.domain.User;
import com.codeforge.exception.InvalidCredentialsException;
import com.codeforge.repository.UserRepository;
import com.codeforge.security.JwtService;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registration and login.
 *
 * <p>Takes entities, not DTOs — the controller does the mapping. Both entry
 * points are explicitly {@code permitAll()} so that the intent is visible rather
 * than merely implied by the absence of an annotation.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    /**
     * Creates an account.
     *
     * <p>The raw password arrives separately from the entity so that a plaintext
     * password is never carried on a persistent object.
     *
     * @param user a transient entity carrying username and email
     * @param rawPassword the plaintext password, already validated
     * @return the persisted user
     */
    @Transactional
    @PreAuthorize("permitAll()")
    public User register(User user, String rawPassword) {
        user.setUsername(user.getUsername().trim());
        user.setEmail(user.getEmail().trim());
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setRole(Role.USER);
        user.setEnabled(true);

        return userRepository.save(user);
    }

    /**
     * Verifies credentials.
     *
     * <p>This is the one place an authenticated flow reads the user table; every
     * subsequent request is served from the token alone.
     *
     * @throws InvalidCredentialsException if the username is unknown, the
     *     password is wrong, or the account is disabled — the three are
     *     indistinguishable to the caller on purpose
     */
    @Transactional(readOnly = true)
    @PreAuthorize("permitAll()")
    public User authenticate(String username, String rawPassword) {
        User user = userRepository
                .findByUsernameIgnoreCase(username.trim())
                .orElseThrow(InvalidCredentialsException::new);

        if (!user.isEnabled() || !passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        return user;
    }

    public String issueAccessToken(User user) {
        return jwtService.issueAccessToken(user);
    }

    public Duration accessTokenTtl() {
        return jwtService.accessTokenTtl();
    }
}
