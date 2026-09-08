package com.codeforge.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** One attempt at a problem, with the judge's verdict. */
@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(
        name = "submissions",
        indexes = {
            @Index(name = "ix_submissions_user", columnList = "user_id"),
            @Index(name = "ix_submissions_problem", columnList = "problem_id"),
            @Index(name = "ix_submissions_contest", columnList = "contest_id"),
            // The activity calendar reads a year of one user's submissions by
            // date; without this it is a full scan per profile view.
            @Index(name = "ix_submissions_user_created", columnList = "user_id, created_at")
        })
public class Submission extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "problem_id", nullable = false)
    private Problem problem;

    /** Set when the submission was made inside a mock interview. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interview_id")
    private Interview interview;

    /**
     * Set when the submission was made inside a contest.
     *
     * <p>Denormalised alongside {@link #contestProblem}, which already knows its
     * contest. It is here because "every submission in contest X" is the query a
     * rejudge runs and the standings are rebuilt from, and reaching it through
     * the problem would make an indexed lookup into a join.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contest_id")
    private Contest contest;

    /** Which question of the contest, so a verdict can be attributed to a slot. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contest_problem_id")
    private ContestProblem contestProblem;

    /**
     * True while the contest was still running when this was submitted.
     *
     * <p>Contest problems stay open afterwards — "virtual" practice on a past
     * round is the most useful thing a finished contest leaves behind — and those
     * attempts are ordinary submissions that must never touch the standings.
     * Stamped at submission time rather than derived from the timestamps later,
     * because a rejudge re-runs a submission months after the fact and has to
     * reach the same answer about whether it counted.
     */
    @Column(name = "counted_in_contest", nullable = false)
    private boolean countedInContest = false;

    /**
     * How far into the contest this was <em>sent</em>, in seconds.
     *
     * <p>Stamped when the request is admitted, before the judge runs, and it has
     * to be: {@link #getCreatedAt()} is written when the verdict comes back, so
     * timing a submission by it would charge every competitor for however long
     * the sandbox happened to take — a cold Java compile costs ten seconds that
     * the person who submitted did nothing to deserve.
     *
     * <p>It is also what lets a rejudge rebuild the standings identically months
     * later. The live scoreboard uses this number as each verdict lands, and the
     * rebuild reads it back off the row; without it the two would disagree, and
     * a rejudge that changed no verdict at all would still quietly reshuffle
     * everybody's finish time by the judging latency.
     */
    @Column(name = "contest_seconds")
    private Long contestSeconds;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Language language;

    @Lob
    @Column(name = "source_code", nullable = false, columnDefinition = "LONGTEXT")
    private String sourceCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private SubmissionStatus status = SubmissionStatus.PENDING;

    @Column(name = "runtime_ms")
    private Integer runtimeMs;

    @Column(name = "memory_kb")
    private Integer memoryKb;

    @Column(name = "passed_tests")
    private Integer passedTests;

    @Column(name = "total_tests")
    private Integer totalTests;

    @Lob
    @Column(name = "failure_message", columnDefinition = "TEXT")
    private String failureMessage;
}
