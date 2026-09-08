package com.codeforge.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * The problem of the day, for one date.
 *
 * <p>Rows are written the first time a date is asked for and never rewritten
 * afterwards, which is the whole point: a streak is a claim about what somebody
 * did on a particular day, and it can only be checked if the question that day
 * asked is still the same question tomorrow. A daily challenge derived on the
 * fly from a catalogue that grows would quietly re-answer "what was Tuesday's
 * problem?" every time the catalogue changed.
 *
 * <p>Which problem a date gets is decided by {@code DailyChallengeService} —
 * either an author pinned one, or the rotation picked it. The distinction is
 * kept in {@link #pinned} because an unpinned row may be replaced by an author
 * up until its date arrives, and a pinned one is a decision that should survive.
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(
        name = "daily_challenges",
        uniqueConstraints = @UniqueConstraint(name = "uk_daily_challenge_date", columnNames = "challenge_date"))
public class DailyChallenge extends AuditableEntity {

    /**
     * The UTC date this is the problem for.
     *
     * <p>UTC rather than anybody's local date, so the challenge turns over at one
     * instant for everybody — the same reason a contest starts at one instant.
     * Somebody in Auckland and somebody in Los Angeles are working on the same
     * problem at the same time, and their streaks are measured on the same
     * calendar.
     */
    @Column(name = "challenge_date", nullable = false)
    private LocalDate date;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "problem_id", nullable = false)
    private Problem problem;

    /**
     * True when an author chose this problem rather than the rotation.
     *
     * <p>A pinned row is never reshuffled; an unpinned one may be replaced while
     * its date is still in the future, which is what lets an author curate a
     * particular week without having to fill in every other day by hand.
     */
    @Column(nullable = false)
    private boolean pinned = false;
}
