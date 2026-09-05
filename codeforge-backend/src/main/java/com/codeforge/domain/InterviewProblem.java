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

    @Column(nullable = false)
    private boolean solved = false;

    @Column(nullable = false)
    private boolean skipped = false;

    /** The submission that solved this slot, if any. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "solved_by_submission_id")
    private Submission solvedBySubmission;
}
