package com.codeforge.validation;

import com.codeforge.domain.DataType;
import com.codeforge.domain.Language;
import com.codeforge.domain.Slugs;
import com.codeforge.repository.ProblemRepository;
import com.codeforge.repository.TagRepository;
import com.codeforge.web.dto.admin.EditorialPayload;
import com.codeforge.web.dto.admin.ProblemExamplePayload;
import com.codeforge.web.dto.admin.ProblemParameterPayload;
import com.codeforge.web.dto.admin.ProblemUpsertRequest;
import com.codeforge.web.dto.admin.TestCasePayload;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Validates an authored problem.
 *
 * <p>Two kinds of rule live here, and the difference between them is the whole
 * design of the authoring flow:
 *
 * <ul>
 *   <li><b>Always</b>: shape and uniqueness. A title fits its column, a slug is a
 *       slug, a parameter name is an identifier in all four languages, a test
 *       case has as many input lines as the function has arguments.
 *   <li><b>Only when publishing</b>: completeness. A signature, test cases, a
 *       sample the solver can run against, a worked example. A draft is allowed
 *       to be half-written — that is what a draft is for — and only becomes
 *       everyone's problem the moment it goes into the catalogue.
 * </ul>
 *
 * <p>Field names are indexed paths ({@code testCases[2].expectedOutput}) so the
 * form can put each message on the row that caused it, however deep in the
 * document it is.
 */
@Component
@RequiredArgsConstructor
public class ProblemUpsertRequestValidator {

    /** A problem may not take this slug: {@code /api/problems/random} already means something else. */
    private static final Set<String> RESERVED_SLUGS = Set.of("random", "new");

    private final ProblemRepository problemRepository;
    private final TagRepository tagRepository;

    /**
     * @param problemId the problem being edited, or null when creating one —
     *     uniqueness has to ignore the row it is checking on behalf of
     */
    public void validate(ProblemUpsertRequest request, Long problemId) {
        ValidationErrors errors = new ValidationErrors();

        validateTitle(request.title(), problemId, errors);
        validateSlug(request, problemId, errors);
        validateStatement(request, errors);
        validateTags(request.tagIds(), errors);
        validateSignature(request, errors);
        validateExamples(request.examples(), errors);
        validateHints(request.hints(), errors);
        validateTestCases(request, errors);
        validateEditorial(request.editorial(), errors);
        validateReadyToPublish(request, errors);

        errors.throwIfAny();
    }

    private void validateTitle(String rawTitle, Long problemId, ValidationErrors errors) {
        String title = ValidationRules.trimToNull(rawTitle);

        if (title == null) {
            errors.add("title", "validation.problem.title.required", "A title is required");
            return;
        }
        if (title.length() > ValidationRules.PROBLEM_TITLE_MAX_LENGTH) {
            errors.add(
                    "title",
                    "validation.problem.title.length",
                    "Title must be at most %d characters".formatted(ValidationRules.PROBLEM_TITLE_MAX_LENGTH));
            return;
        }
        if (exists(problemId, () -> problemRepository.existsByTitleIgnoreCase(title), id -> problemRepository
                .existsByTitleIgnoreCaseAndIdNot(title, id))) {
            errors.add("title", "validation.problem.title.taken", "Another problem already has that title");
        }
    }

    /**
     * The slug is optional in the request: blank means "derive it from the
     * title", which is what an author wants right up until a retitling would
     * otherwise break every link to the problem.
     */
    private void validateSlug(ProblemUpsertRequest request, Long problemId, ValidationErrors errors) {
        String slug = resolveSlug(request);

        if (slug.isEmpty()) {
            // Only reachable from a title with nothing sluggable in it — "***" —
            // and reported against the slug, which is the field that can fix it.
            errors.add("slug", "validation.problem.slug.required", "A URL slug is required");
            return;
        }
        if (slug.length() > ValidationRules.PROBLEM_SLUG_MAX_LENGTH) {
            errors.add(
                    "slug",
                    "validation.problem.slug.length",
                    "Slug must be at most %d characters".formatted(ValidationRules.PROBLEM_SLUG_MAX_LENGTH));
            return;
        }
        if (!ValidationRules.SLUG_PATTERN.matcher(slug).matches()) {
            errors.add(
                    "slug",
                    "validation.problem.slug.format",
                    "Slug may only contain lowercase letters, digits and single hyphens");
            return;
        }
        if (RESERVED_SLUGS.contains(slug)) {
            errors.add("slug", "validation.problem.slug.reserved", "That slug is reserved");
            return;
        }
        if (exists(
                problemId,
                () -> problemRepository.existsBySlug(slug),
                id -> problemRepository.existsBySlugAndIdNot(slug, id))) {
            errors.add("slug", "validation.problem.slug.taken", "Another problem already uses that slug");
        }
    }

