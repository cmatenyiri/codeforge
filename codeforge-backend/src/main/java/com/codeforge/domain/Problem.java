package com.codeforge.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Formula;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A single coding problem, its examples and its test cases. */
@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(
        name = "problems",
        uniqueConstraints = {
            @UniqueConstraint(name = "uk_problems_title", columnNames = "title"),
            @UniqueConstraint(name = "uk_problems_slug", columnNames = "slug")
        })
public class Problem extends AuditableEntity {

    @Column(nullable = false, length = 160)
    private String title;

    @Column(nullable = false, length = 180)
    private String slug;

    /** Markdown. */
    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    /** Markdown. Column is not named `constraints` — that is reserved in MySQL. */
    @Lob
    @Column(name = "constraints_md", columnDefinition = "TEXT")
    private String constraintsMarkdown;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Difficulty difficulty;

    /**
     * {@link Difficulty#rank()}, denormalised so the catalogue can be ordered by
     * difficulty in SQL. Maintained by {@link #setDifficulty}; never set by hand.
     */
    @Column(name = "difficulty_rank")
    private Integer difficultyRank;

    /**
     * Whether the problem is visible in the catalogue.
     *
     * <p>A new problem starts unpublished, which is what makes authoring in the
     * app safe: a half-written statement or a test case whose expected output has
     * not been checked yet is invisible to solvers until an author says otherwise.
     *
     * <p>The column carries a database-level default of true so that problems
     * written before this flag existed — every seeded one — stay in the catalogue
     * when the column is added, rather than all disappearing at once.
     */
    @Column(nullable = false, columnDefinition = "boolean default true")
    private boolean published = true;

    /**
     * Retired: hidden from the catalogue, but kept so the submissions against it
     * still resolve. The reversible alternative to deleting a problem people
     * have already solved.
     */
    @Column(nullable = false)
    private boolean archived = false;

    // ── Submission counters ───────────────────────────────────────────────
    // Kept on the problem rather than recomputed, so listing a page of problems
    // costs no aggregate over the submissions table. They are incremented by a
    // single atomic UPDATE per submission, which is also what keeps two
    // concurrent submissions from losing one of the two increments.

    @Column(name = "total_submissions", nullable = false)
    private long totalSubmissions;

    @Column(name = "accepted_submissions", nullable = false)
    private long acceptedSubmissions;

    /**
     * Accepted / total, or null when nobody has submitted yet.
     *
     * <p>Read-only and computed by the database, which is what lets the catalogue
     * be <em>sorted</em> by acceptance without a second query — a ratio of two
     * columns is not a property Spring Data could sort by on its own.
     */
    @Formula("(case when total_submissions = 0 then null"
            + " else accepted_submissions * 1.0 / total_submissions end)")
    private Double acceptanceRate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    // ── Solution signature ────────────────────────────────────────────────
    // Together these describe the function a solver writes. One declaration
    // drives three things that would otherwise be authored by hand for every
    // (problem, language) pair: the starter code, the harness that feeds stdin
    // into the function, and the canonical formatting of its return value.

    /** Name of the function to implement, e.g. {@code twoSum}. */
    @Column(name = "function_name", length = 64)
    private String functionName;

    @Enumerated(EnumType.STRING)
    @Column(name = "return_type", length = 32)
    private DataType returnType;

    /** In signature order, which is also the order of the input lines per case. */
    @OrderColumn(name = "position")
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "problem_parameters", joinColumns = @JoinColumn(name = "problem_id"))
    private List<ProblemParameter> parameters = new ArrayList<>();

    // Batch fetching turns the per-row tag lookup on a listing page into one
    // extra query instead of one per problem.
    @BatchSize(size = 32)
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "problem_tags",
            joinColumns = @JoinColumn(name = "problem_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private Set<Tag> tags = new LinkedHashSet<>();

    @OrderBy("displayOrder ASC")
    @OneToMany(mappedBy = "problem", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProblemExample> examples = new ArrayList<>();

    @OrderBy("displayOrder ASC")
    @OneToMany(mappedBy = "problem", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProblemHint> hints = new ArrayList<>();

    @OrderBy("displayOrder ASC")
    @OneToMany(mappedBy = "problem", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TestCase> testCases = new ArrayList<>();

    /** Also keeps {@link #difficultyRank} in step, so the two can never disagree. */
    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
        this.difficultyRank = difficulty == null ? null : difficulty.rank();
    }
}
