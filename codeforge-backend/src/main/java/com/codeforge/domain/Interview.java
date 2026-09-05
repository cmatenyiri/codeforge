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
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
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
    private InterviewStatus status = InterviewStatus.IN_PROGRESS;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    /** Solved problems out of the assigned set; null until the interview ends. */
    @Column(name = "score")
    private Integer score;

    @OrderBy("position ASC")
    @OneToMany(mappedBy = "interview", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InterviewProblem> problems = new ArrayList<>();
}
