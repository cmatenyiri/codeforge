package com.codeforge.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * How one person got on with one problem of a contest.
 *
 * <p>The row the standings grid is drawn from — the cell that says "solved at
 * 18:42, two wrong" — and the record a rejudge rewrites.
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(
        name = "contest_participation_problems",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_contest_participation_problem",
                        columnNames = {"participation_id", "position"}))
public class ContestParticipationProblem extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "participation_id", nullable = false)
    private ContestParticipation participation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contest_problem_id", nullable = false)
    private ContestProblem contestProblem;

    /**
     * Copied off the contest problem so the standings grid can be laid out
     * without loading it, and so the ordering above needs no join.
     */
    @Column(name = "position", nullable = false)
    private int position;

    @Column(nullable = false)
    private boolean solved = false;

    /** Seconds into the contest at which it was solved; null while it is not. */
    @Column(name = "solved_at_seconds")
    private Long solvedAtSeconds;

    /**
     * Rejected submissions made <em>before</em> the accepted one.
     *
     * <p>Attempts after a solve are not counted. Once a problem is solved its
     * score is settled, and re-submitting to try a faster solution — which
     * costs nothing and is a perfectly reasonable thing to do with a spare ten
     * minutes — must not retroactively add penalty to a finished problem.
     */
    @Column(name = "wrong_attempts", nullable = false)
    private int wrongAttempts = 0;

    /** Every submission against this problem, accepted or not, before and after. */
    @Column(nullable = false)
    private int attempts = 0;

    /** The submission that solved it, for the link on the standings. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "solved_by_submission_id")
    private Submission solvedBySubmission;

    /** The penalty this problem contributes: nothing at all unless it was solved. */
    public long penaltySeconds() {
        return solved ? wrongAttempts * ContestParticipation.WRONG_SUBMISSION_PENALTY.toSeconds() : 0;
    }
}
