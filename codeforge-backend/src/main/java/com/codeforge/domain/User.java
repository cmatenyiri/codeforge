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

    /** Where every account starts, before a single contest has been sat. */
    public static final double INITIAL_RATING = 1500.0;

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

    // ── Contest rating ────────────────────────────────────────────────────
    // The running balance of ContestRatingChange, denormalised onto the user so
    // that a rating leaderboard is one indexed ORDER BY rather than an aggregate
    // over the whole ledger. Every write to these three goes through
    // ContestRatingService, which owns the ledger and keeps them in step.
    //
    // All three carry database-level defaults, which is what makes adding them to
    // a running instance safe: every account that existed before contests did
    // starts on the same 1500 as a new one, rather than on the 0 an unqualified
    // NOT NULL column would have given them — a rating of zero would sort every
    // pre-existing account to the bottom of a table they have never competed in.

    /**
     * Where this account currently sits, on the scale every rating system of
     * this shape uses: 1500 is average and the starting point for everybody.
     *
     * <p>A double rather than an int because the deltas are fractional and
     * rounding on every contest would let a long history drift by a few points
     * for no reason. It is rounded once, on the way to the screen.
     */
    @Column(nullable = false, columnDefinition = "double default 1500")
    private double rating = INITIAL_RATING;

    /** The highest it has ever been — a thing people care about keeping. */
    @Column(name = "max_rating", nullable = false, columnDefinition = "double default 1500")
    private double maxRating = INITIAL_RATING;

    /**
     * Rated contests actually sat, which is not the same as contests registered
     * for. Drives the damping that makes a newcomer's rating move fast and a
     * veteran's move slowly.
     */
    @Column(name = "contests_attended", nullable = false, columnDefinition = "int default 0")
    private int contestsAttended = 0;

    /** True once they have sat a rated contest; before that the rating is a placeholder, not a measurement. */
    public boolean hasRating() {
        return contestsAttended > 0;
    }
}
