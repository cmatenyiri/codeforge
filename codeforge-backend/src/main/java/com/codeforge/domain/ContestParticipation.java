package com.codeforge.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
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
 * One person's involvement with a contest: that they entered it, what they
 * scored, and where that put them.
 *
 * <p>Created when they register, and it is the <em>only</em> record of their
 * involvement — there is no separate registrations table. That merge follows
 * from the entry rule: a contest's problems cannot be opened without
 * registering, so everybody who competes has registered and the two sets were
 * storing one relationship twice. It also makes "competed without registering"
 * unrepresentable rather than merely forbidden.
 *
 * <p>Somebody who registers and never submits keeps a row with
 * {@link #submissionCount} at zero. That is what a no-show <em>is</em> — a fact
 * you can read, rather than the absence of a row somewhere else — and it is why
 * they can be ranked last and rated for it.
 *
 * <h2>How a contest is scored</h2>
 *
 * <p>Three numbers, in strict priority order:
 *
 * <ol>
 *   <li><b>Score</b>: the points of the problems solved. More is better, and it
 *       decides the ranking outright — no amount of speed promotes a
 *       three-problem run above a four-problem one.
 *   <li><b>Finish time</b>: seconds from the start to the accepted submission
 *       that solved the <em>last</em> problem they solved. Not the sum of the
 *       per-problem times: a contest is ninety minutes of wall clock and what is
 *       being measured is when you were done.
 *   <li><b>Penalty</b>: {@link #WRONG_SUBMISSION_PENALTY} for every rejected
 *       submission on a problem they went on to solve, added to the finish time.
 * </ol>
 *
 * <p>Penalising only the problems that were eventually solved is the detail that
 * makes the last twenty minutes of a contest worth playing. Charging for wrong
 * answers on an unsolved problem would mean the correct endgame is to stop
 * submitting once you are unsure — so the field would spend the closing minutes
 * sitting on their hands, and a near-miss on the hard problem would cost more
 * than never opening it. Here, a failed attempt at Q4 is free, and there is
 * never a reason not to try.
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(
        name = "contest_participations",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_contest_participation",
                        columnNames = {"contest_id", "user_id"}),
        indexes = {
            @Index(name = "ix_contest_participations_user", columnList = "user_id"),
            // The standings query orders by exactly this pair, thousands of rows
            // at a time, every few seconds while a contest is live.
            @Index(
                    name = "ix_contest_participations_ranking",
                    columnList = "contest_id, score, total_time_seconds")
        })
public class ContestParticipation extends AuditableEntity {

    /** Five minutes per rejected submission, on solved problems only. */
    public static final Duration WRONG_SUBMISSION_PENALTY = Duration.ofMinutes(5);

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contest_id", nullable = false)
    private Contest contest;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * When they entered. Set once, at registration, and never moved.
     *
     * <p>The row's first life is as a bare intention; everything below it stays
     * zero until they actually submit something.
     */
    @Column(name = "registered_at", nullable = false)
    private Instant registeredAt = Instant.now();

    @Column(nullable = false)
    private int score = 0;

    /**
     * Seconds from the start to the last accepted submission, before penalties.
     *
     * <p>Zero for somebody who has not solved anything, which is correct rather
     * than merely convenient: they are ranked last on score anyway, and a null
     * would make the ordering expression need a special case.
     */
    @Column(name = "finish_seconds", nullable = false)
    private long finishSeconds = 0;

    @Column(name = "penalty_seconds", nullable = false)
    private long penaltySeconds = 0;

    /**
     * {@link #finishSeconds} + {@link #penaltySeconds}, stored rather than added
     * in the query.
     *
     * <p>The sort key. A computed expression cannot be indexed, and this is the
     * second half of the ordering on a table read in full every few seconds while
     * a contest is live — so it is written once per submission instead of being
     * recomputed for every row of every page.
     */
    @Column(name = "total_time_seconds", nullable = false)
    private long totalTimeSeconds = 0;

    /**
     * Position on the standings, 1-based; null until the standings are computed.
     *
     * <p>Ties share a rank and consume the places behind them — two people on
     * rank 2 are followed by rank 4 — which is what makes "top 100" mean a
     * hundred people and not a hundred rows.
     */
    @Column(name = "rank_position")
    private Integer rank;

    @Column(name = "submission_count", nullable = false)
    private int submissionCount = 0;

    @OrderBy("position ASC")
    @OneToMany(mappedBy = "participation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ContestParticipationProblem> problems = new ArrayList<>();

    public Optional<ContestParticipationProblem> problemAt(int position) {
        return problems.stream().filter(row -> row.getPosition() == position).findFirst();
    }

    public long solvedCount() {
        return problems.stream().filter(ContestParticipationProblem::isSolved).count();
    }

    /** Recomputes the stored sort key. Called by whatever last changed the two halves. */
    public void refreshTotalTime() {
        this.totalTimeSeconds = finishSeconds + penaltySeconds;
    }
}