    private void validateStatement(ProblemUpsertRequest request, ValidationErrors errors) {
        if (request.difficulty() == null) {
            errors.add("difficulty", "validation.problem.difficulty.required", "Choose a difficulty");
        }

        String description = ValidationRules.trimToNull(request.description());
        if (description == null) {
            errors.add("description", "validation.problem.description.required", "A description is required");
        } else {
            checkMarkdownLength("description", description, errors);
        }

        String constraints = ValidationRules.trimToNull(request.constraintsMarkdown());
        if (constraints != null) {
            checkMarkdownLength("constraintsMarkdown", constraints, errors);
        }
    }

    private void validateTags(List<Long> tagIds, ValidationErrors errors) {
        if (tagIds == null || tagIds.isEmpty()) {
            return;
        }
        // One query for the whole list rather than an exists per id.
        long found = tagRepository.findAllById(tagIds).size();
        if (found != Set.copyOf(tagIds).size()) {
            errors.add("tagIds", "validation.problem.tags.unknown", "One of those topics no longer exists");
        }
    }

    /**
     * The signature is all-or-nothing.
     *
     * <p>A function name without a return type generates nothing, so a problem
     * carrying half a signature is indistinguishable to the editor from one
     * carrying none — except that it looks, in the form, as though it had one.
     */
    private void validateSignature(ProblemUpsertRequest request, ValidationErrors errors) {
        String functionName = ValidationRules.trimToNull(request.functionName());
        List<ProblemParameterPayload> parameters = nullToEmpty(request.parameters());
        boolean started = functionName != null || request.returnType() != null || !parameters.isEmpty();

        if (!started) {
            return;
        }
        if (functionName == null) {
            errors.add("functionName", "validation.problem.functionName.required", "A function name is required");
        } else if (functionName.length() > ValidationRules.FUNCTION_NAME_MAX_LENGTH) {
            errors.add(
                    "functionName",
                    "validation.problem.functionName.length",
                    "Function name must be at most %d characters"
                            .formatted(ValidationRules.FUNCTION_NAME_MAX_LENGTH));
        } else if (!ValidationRules.IDENTIFIER_PATTERN.matcher(functionName).matches()) {
            errors.add(
                    "functionName",
                    "validation.problem.functionName.format",
                    "Function name must be a plain identifier, e.g. twoSum");
        }

        if (request.returnType() == null) {
            errors.add("returnType", "validation.problem.returnType.required", "Choose a return type");
        }

        Set<String> seen = new HashSet<>();
        for (int i = 0; i < parameters.size(); i++) {
            ProblemParameterPayload parameter = parameters.get(i);
            String field = "parameters[%d].name".formatted(i);
            String name = ValidationRules.trimToNull(parameter.name());

            if (name == null) {
                errors.add(field, "validation.problem.parameter.required", "Every argument needs a name");
            } else if (name.length() > ValidationRules.PARAMETER_NAME_MAX_LENGTH) {
                errors.add(
                        field,
                        "validation.problem.parameter.length",
                        "Argument name must be at most %d characters"
                                .formatted(ValidationRules.PARAMETER_NAME_MAX_LENGTH));
            } else if (!ValidationRules.IDENTIFIER_PATTERN.matcher(name).matches()) {
                errors.add(
                        field,
                        "validation.problem.parameter.format",
                        "Argument name must be a plain identifier, e.g. nums");
            } else if (!seen.add(name.toLowerCase(Locale.ROOT))) {
                // Case-insensitively, because two arguments differing only in case
                // compile in Java and read as a typo in every language.
                errors.add(field, "validation.problem.parameter.duplicate", "Two arguments share that name");
            }

            if (parameter.type() == null) {
                errors.add(
                        "parameters[%d].type".formatted(i),
                        "validation.problem.parameter.type",
                        "Choose a type for every argument");
            }
        }
    }

