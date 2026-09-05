package com.codeforge.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A worked example shown in the problem description. */
@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "problem_examples")
public class ProblemExample extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "problem_id", nullable = false)
    private Problem problem;

    @Lob
    @Column(name = "input_text", nullable = false, columnDefinition = "TEXT")
    private String input;

    @Lob
    @Column(name = "output_text", nullable = false, columnDefinition = "TEXT")
    private String output;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String explanation;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;
}
