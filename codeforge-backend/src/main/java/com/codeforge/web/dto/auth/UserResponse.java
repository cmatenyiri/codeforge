package com.codeforge.web.dto.auth;

import com.codeforge.domain.Avatar;
import com.codeforge.domain.Role;
import java.time.Instant;

/** The safe public projection of a user. Never carries the password hash. */
public record UserResponse(
        Long id, String username, String email, Role role, Avatar avatar, Instant createdAt) {}