    private void validateExamples(List<ProblemExamplePayload> examples, ValidationErrors errors) {
        List<ProblemExamplePayload> list = nullToEmpty(examples);

        for (int i = 0; i < list.size(); i++) {
            ProblemExamplePayload example = list.get(i);

            if (ValidationRules.isBlank(example.input())) {
                errors.add(
                        "examples[%d].input".formatted(i),
                        "validation.problem.example.input",
                        "An example needs an input");
            }
            if (ValidationRules.isBlank(example.output())) {
                errors.add(
                        "examples[%d].output".formatted(i),
                        "validation.problem.example.output",
                        "An example needs an output");
            }
        }
    }

    private void validateHints(List<String> hints, ValidationErrors errors) {
        List<String> list = nullToEmpty(hints);

        for (int i = 0; i < list.size(); i++) {
            if (ValidationRules.isBlank(list.get(i))) {
                errors.add("hints[%d]".formatted(i), "validation.problem.hint.required", "A hint cannot be empty");
            }
        }
    }

    /**
     * Cases are checked against the signature, not just for shape.
     *
     * <p>The harness reads one line per argument, so a case carrying the wrong
     * number of them does not fail as a wrong answer — it fails as a crash inside
     * generated code the solver cannot see, on a case they may not even be shown.
     *
     * <p>Too many lines is always an error. Too few is only sometimes one: an
     * empty line is a perfectly good STRING — the empty-string case is the first
     * one worth writing for half the string problems in any catalogue — and
     * trailing empty values leave no trace in stored text. So a shortfall is
     * accepted exactly when every argument it would have supplied is a STRING,
     * which keeps "you forgot a line" caught for every numeric signature without
     * making the empty string unwritable.
     */
    private void validateTestCases(ProblemUpsertRequest request, ValidationErrors errors) {
        List<TestCasePayload> cases = nullToEmpty(request.testCases());
        int argumentCount = nullToEmpty(request.parameters()).size();
        boolean signed = ValidationRules.trimToNull(request.functionName()) != null && request.returnType() != null;

        for (int i = 0; i < cases.size(); i++) {
            TestCasePayload testCase = cases.get(i);
            String input = testCase.input() == null ? "" : testCase.input();

            if (input.length() > ValidationRules.TEST_CASE_MAX_LENGTH) {
                errors.add(
                        "testCases[%d].input".formatted(i),
                        "validation.problem.testCase.length",
                        "That input is too large");
            } else if (signed && !linesFit(lineCount(input), nullToEmpty(request.parameters()))) {
                errors.add(
                        "testCases[%d].input".formatted(i),
                        "validation.problem.testCase.lineCount",
                        "Expected one line per argument (%d)".formatted(argumentCount));
            }

            if (ValidationRules.isBlank(testCase.expectedOutput())) {
                errors.add(
                        "testCases[%d].expectedOutput".formatted(i),
                        "validation.problem.testCase.expected",
                        "Every case needs an expected output");
            } else if (testCase.expectedOutput().length() > ValidationRules.TEST_CASE_MAX_LENGTH) {
                errors.add(
                        "testCases[%d].expectedOutput".formatted(i),
                        "validation.problem.testCase.length",
                        "That expected output is too large");
            }
        }
    }

    private void validateEditorial(EditorialPayload editorial, ValidationErrors errors) {
        if (editorial == null) {
            return;
        }

        String content = ValidationRules.trimToNull(editorial.contentMarkdown());
        if (content == null) {
            errors.add(
                    "editorial.contentMarkdown",
                    "validation.problem.editorial.content",
                    "An editorial needs a walkthrough, or remove it");
        } else {
            checkMarkdownLength("editorial.contentMarkdown", content, errors);
        }

        checkComplexity("editorial.timeComplexity", editorial.timeComplexity(), errors);
        checkComplexity("editorial.spaceComplexity", editorial.spaceComplexity(), errors);

        Map<Language, String> solutions = editorial.solutions();
        if (solutions == null) {
            return;
        }
        for (Map.Entry<Language, String> solution : solutions.entrySet()) {
            if (ValidationRules.isBlank(solution.getValue())) {
                errors.add(
                        "editorial.solutions." + solution.getKey().name(),
                        "validation.problem.editorial.solution",
                        "A reference solution cannot be empty");
            }
        }
    }

