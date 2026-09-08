package com.codeforge.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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
 * One question in a contest: which problem, in which position, for how many
 * points, and the frozen copy everybody is judged against.
 *
 * <p>The snapshot lives here — on the contest's question, not on each
 * participant's attempt at it — and that placement is the fairness guarantee.
 * One copy for the whole field means there is no arrangement of edits, however
 * badly timed, that can result in two people being asked different questions or
 * judged against different cases. Per-participant snapshots would look almost
 * identical and quietly lose that property.
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(
        name = "contest_problems",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_contest_problem_position",
                    columnNames = {"contest_id", "position"}),
            // The same problem twice in one contest is always a mistake, and it
            // would score a participant twice for one solve.
            @UniqueConstraint(
                    name = "uk_contest_problem_problem",
                    columnNames = {"contest_id", "problem_id"})
        })
public class ContestProblem extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contest_id", nullable = false)
    private Contest contest;

    /**
     * The catalogue row this was taken from.
     *
     * <p>Still a real association even though nothing during the contest reads
     * through it: submissions are recorded against the catalogue problem, and the
     * standings link to it once the contest is over and it has been published.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "problem_id", nullable = false)
    private Problem problem;

    /** Zero-based. Q1 is position 0, and the ordering is the intended difficulty ramp. */
    @Column(name = "position", nullable = false)
    private int position;

    /**
     * What solving it is worth.
     *
     * <p>Per-problem rather than one point each, because a contest is decided by
     * <em>which</em> problems you solved and not how many. Four problems worth
     * 3/4/5/6 means the person who cracked the hard one outranks the person who
     * cleared three easy ones — which is the judgement a contest exists to make.
     */
    @Column(nullable = false)
    private int points = 3;

    /**
     * The problem as it stood when the contest sealed, and the only thing anyone
     * reads or is judged against from that moment on.
     *
     * <p>Refreshed from the live catalogue on every read until the contest
     * starts, so an author's last-minute fix still lands; never written again
     * afterwards except by an explicit rejudge, which is the one operation whose
     * entire purpose is to change what "correct" meant. See {@link ProblemSnapshot}.
     */
    @Convert(converter = ProblemSnapshotConverter.class)
    @Column(name = "problem_snapshot", nullable = false, columnDefinition = "LONGTEXT")
    private ProblemSnapshot snapshot;

    /**
     * The default point value for a position: 3, 4, 5, 6, …
     *
     * <p>Only a starting point — an author may set any value — but it is the
     * ramp the format is built around, and pre-filling it means the common case
     * needs no thought.
     */
    public static int defaultPointsFor(int position) {
        return 3 + position;
    }

    /** The letter a contest problem is known by on the standings: Q1, Q2, … */
    public String label() {
        return "Q" + (position + 1);
    }
}
