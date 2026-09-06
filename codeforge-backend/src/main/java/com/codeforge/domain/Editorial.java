package com.codeforge.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.MapKeyEnumerated;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.EnumMap;
import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * The written solution to a problem: how to think about it, and code that works.
 *
 * <p>A separate entity rather than columns on {@link Problem} for the same
 * reason the hidden test cases are fetched separately — a walkthrough plus four
 * reference solutions is far larger than the rest of a problem put together, and
 * the solving page loads the problem on every visit while the editorial is read
 * only when somebody opens the tab.
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(
        name = "editorials",
        uniqueConstraints = @UniqueConstraint(name = "uk_editorials_problem", columnNames = "problem_id"))
public class Editorial extends AuditableEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "problem_id", nullable = false)
    private Problem problem;

    /** Markdown: the approach, why the naive one is not enough, what to watch for. */
    @Lob
    @Column(name = "content_md", nullable = false, columnDefinition = "TEXT")
    private String contentMarkdown;

    /** Of the solution below, in the usual notation — {@code O(n log n)}. */
    @Column(name = "time_complexity", length = 48)
    private String timeComplexity;

    @Column(name = "space_complexity", length = 48)
    private String spaceComplexity;

    /**
     * Reference implementations, keyed by language.
     *
     * <p>Keyed rather than concatenated into the prose so the client can offer a
     * language picker over exactly the languages that were authored — the same
     * shape, and the same reasoning, as a problem's starter code.
     */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "editorial_solutions", joinColumns = @JoinColumn(name = "editorial_id"))
    @MapKeyEnumerated(EnumType.STRING)
    @MapKeyColumn(name = "language", length = 16)
    @Lob
    @Column(name = "source_code", nullable = false, columnDefinition = "LONGTEXT")
    private Map<Language, String> solutions = new EnumMap<>(Language.class);
}
