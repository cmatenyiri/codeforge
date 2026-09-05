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
            @Index(name = "ix_submissions_problem", columnList = "problem_id")
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
