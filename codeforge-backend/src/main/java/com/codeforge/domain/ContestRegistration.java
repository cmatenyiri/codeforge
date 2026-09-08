package com.codeforge.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A declared intention to sit a contest.
 *
 * <p>Kept apart from {@link ContestParticipation} — which only exists once
 * somebody actually submits — because the two answer different questions and
 * conflating them breaks both. Registration is what drives the count on the
 * announcement and the reminder before the start; participation is what gets
 * ranked and rated. Somebody who signs up and never shows must appear in the
 * first and not in the second, or every standings page ends in a tail of
 * hundreds of people on zero points and the rating of everyone above them is
 * computed against a field that never turned up.
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(
        name = "contest_registrations",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_contest_registration",
                        columnNames = {"contest_id", "user_id"}),
        indexes = @Index(name = "ix_contest_registrations_user", columnList = "user_id"))
public class ContestRegistration extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contest_id", nullable = false)
    private Contest contest;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "registered_at", nullable = false)
    private Instant registeredAt = Instant.now();
}
