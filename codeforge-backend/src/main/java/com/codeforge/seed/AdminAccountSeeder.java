package com.codeforge.seed;

import com.codeforge.domain.Avatar;
import com.codeforge.domain.Role;
import com.codeforge.domain.User;
import com.codeforge.repository.UserRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Makes sure there is somebody who can author problems.
 *
 * <p>Runs before {@link ProblemSeeder} so that a first start has an administrator
 * before it has a catalogue. Two cases, and it is deliberate that they behave
 * differently:
 *
 * <ul>
 *   <li>No such account: it is created, with the configured password.
 *   <li>The account exists as an ordinary user: it is promoted, and its password
 *       is left exactly as it was. Naming a username in this setting is a
 *       statement about who administers the instance, but it is not a licence to
 *       overwrite the credentials of somebody who has already registered.
 * </ul>
 *
 * <p>The default credentials are a local-development convenience in the same way
 * the checked-in JWT secret is. Anything reachable from outside localhost should
 * set {@code codeforge.seed.admin.enabled=false} — or at the very least a real
 * password — before it starts.
 */
@Component
@Order(0)
@RequiredArgsConstructor
@ConditionalOnProperty(name = "codeforge.seed.admin.enabled", havingValue = "true", matchIfMissing = true)
public class AdminAccountSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminAccountSeeder.class);

    private final AdminAccountProperties properties;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String username = properties.username();
        if (username == null || username.isBlank()) {
            return;
        }

        Optional<User> existing = userRepository.findByUsernameIgnoreCase(username);
        if (existing.isPresent()) {
            promote(existing.get());
            return;
        }

        User admin = new User();
        admin.setUsername(username.trim());
        admin.setEmail(properties.email());
        admin.setPasswordHash(passwordEncoder.encode(properties.password()));
        admin.setRole(Role.ADMIN);
        admin.setAvatar(Avatar.FORGE);
        admin.setEnabled(true);
        userRepository.save(admin);

        log.warn(
                "Created the bootstrap administrator '{}' with the configured password."
                        + " Change it, or set codeforge.seed.admin.enabled=false, before this instance is reachable"
                        + " from anywhere but localhost.",
                admin.getUsername());
    }

    private void promote(User user) {
        if (user.getRole() == Role.ADMIN) {
            return;
        }

        user.setRole(Role.ADMIN);
        userRepository.save(user);
        log.warn("Promoted the existing account '{}' to ADMIN; its password was left unchanged.", user.getUsername());
    }
}
