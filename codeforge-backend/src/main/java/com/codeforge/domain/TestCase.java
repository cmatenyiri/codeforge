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

/**
 * One judged case. Visible cases run on "Run"; hidden cases only on "Submit"
 * and are never serialized to a non-admin client.
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "test_cases")
public class TestCase extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "problem_id", nullable = false)
    private Problem problem;

    @Lob
    @Column(name = "input_text", nullable = false, columnDefinition = "LONGTEXT")
    private String input;

    @Lob
    @Column(name = "expected_output", nullable = false, columnDefinition = "LONGTEXT")
    private String expectedOutput;

    @Column(nullable = false)
    private boolean hidden = true;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;
}
