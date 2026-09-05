package com.codeforge.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One argument of a problem's solution function.
 *
 * <p>The declaration order is the order the arguments appear on stdin: a test
 * case supplies exactly one line per parameter, in this order.
 */
@Getter
@Setter
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
public class ProblemParameter {

    @Column(name = "param_name", nullable = false, length = 64)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "param_type", nullable = false, length = 32)
    private DataType type;
}
