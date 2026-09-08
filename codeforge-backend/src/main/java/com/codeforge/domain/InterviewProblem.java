package com.codeforge.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Duration;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** One slot in an interview: which problem, in which position, and how it went. */
@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(
        name = "interview_problems",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_interview_problem_position",
                        columnNames = {"interview_id", "position"}))
public class InterviewProblem extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "interview_id", nullable = false)
    private Interview interview;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "problem_id", nullable = false)
    private Problem problem;

    @Column(name = "position", nullable = false)
    private int position;

    /**
     * The problem exactly as it stood when the round began.
     *
     * <p>What the candidate reads and what the judge judges, for as long as the
     * round lasts. The association above still points at the live catalogue row —
     * that is what a submission is recorded against and what the debrief links to
     * afterwards — but nothing inside the round reads through it, so an author
     * editing the problem cannot reach a round already in progress. See
     * {@link ProblemSnapshot} for why that matters.
     */
    @Convert(converter = ProblemSnapshotConverter.class)
    @Column(name = "problem_snapshot", nullable = false, columnDefinition = "LONGTEXT")
    private ProblemSnapshot snapshot;

    @Column(nullable = false)
    private boolean solved = false;

    @Column(nullable = false)
    private boolean skipped = false;

    /**
     * When the candidate first put this problem on screen.
     *
     * <p>Time-to-solve is measured from here rather than from the start of the
     * interview, so the second problem is not charged for the time the first one
     * took. It is stamped once and never moved: coming back to a problem should
     * not reset the clock on it.
     */
    @Column(name = "opened_at")
    private Instant openedAt;

    @Column(name = "solved_at")
    private Instant solvedAt;

    /**
     * Submissions made against this slot, accepted or not.
     *
     * <p>Kept separately from the submission history because the ratio is the
     * interesting number: four attempts on a solved problem is a candidate
     * compiling in the judge rather than reading their own code, and that is
     * worth saying out loud in the report.
     */
    @Column(nullable = false)
    private int attempts = 0;

    /** How many of the problem's hints were opened, in order. */
    @Column(name = "hints_revealed", nullable = false)
    private int hintsRevealed = 0;

    /** The submission that solved this slot, if any. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "solved_by_submission_id")
    private Submission solvedBySubmission;

    /**
     * The last attempt sent to the judge, accepted or not.
     *
     * <p>What a closed problem shows. The editor's buffer is a local draft and
     * keeps taking keystrokes while a submission is being judged — so by the
     * time an acceptance comes back and locks the problem, the text on screen
     * may be something that was never judged at all. This is the copy the
     * verdict was actually about, and unlike a draft it survives a reload, a
     * different browser and cleared site data.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_submission_id")
    private Submission lastSubmission;

    /** The submission a closed problem should display: the one that solved it, else the last try. */
    public Submission displayedSubmission() {
        return solvedBySubmission != null ? solvedBySubmission : lastSubmission;
    }

    /**
     * Seconds between opening the problem and solving it, or null while either
     * end of that interval is missing.
     */
    public Integer timeToSolveSeconds() {
        if (openedAt == null || solvedAt == null) {
            return null;
        }
        return (int) Math.max(0, Duration.between(openedAt, solvedAt).toSeconds());
    }

    /**
     * Whether this slot is finished with, one way or the other.
     *
     * <p>The unit the round advances on. An interviewer moves you off a question
     * when you have cracked it <em>or</em> when you have decided to leave it, and
     * both readings end the same way: there is nothing more to do here.
     */
    public boolean isResolved() {
        return solved || skipped;
    }

    /** The opener of a multi-problem round: the one pitched to be the gentler. */
    public boolean isWarmUp() {
        return position == 0 && interview != null && interview.getProblems().size() > 1;
    }
}
