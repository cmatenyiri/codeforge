package com.codeforge.seed;

import com.codeforge.domain.DataType;
import com.codeforge.domain.Difficulty;
import com.codeforge.domain.Editorial;
import com.codeforge.domain.Problem;
import com.codeforge.domain.ProblemExample;
import com.codeforge.domain.ProblemHint;
import com.codeforge.domain.ProblemParameter;
import com.codeforge.domain.Slugs;
import com.codeforge.domain.Tag;
import com.codeforge.domain.TestCase;
import com.codeforge.repository.EditorialRepository;
import com.codeforge.repository.ProblemRepository;
import com.codeforge.repository.TagRepository;
import java.util.HashSet;
import java.util.Set;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Puts a starter catalogue in place on first run.
 *
 * <p>Inserts only when the problems table is empty, so it never fights with
 * edits made through the app; on a database that already has problems it only
 * backfills things that were added to the model after the row was written —
 * solution signatures, the difficulty rank the catalogue sorts by, and hidden
 * test cases the row predates. Switched off with
 * {@code codeforge.seed.problems=false} — it exists for local development, not
 * as a migration mechanism.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "codeforge.seed.problems", havingValue = "true", matchIfMissing = true)
public class ProblemSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ProblemSeeder.class);

    private final ProblemRepository problemRepository;
    private final TagRepository tagRepository;
    private final EditorialRepository editorialRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (problemRepository.count() > 0) {
            backfill();
            return;
        }

        Map<String, Tag> tags = createTags();
        List<Problem> problems = SeedCatalogue.build(tags);
        problemRepository.saveAll(problems);

        int editorials = createEditorials(problems);

        log.info(
                "Seeded {} problems across {} tags, with {} editorials",
                problems.size(),
                tags.size(),
                editorials);
    }

    /**
     * Brings problems seeded by an older version of this file up to date.
     *
     * <p>Without it a database from an earlier run keeps problems that can be
     * read but never solved, sort into the wrong order, or are judged against a
     * single hidden case. A signature already set, a rank already computed and a
     * case already present are left alone; the one thing it does rewrite is the
     * constraint block, which has to stay consistent with the test cases it is
     * quoted alongside.
     */
    private void backfill() {
        Map<String, Problem> authored = new LinkedHashMap<>();
        for (Problem problem : SeedCatalogue.build(Map.of())) {
            authored.put(problem.getSlug(), problem);
        }

        Set<String> existingEditorials = editorialRepository.findProblemSlugs();
        int signatures = 0;
        int ranks = 0;
        int cases = 0;
        int constraints = 0;

        for (Problem existing : problemRepository.findAll()) {
            if (existing.getDifficultyRank() == null) {
                // Re-setting it through the setter is what computes the rank.
                existing.setDifficulty(existing.getDifficulty());
                ranks++;
            }

            Problem template = authored.get(existing.getSlug());
            if (template == null) {
                continue;
            }

            // Sizing a hidden case to the top of a problem's constraints means the
            // two have to move together. A stored problem still claiming
            // "nums.length <= 10^4" while being judged against 300,000 elements
            // is a contradiction the solver cannot see past, so the constraint
            // text is the one piece of authored prose kept in step here.
            if (!Objects.equals(existing.getConstraintsMarkdown(), template.getConstraintsMarkdown())) {
                existing.setConstraintsMarkdown(template.getConstraintsMarkdown());
                constraints++;
            }

            if (existing.getFunctionName() == null && template.getFunctionName() != null) {
                existing.setFunctionName(template.getFunctionName());
                existing.setReturnType(template.getReturnType());
                existing.getParameters().clear();
                existing.getParameters().addAll(template.getParameters());
                signatures++;
            }

            cases += addMissingTestCases(existing, template);
        }

        // Editorials arrived after the first problems did, so a database from an
        // earlier run has none. Written for problems that lack one and never
        // rewritten, which is what leaves a hand-edited editorial alone.
        List<Problem> withoutEditorial = problemRepository.findAll().stream()
                .filter(problem -> !existingEditorials.contains(problem.getSlug()))
                .toList();
        int editorials = createEditorials(withoutEditorial);

        if (signatures > 0 || ranks > 0 || cases > 0 || constraints > 0 || editorials > 0) {
            log.info(
                    "Backfilled {} signatures, {} difficulty ranks, {} test cases,"
                            + " {} constraint blocks and {} editorials on existing problems",
                    signatures,
                    ranks,
                    cases,
                    constraints,
                    editorials);
        }
    }

    /**
     * Attaches the authored editorial to each problem that has one.
     *
     * <p>Saved separately from the problems rather than cascaded: the
     * association is owned by the editorial, and a problem with no editorial
     * written yet is a perfectly normal problem.
     */
    private int createEditorials(Collection<Problem> problems) {
        Map<String, EditorialCatalogue.Draft> drafts = EditorialCatalogue.build();
        List<Editorial> written = new ArrayList<>();

        for (Problem problem : problems) {
            EditorialCatalogue.Draft draft = drafts.get(problem.getSlug());
            if (draft == null) {
                continue;
            }

            Editorial editorial = new Editorial();
            editorial.setProblem(problem);
            editorial.setContentMarkdown(draft.contentMarkdown());
            editorial.setTimeComplexity(draft.timeComplexity());
            editorial.setSpaceComplexity(draft.spaceComplexity());
            editorial.getSolutions().putAll(draft.solutions());
            written.add(editorial);
        }

        editorialRepository.saveAll(written);
        return written.size();
    }

    /**
     * Adds catalogue cases the stored problem does not have.
     *
     * <p>Matched on the input text rather than on position: a problem seeded
     * before a case was inserted in the middle would otherwise have every later
     * case duplicated.
     */
    private int addMissingTestCases(Problem existing, Problem template) {
        Set<String> present = new HashSet<>();
        for (TestCase testCase : existing.getTestCases()) {
            present.add(testCase.getInput());
        }

        int added = 0;
        for (TestCase candidate : template.getTestCases()) {
            if (present.add(candidate.getInput())) {
                addTestCase(existing, candidate.getInput(), candidate.getExpectedOutput(), candidate.isHidden());
                added++;
            }
        }
        return added;
    }

    private Map<String, Tag> createTags() {
        Map<String, Tag> tags = new LinkedHashMap<>();
        List<String> names = SeedCatalogue.TAG_NAMES;

        List<Tag> saved = new ArrayList<>();
        for (String name : names) {
            Tag tag = new Tag();
            tag.setName(name);
            tag.setSlug(slugify(name));
            saved.add(tag);
        }
        for (Tag tag : tagRepository.saveAll(saved)) {
            tags.put(tag.getName(), tag);
        }
        return tags;
    }

    static String slugify(String value) {
        return Slugs.slugify(value);
    }

    /** Attaches an example to a problem, keeping both sides of the association in step. */
    static void addExample(Problem problem, String input, String output, String explanation) {
        ProblemExample example = new ProblemExample();
        example.setProblem(problem);
        example.setInput(input);
        example.setOutput(output);
        example.setExplanation(explanation);
        example.setDisplayOrder(problem.getExamples().size());
        problem.getExamples().add(example);
    }

    static void addHint(Problem problem, String content) {
        ProblemHint hint = new ProblemHint();
        hint.setProblem(problem);
        hint.setContent(content);
        hint.setDisplayOrder(problem.getHints().size());
        problem.getHints().add(hint);
    }

    static void addTestCase(Problem problem, String input, String expectedOutput, boolean hidden) {
        TestCase testCase = new TestCase();
        testCase.setProblem(problem);
        testCase.setInput(input);
        testCase.setExpectedOutput(expectedOutput);
        testCase.setHidden(hidden);
        testCase.setDisplayOrder(problem.getTestCases().size());
        problem.getTestCases().add(testCase);
    }

    /** Declares the function a solver implements, and how stdin maps onto it. */
    static void signature(Problem problem, String functionName, DataType returnType, ProblemParameter... parameters) {
        problem.setFunctionName(functionName);
        problem.setReturnType(returnType);
        problem.getParameters().addAll(List.of(parameters));
    }

    static ProblemParameter param(String name, DataType type) {
        return new ProblemParameter(name, type);
    }

    static Problem problem(String title, Difficulty difficulty, String description, String constraints, Tag... tags) {
        Problem problem = new Problem();
        problem.setTitle(title);
        problem.setSlug(slugify(title));
        problem.setDifficulty(difficulty);
        problem.setDescription(description);
        problem.setConstraintsMarkdown(constraints);
        // Nulls are tolerated so the signature backfill can rebuild the catalogue
        // with an empty tag map, without caring about tags it will not use.
        Arrays.stream(tags).filter(Objects::nonNull).forEach(problem.getTags()::add);
        return problem;
    }
}
