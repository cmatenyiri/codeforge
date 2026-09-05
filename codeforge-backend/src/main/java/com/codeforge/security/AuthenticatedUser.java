package com.codeforge.security;

import com.codeforge.domain.Role;

/**
 * The authentication principal, reconstructed from the JWT on every request.
 *
 * <p>Deliberately a value object with no entity reference: the whole point of the
 * token layout is that an authenticated request never touches the database just
 * to find out who is calling. Load the {@link com.codeforge.domain.User} only
 * when you actually need to read or write its state.
 */
public record AuthenticatedUser(Long id, String username, Role role) {

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }
}
