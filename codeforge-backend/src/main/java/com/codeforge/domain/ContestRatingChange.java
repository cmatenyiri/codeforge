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
 * One movement of one person's rating, caused by one contest.
 *
 * <p>This is the ledger, and {@link User#getRating()} is the running balance.
 * Keeping both is not redundancy: the column on the user is what a leaderboard
 * of ten thousand people can be sorted by in SQL, and these rows are what make
 * the number defensible — the rating graph on a profile is a read of them, and
 * so is any answer to "why did I lose 14 points?".
 *
 * <p>They are also what makes a rating <em>reversible</em>, which a rejudge and
 * the unrated switch both need. Because every row records the rating it started
 * from and the field it was computed against, withdrawing a contest is deleting
 * its rows and replaying the ledger forward from that point, rather than trying
 * to subtract a delta that later contests have already built on top of.
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(
        name = "contest_rating_changes",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_contest_rating_change",
                        columnNames = {"contest_id", "user_id"}),
        indexes = @Index(name = "ix_contest_rating_changes_user", columnList = "user_id"))
public class ContestRatingChange extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contest_id", nullable = false)
    private Contest contest;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "rating_before", nullable = false)
    private double ratingBefore;

    @Column(name = "rating_after", nullable = false)
    private double ratingAfter;

    /**
     * {@code after − before}, stored rather than subtracted on read.
     *
     * <p>It is the number the product actually shows — "+27" next to a contest in
     * a profile's history — and storing it keeps the displayed value identical to
     * the one that was applied, whatever rounding the two endpoints went through.
     */
    @Column(nullable = false)
    private double delta;

    /** Where they finished, kept so the history reads without joining the standings. */
    @Column(name = "rank_position", nullable = false)
    private int rank;

    /**
     * How many people were ranked in the contest.
     *
     * <p>Context for the rank — 300th of 320 and 300th of 12 000 are opposite
     * results — and part of what makes the row reproducible after the fact.
     */
    @Column(name = "participant_count", nullable = false)
    private int participantCount;

    /**
     * Rated contests this user had already sat when this one was scored.
     *
     * <p>Drives the damping factor in {@link com.codeforge.service.ContestRatingService}:
     * a first contest moves a rating a long way and a fiftieth barely at all.
     * Stored rather than counted at replay time so that a recomputation reaches
     * the same numbers in the same order.
     */
    @Column(name = "attended_before", nullable = false)
    private int attendedBefore;

    @Column(name = "applied_at", nullable = false)
    private Instant appliedAt = Instant.now();
}
