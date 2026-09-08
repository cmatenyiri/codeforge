package com.codeforge.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A timed, ranked round that everybody sits at the same moment.
 *
 * <p>The difference from a mock interview is not the clock — both have one — but
 * that a contest is a <em>comparison</em>. That single fact is what every design
 * decision below follows from:
 *
 * <ul>
 *   <li>The clock is absolute, not per-user. An interview starts when you start
 *       it; a contest starts at {@link #startsAt} whether you are there or not,
 *       because a score is only comparable against the same ninety minutes.
 *   <li>The problems are frozen once for the whole contest rather than once per
 *       participant — see {@link ContestProblem#getSnapshot()}.
 *   <li>The result is not private. Standings are public, and for a rated contest
 *       they move everybody's rating, which is why {@link #rated} can be
 *       withdrawn after the fact and why a rejudge has to be able to run.
 * </ul>
 *
 * <h2>Status is derived, never stored</h2>
 *
 * <p>{@link #status(Instant)} computes DRAFT/SCHEDULED/RUNNING/ENDED/FINALIZED
 * from the timestamps and two flags. Nothing has to run on a schedule for a
 * contest to start or end on time, and there is no stored status to drift out of
 * step with the clock — the same reason {@link ProblemState} is derived from the
 * two booleans on a problem.
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(
        name = "contests",
        uniqueConstraints = {
            @UniqueConstraint(name = "uk_contests_slug", columnNames = "slug"),
            @UniqueConstraint(name = "uk_contests_title", columnNames = "title")
        },
        indexes = @Index(name = "ix_contests_starts_at", columnList = "starts_at"))
public class Contest extends AuditableEntity {

    @Column(nullable = false, length = 160)
    private String title;

    @Column(nullable = false, length = 180)
    private String slug;

    /** Markdown. Rules, prizes, anything the announcement should say. */
    @Lob
    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ContestType type = ContestType.WEEKLY;

    /**
     * When it begins, for everyone at once.
     *
     * <p>Stored as an instant and rendered in the viewer's own zone. A contest
     * announced for "10:30" means one moment in time, not a different one per
     * reader.
     */
    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes = 90;

    /**
     * {@code startsAt + durationMinutes}, denormalised so the clock can be
     * queried in SQL. Maintained by {@link #setStartsAt} and
     * {@link #setDurationMinutes}; never set by hand.
     *
     * <p>The same move {@link Problem#getDifficultyRank()} makes, and for the
     * same reason: "which contests are running right now" and "which ended
     * before this one" are ordinary indexed comparisons against this column,
     * where deriving them would need date arithmetic over two columns that no
     * index could help with.
     */
    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    /**
     * Announced. An unpublished contest is a draft: invisible to everyone but its
     * author, and freely editable — including its start time.
     */
    @Column(nullable = false)
    private boolean published = false;

    /**
     * Whether finishing this contest moves anybody's rating.
     *
     * <p>Withdrawable after the fact, which is the whole reason it is a mutable
     * column rather than a property of the type. When a problem turns out to
     * have been wrong — a test case that rejected correct answers, a statement
     * that was ambiguous — the standings are no longer a measurement of anything,
     * and the honest repair is to stop pretending they were. See
     * {@link #unratedReason}.
     */
    @Column(nullable = false)
    private boolean rated = true;

    /**
     * Why a contest was made unrated, shown to participants on the standings.
     *
     * <p>Not optional in practice even though the column is nullable: silently
     * withdrawing everybody's rating change is the fastest way to lose their
     * trust, and "test case 7 on Q3 was wrong" is the entire difference between
     * an admission and a shrug.
     */
    @Column(name = "unrated_reason", length = 500)
    private String unratedReason;

    /**
     * When the problems were frozen for good.
     *
     * <p>Null until the contest starts. Until then the snapshots are refreshed
     * from the live catalogue on every read, so an author fixing a typo at
     * 09:55 is fixing it for the contest too; from this instant they are never
     * written again by anything but an explicit rejudge.
     */
    @Column(name = "sealed_at")
    private Instant sealedAt;

    /**
     * When ratings were last applied.
     *
     * <p>Also the flag that decides how much a rejudge has to undo: before this
     * is set, a rejudge only has to recompute standings; after it, every rating
     * derived from this contest and every rated contest since has to be replayed.
     */
    @Column(name = "ratings_applied_at")
    private Instant ratingsAppliedAt;

    /** Progress of the last rejudge, so the authoring screen can show it. */
    @Enumerated(EnumType.STRING)
    @Column(name = "rejudge_state", nullable = false, length = 16)
    private RejudgeState rejudgeState = RejudgeState.NONE;

    @Column(name = "rejudge_started_at")
    private Instant rejudgeStartedAt;

    @Column(name = "rejudge_finished_at")
    private Instant rejudgeFinishedAt;

    @Column(name = "rejudge_total")
    private Integer rejudgeTotal;

    @Column(name = "rejudge_done")
    private Integer rejudgeDone;

    /** Why the last rejudge failed, if it did. Null on success. */
    @Column(name = "rejudge_error", length = 500)
    private String rejudgeError;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @OrderBy("position ASC")
    @OneToMany(mappedBy = "contest", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ContestProblem> problems = new ArrayList<>();

    // ── The clock ─────────────────────────────────────────────────────────

    /** Also keeps {@link #endsAt} in step, so the two can never disagree. */
    public void setStartsAt(Instant startsAt) {
        this.startsAt = startsAt;
        refreshEndsAt();
    }

    /** Also keeps {@link #endsAt} in step. */
    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
        refreshEndsAt();
    }

    private void refreshEndsAt() {
        if (startsAt != null) {
            this.endsAt = startsAt.plus(Duration.ofMinutes(durationMinutes));
        }
    }

    public boolean hasStarted(Instant now) {
        return !now.isBefore(startsAt);
    }

    public boolean hasEnded(Instant now) {
        return !now.isBefore(getEndsAt());
    }

    public boolean isRunning(Instant now) {
        return hasStarted(now) && !hasEnded(now);
    }

    /** Seconds until it begins, floored at zero. */
    public long secondsUntilStart(Instant now) {
        return Math.max(0, Duration.between(now, startsAt).toSeconds());
    }

    /** Seconds left on the clock, floored at zero. */
    public long remainingSeconds(Instant now) {
        return Math.max(0, Duration.between(now, getEndsAt()).toSeconds());
    }

    /**
     * Seconds from the start to some moment inside the contest, floored at zero.
     *
     * <p>The unit every score is expressed in: a finish time is "4 812 seconds
     * in", never a wall-clock stamp, so two people in different time zones can be
     * ranked against each other without either of them thinking about it.
     */
    public long secondsIntoContest(Instant moment) {
        return Math.max(0, Duration.between(startsAt, moment).toSeconds());
    }

    /**
     * Where this contest is in its life.
     *
     * @param now the caller's single reading of the clock, so that a response
     *     which mentions the status more than once cannot contradict itself
     */
    public ContestStatus status(Instant now) {
        if (!published) {
            return ContestStatus.DRAFT;
        }
        if (!hasStarted(now)) {
            return ContestStatus.SCHEDULED;
        }
        if (!hasEnded(now)) {
            return ContestStatus.RUNNING;
        }
        // An unrated contest is settled the moment it ends: there is no rating
        // left to apply, so leaving it in ENDED would promise a step that will
        // never come.
        return ratingsAppliedAt != null || !rated ? ContestStatus.FINALIZED : ContestStatus.ENDED;
    }

    /** True once the problems may no longer follow the live catalogue. */
    public boolean isSealed() {
        return sealedAt != null;
    }

    public Optional<ContestProblem> problemAt(int position) {
        return problems.stream().filter(slot -> slot.getPosition() == position).findFirst();
    }

    /** The points on offer, which is the maximum any participant can score. */
    public int totalPoints() {
        return problems.stream().mapToInt(ContestProblem::getPoints).sum();
    }
}