    /**
     * The gate between a draft and the catalogue.
     *
     * <p>Everything here is reported against {@code published}, because that is
     * the one field the author can change to make the request valid: the answer
     * to "this problem has no test cases" is either to write some or to leave it
     * as a draft.
     */
    private void validateReadyToPublish(ProblemUpsertRequest request, ValidationErrors errors) {
        if (!request.published() || request.archived()) {
            return;
        }

        List<TestCasePayload> cases = nullToEmpty(request.testCases());
        boolean signed = ValidationRules.trimToNull(request.functionName()) != null && request.returnType() != null;

        if (!signed) {
            errors.add(
                    "published",
                    "validation.problem.publish.signature",
                    "A published problem needs a solution signature");
        }
        if (nullToEmpty(request.examples()).isEmpty()) {
            errors.add("published", "validation.problem.publish.examples", "A published problem needs an example");
        }
        if (cases.isEmpty()) {
            errors.add("published", "validation.problem.publish.testCases", "A published problem needs test cases");
        } else if (cases.stream().allMatch(TestCasePayload::hidden)) {
            errors.add(
                    "published",
                    "validation.problem.publish.samples",
                    "A published problem needs at least one sample case to run against");
        }
    }

    /** The slug as it will be stored: what the author typed, or the title reduced to one. */
    public static String resolveSlug(ProblemUpsertRequest request) {
        String slug = ValidationRules.trimToNull(request.slug());
        return slug == null
                ? Slugs.slugify(request.title() == null ? "" : request.title())
                : slug.toLowerCase(Locale.ROOT);
    }

    /**
     * Whether an input of {@code lines} lines can feed this argument list.
     *
     * <p>Exact, except that the arguments a shortfall would have supplied must
     * all be STRINGs — the one type whose value can be an empty line, and so the
     * one type a stored input cannot record the presence of.
     */
    private static boolean linesFit(int lines, List<ProblemParameterPayload> parameters) {
        if (lines == parameters.size()) {
            return true;
        }
        if (lines > parameters.size()) {
            return false;
        }
        return parameters.subList(lines, parameters.size()).stream()
                .allMatch(parameter -> parameter.type() == DataType.STRING);
    }

    /**
     * How many lines a test case input carries.
     *
     * <p>An empty input is no lines, which is the right answer for a function
     * that takes no arguments; anything else is one more line than it has
     * separators, blank lines included — an empty STRING argument is a line.
     */
    private static int lineCount(String input) {
        String trimmed = input.strip().isEmpty() ? "" : input.stripTrailing();
        return trimmed.isEmpty() ? 0 : (int) trimmed.chars().filter(character -> character == '\n').count() + 1;
    }

    private void checkMarkdownLength(String field, String value, ValidationErrors errors) {
        if (value.length() > ValidationRules.MARKDOWN_MAX_LENGTH) {
            errors.add(field, "validation.problem.markdown.length", "That text is too long");
        }
    }

    private void checkComplexity(String field, String value, ValidationErrors errors) {
        String complexity = ValidationRules.trimToNull(value);
        if (complexity != null && complexity.length() > ValidationRules.COMPLEXITY_MAX_LENGTH) {
            errors.add(
                    field,
                    "validation.problem.complexity.length",
                    "Use a short form such as O(n log n)");
        }
    }

    /** Runs the create-time check or the edit-time one, depending on which this is. */
    private static boolean exists(Long problemId, BooleanSupplier whenCreating, Predicate<Long> whenEditing) {
        return problemId == null ? whenCreating.getAsBoolean() : whenEditing.test(problemId);
    }

    private static <T> List<T> nullToEmpty(List<T> values) {
        return values == null ? List.of() : values;
    }
}
