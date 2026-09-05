package com.codeforge.seed;

import com.codeforge.domain.DataType;
import com.codeforge.domain.Difficulty;
import com.codeforge.domain.Problem;
import com.codeforge.domain.ProblemExample;
import com.codeforge.domain.ProblemHint;
import com.codeforge.domain.ProblemParameter;
import com.codeforge.domain.Tag;
import com.codeforge.domain.TestCase;
import com.codeforge.repository.ProblemRepository;
import com.codeforge.repository.TagRepository;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
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
 * backfills solution signatures onto rows seeded before those existed. Switched
 * off with
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

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (problemRepository.count() > 0) {
            backfillSignatures();
            return;
        }

        Map<String, Tag> tags = createTags();
        List<Problem> problems = SeedCatalogue.build(tags);
        problemRepository.saveAll(problems);

        log.info("Seeded {} problems across {} tags", problems.size(), tags.size());
    }

    /**
     * Fills in signatures on problems seeded before they existed.
     *
     * <p>Without this a database from an earlier run keeps problems that can be
     * read but never solved, and the only fix is to drop the table. It touches
     * nothing else — a problem that already has a signature is left alone, so
     * this cannot overwrite anything authored by hand.
     */
    private void backfillSignatures() {
        Map<String, Problem> authored = new LinkedHashMap<>();
        for (Problem problem : SeedCatalogue.build(Map.of())) {
            authored.put(problem.getSlug(), problem);
        }

        int updated = 0;
        for (Problem existing : problemRepository.findAll()) {
            Problem template = authored.get(existing.getSlug());
            if (template == null || existing.getFunctionName() != null || template.getFunctionName() == null) {
                continue;
            }

            existing.setFunctionName(template.getFunctionName());
            existing.setReturnType(template.getReturnType());
            existing.getParameters().clear();
            existing.getParameters().addAll(template.getParameters());
            updated++;
        }

        if (updated > 0) {
            log.info("Backfilled solution signatures on {} existing problems", updated);
        }
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
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
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
