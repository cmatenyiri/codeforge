package com.codeforge.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * An account. Table is {@code users} because {@code user} is reserved in MySQL.
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(
        name = "users",
        uniqueConstraints = {
            @UniqueConstraint(name = "uk_users_username", columnNames = "username"),
            @UniqueConstraint(name = "uk_users_email", columnNames = "email")
        })
public class User extends AuditableEntity {

    @Column(nullable = false, length = 32)
    private String username;

    @Column(nullable = false, length = 255)
    private String email;

    /** BCrypt hash. Never leaves the service layer. */
    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Role role = Role.USER;

    /** Chosen from a fixed catalogue at registration; never absent. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Avatar avatar;

    @Column(nullable = false)
    private boolean enabled = true;
}
