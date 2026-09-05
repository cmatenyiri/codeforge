package com.codeforge.security;

import com.codeforge.domain.Role;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Access to the caller behind the current request.
 *
 * <p>Everything here reads the JWT-derived principal already in the
 * SecurityContext, so none of it hits the database.
 */
public final class SecurityUtils {

    private SecurityUtils() {}

    /** The current principal, or empty for anonymous/unauthenticated requests. */
    public static Optional<AuthenticatedUser> currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        return authentication.getPrincipal() instanceof AuthenticatedUser user ? Optional.of(user) : Optional.empty();
    }

    /**
     * The current principal, for code paths that are already behind an
     * authorization check.
     *
     * @throws IllegalStateException if there is no authenticated caller
     */
    public static AuthenticatedUser requireCurrentUser() {
        return currentUser()
                .orElseThrow(() -> new IllegalStateException("No authenticated user in the SecurityContext"));
    }

    public static Optional<Long> currentUserId() {
        return currentUser().map(AuthenticatedUser::id);
    }

    public static Long requireCurrentUserId() {
        return requireCurrentUser().id();
    }

    public static Optional<String> currentUsername() {
        return currentUser().map(AuthenticatedUser::username);
    }

    public static Optional<Role> currentRole() {
        return currentUser().map(AuthenticatedUser::role);
    }

    public static boolean isAuthenticated() {
        return currentUser().isPresent();
    }

    public static boolean isAdmin() {
        return currentUser().map(AuthenticatedUser::isAdmin).orElse(false);
    }
}
