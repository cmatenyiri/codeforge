package com.codeforge.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A timed mock interview: a fixed set of problems against a countdown. */
@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "interviews", indexes = @Index(name = "ix_interviews_user", columnList = "user_id"))
public class Interview extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private InterviewFormat format;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private InterviewStatus status = InterviewStatus.IN_PROGRESS;

    /**
     * Copied off the format at the start rather than read back through it.
     *
     * <p>The countdown a candidate agreed to has to survive someone later
     * deciding that STANDARD should be fifty minutes: a finished report would
     * otherwise start describing a round that never happened.
     */
    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    /** Solved problems out of the assigned set; null until the interview ends. */
    @Column(name = "score")
    private Integer score;

    /** The band the score lands in; null until the interview ends. */
    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private InterviewOutcome outcome;

    /**
     * The candidate's own answer to "did you look anything up?", null until they
     * say.
     *
     * <p>Nothing enforces it and nothing penalises it. A self-guided mock has no
     * stakes to cheat for, so the only thing worth building is a way to keep the
     * record honest for the one person who reads it.
     */
    @Column(name = "used_outside_help")
    private Boolean usedOutsideHelp;

    @OrderBy("position ASC")
    @OneToMany(mappedBy = "interview", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InterviewProblem> problems = new ArrayList<>();

    /** When the countdown runs out. */
    public Instant deadline() {
        return startedAt.plus(Duration.ofMinutes(durationMinutes));
    }

    /**
     * Seconds left on the clock, floored at zero.
     *
     * <p>The server is the only clock that counts. The browser renders a
     * countdown for the candidate to work against, but a tab left open across a
     * laptop suspend — or a devtools console — must not be able to buy time.
     */
    public long remainingSeconds(Instant now) {
        return Math.max(0, Duration.between(now, deadline()).toSeconds());
    }

    public long elapsedSeconds(Instant now) {
        Instant until = endedAt == null ? now : endedAt;
        return Math.max(0, Duration.between(startedAt, until).toSeconds());
    }

    public boolean isExpired(Instant now) {
        return !now.isBefore(deadline());
    }

    public boolean isActive() {
        return status == InterviewStatus.IN_PROGRESS;
    }

    /** The slot at a position, or empty when the position is not in this set. */
    public Optional<InterviewProblem> slotAt(int position) {
        return problems.stream().filter(slot -> slot.getPosition() == position).findFirst();
    }

    public long solvedCount() {
        return problems.stream().filter(InterviewProblem::isSolved).count();
    }

    /**
     * The problem the candidate is on: the first that is neither solved nor
     * skipped, or empty once the whole set is behind them.
     *
     * <p>This is what makes the round sequential. A real interviewer asks one
     * question at a time; seeing both up front turns the round into a choice of
     * which question to answer, which is not a choice a real one offers.
     *
     * <p>Everything before it is read-only and everything after it is out of
     * reach, so the position is derived rather than stored — there is no second
     * copy of the truth to drift.
     */
    public OptionalInt activePosition() {
        return problems.stream()
                .filter(slot -> !slot.isResolved())
                .mapToInt(InterviewProblem::getPosition)
                .min();
    }

    /** True once every problem is solved or skipped and only the debrief is left. */
    public boolean isSetFinished() {
        return activePosition().isEmpty();
    }
}
