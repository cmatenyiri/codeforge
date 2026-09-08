package com.codeforge.service;

import com.codeforge.domain.Difficulty;
import com.codeforge.domain.Editorial;
import com.codeforge.domain.Language;
import com.codeforge.domain.Problem;
import com.codeforge.domain.ProblemExample;
import com.codeforge.domain.ProblemHint;
import com.codeforge.domain.ProblemParameter;
import com.codeforge.domain.ProblemState;
import com.codeforge.domain.Slugs;
import com.codeforge.domain.Tag;
import com.codeforge.domain.TestCase;
import com.codeforge.exception.BusinessRuleException;
import com.codeforge.exception.NotFoundException;
import com.codeforge.execution.codegen.CodeTemplateService;
import com.codeforge.repository.ContestRepository;
import com.codeforge.repository.EditorialRepository;
import com.codeforge.repository.ProblemRepository;
import com.codeforge.repository.SubmissionRepository;
import com.codeforge.repository.TagRepository;
import com.codeforge.repository.TestCaseRepository;
import com.codeforge.repository.UserRepository;
import com.codeforge.security.SecurityUtils;
import com.codeforge.validation.ProblemUpsertRequestValidator;
import com.codeforge.validation.ValidationRules;
import com.codeforge.validation.TagCreateRequestValidator;
import com.codeforge.web.dto.admin.EditorialPayload;
import com.codeforge.web.dto.admin.ProblemExamplePayload;
import com.codeforge.web.dto.admin.ProblemParameterPayload;
import com.codeforge.web.dto.admin.ProblemUpsertRequest;
import com.codeforge.web.dto.admin.SignaturePreviewRequest;
import com.codeforge.web.dto.admin.TagCreateRequest;
import com.codeforge.web.dto.admin.TestCasePayload;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Writing problems, from the app.
 *
 * <p>Separate from {@link ProblemService} because the two answer to opposite
 * requirements. That one serves solvers: it hides drafts, never loads a hidden
 * test case, and is the hottest read path in the product. This one serves the
 * handful of people who write the catalogue: every method is
 * {@code hasRole('ADMIN')}, every read is the whole problem including the cases
 * that decide submissions, and none of it is on a page a solver ever opens.
 *
 * <p>The unit of work is the entire problem. An author edits a statement, its
 * examples, its signature, its cases and its editorial as one document, so a save
 * carries all of it and this service reconciles that document against what is
 * stored — matching child rows by id where the form sent one, so that fixing a
 * typo in one hint does not delete and recreate the other five.
 */
@Service
@RequiredArgsConstructor
public class ProblemAuthoringService {

    /** Leaves room for the " (copy 99)" a duplicate's title gains. */
    private static final int COPY_TITLE_BASE_MAX_LENGTH = ValidationRules.PROBLEM_TITLE_MAX_LENGTH - 12;

    private final ProblemRepository problemRepository;
    private final TagRepository tagRepository;
    private final TestCaseRepository testCaseRepository;
    private final ContestRepository contestRepository;
    private final EditorialRepository editorialRepository;
    private final SubmissionRepository submissionRepository;
    private final UserRepository userRepository;
    private final CodeTemplateService codeTemplateService;
    private final ProblemUpsertRequestValidator problemValidator;
    private final TagCreateRequestValidator tagValidator;

    /**
     * The authoring catalogue: drafts, published problems and archived ones
     * together, narrowed by the same filters plus their state.
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public Page<Problem> search(String search, Difficulty difficulty, String tagSlug, ProblemState state,
            Pageable pageable) {

        Page<Problem> problems = problemRepository.searchForAuthor(
                search, difficulty, tagSlug, state == null ? null : state.name(), pageable);

        // Mapping happens outside this transaction and `open-in-view` is off, so
        // the tags every row shows have to be pulled in here; @BatchSize makes it
        // one extra query for the page rather than one per problem.
        problems.forEach(problem -> problem.getTags().size());

        return problems;
    }

    /** Case counts for a whole page of rows, in one query. */
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public Map<Long, TestCaseCounts> testCaseCounts(List<Long> problemIds) {
        if (problemIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, TestCaseCounts> counts = new HashMap<>();
        for (TestCaseRepository.TestCaseCounts row : testCaseRepository.countByProblemIds(problemIds)) {
            counts.put(row.getProblemId(), new TestCaseCounts((int) row.getTotal(), (int) row.getHidden()));
        }
        return counts;
    }

    /** Which of these problems have an editorial, in one query. */
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public Set<Long> problemIdsWithEditorial(List<Long> problemIds) {
        return problemIds.isEmpty() ? Set.of() : editorialRepository.findProblemIdsIn(problemIds);
    }

    /**
     * One problem with everything on it — hidden cases and editorial included.
     *
     * <p>The opposite trade-off from the solving page, which deliberately loads
     * neither: the form cannot edit what it was not sent, and an author opening a
     * problem is a rare, deliberate act rather than a page view.
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public Authored get(Long id) {
        Problem problem = load(id);

        // Separate collection reads rather than one fetch join: three List
        // associations in a single join is Hibernate's MultipleBagFetchException.
        problem.getTags().size();
        problem.getParameters().size();
        problem.getExamples().size();
        problem.getHints().size();
        problem.getTestCases().size();

        Editorial editorial = editorialRepository.findByProblemId(id).orElse(null);
        if (editorial != null) {
            editorial.getSolutions().size();
        }

        return new Authored(problem, editorial, codeTemplateService.starterCode(problem));
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public Long create(ProblemUpsertRequest request) {
        problemValidator.validate(request, null);

        Problem problem = new Problem();
        problem.setCreatedBy(userRepository.getReferenceById(SecurityUtils.requireCurrentUserId()));
        apply(problem, request);

        // Saved before the editorial is written: the editorial owns the
        // association, so it needs a problem that already has an id.
        Problem saved = problemRepository.save(problem);
        applyEditorial(saved, request.editorial());

        return saved.getId();
    }

    /**
     * Rewrites a problem from the form's document.
     *
     * <p>Note what is not touched: the submission counters. They are the record
     * of what solvers have actually done with the problem, and an edit — even one
     * that changes every test case — is not a reason to pretend it never happened.
     */
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void update(Long id, ProblemUpsertRequest request) {
        problemValidator.validate(request, id);

        Problem problem = load(id);
        problem.getTags().size();
        problem.getParameters().size();
        problem.getExamples().size();
        problem.getHints().size();
        problem.getTestCases().size();

        apply(problem, request);
        problemRepository.save(problem);
        applyEditorial(problem, request.editorial());
    }

    /**
     * Copies a problem, as an unpublished draft.
     *
     * <p>The usual way a new problem starts: most of them are a variation on one
     * that exists, and re-typing four test cases to change one of them is how
     * transcription errors get into a catalogue.
     */
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public Long duplicate(Long id) {
        Problem source = get(id).problem();

        Problem copy = new Problem();
        copy.setTitle(freeTitle(source.getTitle()));
        copy.setSlug(freeSlug(Slugs.slugify(copy.getTitle())));
        copy.setDifficulty(source.getDifficulty());
        copy.setDescription(source.getDescription());
        copy.setConstraintsMarkdown(source.getConstraintsMarkdown());
        // Always a draft, whatever the original was: a copy is by definition not
        // the problem that was reviewed, and its slug is a placeholder.
        copy.setPublished(false);
        copy.setArchived(false);
        copy.setCreatedBy(userRepository.getReferenceById(SecurityUtils.requireCurrentUserId()));
        copy.setFunctionName(source.getFunctionName());
        copy.setReturnType(source.getReturnType());
        copy.getParameters().addAll(source.getParameters());
        copy.getTags().addAll(source.getTags());

        for (ProblemExample example : source.getExamples()) {
            addExample(copy, example.getInput(), example.getOutput(), example.getExplanation());
        }
        for (ProblemHint hint : source.getHints()) {
            addHint(copy, hint.getContent());
        }
        for (TestCase testCase : source.getTestCases()) {
            addTestCase(copy, testCase.getInput(), testCase.getExpectedOutput(), testCase.isHidden());
        }

        Problem saved = problemRepository.save(copy);

        editorialRepository.findByProblemId(id).ifPresent(source_ -> {
            Editorial editorial = new Editorial();
            editorial.setProblem(saved);
            editorial.setContentMarkdown(source_.getContentMarkdown());
            editorial.setTimeComplexity(source_.getTimeComplexity());
            editorial.setSpaceComplexity(source_.getSpaceComplexity());
            editorial.getSolutions().putAll(source_.getSolutions());
            editorialRepository.save(editorial);
        });

        return saved.getId();
    }

    /**
     * Publishes or unpublishes, from the catalogue's row menu.
     *
     * <p>Publishing runs the same completeness rules a save does, by re-validating
     * the stored problem as though it had just been submitted: the quick action
     * and the form must not be able to disagree about what is fit to release.
     */
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void setPublished(Long id, boolean published) {
        Authored authored = get(id);
        Problem problem = authored.problem();

        if (published) {
            problemValidator.validate(asRequest(authored, true), id);
        }
        problem.setPublished(published);
        problemRepository.save(problem);
    }

    /** Retires a problem, or brings it back. Reversible, unlike a delete. */
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void setArchived(Long id, boolean archived) {
        Problem problem = load(id);
        problem.setArchived(archived);
        problemRepository.save(problem);
    }

    /**
     * Deletes a problem outright.
     *
     * <p>Refused once anyone has submitted to it or an interview has asked it:
     * both hold a reference that only makes sense while the problem exists — a
     * submission history would lose its title, a debrief would name nothing.
     * Archiving is the answer for those, and the client says so.
     */
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(Long id) {
        Problem problem = load(id);

        if (submissionRepository.countByProblemId(id) > 0) {
            throw new BusinessRuleException(
                    "error.problem.hasSubmissions", "This problem has submissions; archive it instead");
        }
        if (problemRepository.isUsedByInterviews(id)) {
            throw new BusinessRuleException(
                    "error.problem.usedByInterviews", "This problem has been asked in an interview; archive it instead");
        }
        // A contest names the problems it asked and links to each one, and its
        // standings are permanent — so deleting one would leave a finished
        // contest pointing at nothing, exactly as an interview debrief would.
        if (contestRepository.isUsedByContests(id)) {
            throw new BusinessRuleException(
                    "error.problem.usedByContests", "This problem has been asked in a contest; archive it instead");
        }

        editorialRepository.deleteByProblemId(id);
        problemRepository.delete(problem);
    }

    /** Starter code for a signature the author has not saved yet. */
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public Map<Language, String> previewStarterCode(SignaturePreviewRequest request) {
        Problem draft = new Problem();
        draft.setFunctionName(request.functionName());
        draft.setReturnType(request.returnType());
        applyParameters(draft, request.parameters());

        return codeTemplateService.starterCode(draft);
    }

    /**
     * Adds a topic.
     *
     * <p>Lives here rather than in {@link TagService} because creating one is an
     * authoring act: the filter list on the catalogue is a closed vocabulary, and
     * it stays useful only while a new entry is a deliberate decision rather than
     * a by-product of a typo in a problem form.
     */
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public Tag createTag(TagCreateRequest request) {
        tagValidator.validate(request);

        Tag tag = new Tag();
        tag.setName(request.name().trim());
        tag.setSlug(Slugs.slugify(tag.getName()));

        return tagRepository.save(tag);
    }

    // ── Writing the document onto the entity ──────────────────────────────

    private void apply(Problem problem, ProblemUpsertRequest request) {
        problem.setTitle(request.title().trim());
        problem.setSlug(ProblemUpsertRequestValidator.resolveSlug(request));
        problem.setDifficulty(request.difficulty());
        problem.setDescription(request.description().trim());
        problem.setConstraintsMarkdown(blankToNull(request.constraintsMarkdown()));
        problem.setPublished(request.published());
        problem.setArchived(request.archived());
        problem.setFunctionName(blankToNull(request.functionName()));
        problem.setReturnType(request.returnType());

        applyParameters(problem, request.parameters());
        applyTags(problem, request.tagIds());
        applyExamples(problem, nullToEmpty(request.examples()));
        applyHints(problem, nullToEmpty(request.hints()));
        applyTestCases(problem, nullToEmpty(request.testCases()));
    }

    private static void applyParameters(Problem problem, List<ProblemParameterPayload> parameters) {
        problem.getParameters().clear();
        for (ProblemParameterPayload parameter : nullToEmpty(parameters)) {
            problem.getParameters().add(new ProblemParameter(parameter.name().trim(), parameter.type()));
        }
    }

    private void applyTags(Problem problem, List<Long> tagIds) {
        Set<Tag> tags = new LinkedHashSet<>(tagRepository.findAllById(nullToEmpty(tagIds)));
        problem.getTags().clear();
        problem.getTags().addAll(tags);
    }

    /**
     * Reconciles the examples the form sent against the ones that are stored.
     *
     * <p>Matched by id rather than rebuilt wholesale, so an edit updates the rows
     * it changed and leaves the rest alone. Position in the submitted list is the
     * display order, which is what makes reordering in the form work at all.
     */
    private static void applyExamples(Problem problem, List<ProblemExamplePayload> payloads) {
        Map<Long, ProblemExample> existing = byId(problem.getExamples(), ProblemExample::getId);
        List<ProblemExample> next = new ArrayList<>(payloads.size());

        for (int i = 0; i < payloads.size(); i++) {
            ProblemExamplePayload payload = payloads.get(i);
            ProblemExample example = Optional.ofNullable(payload.id())
                    .map(existing::get)
                    .orElseGet(ProblemExample::new);

            example.setProblem(problem);
            example.setInput(payload.input().strip());
            example.setOutput(payload.output().strip());
            example.setExplanation(blankToNull(payload.explanation()));
            example.setDisplayOrder(i);
            next.add(example);
        }

        // Cleared and refilled rather than reassigned: the collection instance is
        // the one Hibernate tracks, and replacing it is what raises
        // "a collection with cascade=all-delete-orphan was no longer referenced".
        problem.getExamples().clear();
        problem.getExamples().addAll(next);
    }

    private static void applyHints(Problem problem, List<String> contents) {
        // Hints are bare strings in the payload — there is nothing to match on but
        // position, and reusing rows in order keeps the ids stable for an edit
        // that only changes the wording.
        List<ProblemHint> existing = List.copyOf(problem.getHints());
        List<ProblemHint> next = new ArrayList<>(contents.size());

        for (int i = 0; i < contents.size(); i++) {
            ProblemHint hint = i < existing.size() ? existing.get(i) : new ProblemHint();
            hint.setProblem(problem);
            hint.setContent(contents.get(i).strip());
            hint.setDisplayOrder(i);
            next.add(hint);
        }

        problem.getHints().clear();
        problem.getHints().addAll(next);
    }

    private static void applyTestCases(Problem problem, List<TestCasePayload> payloads) {
        Map<Long, TestCase> existing = byId(problem.getTestCases(), TestCase::getId);
        List<TestCase> next = new ArrayList<>(payloads.size());

        for (int i = 0; i < payloads.size(); i++) {
            TestCasePayload payload = payloads.get(i);
            TestCase testCase =
                    Optional.ofNullable(payload.id()).map(existing::get).orElseGet(TestCase::new);

            testCase.setProblem(problem);
            // Trailing whitespace is stripped on both sides before a comparison
            // anyway; storing it would only make two identical cases look different.
            testCase.setInput(payload.input() == null ? "" : payload.input().stripTrailing());
            testCase.setExpectedOutput(payload.expectedOutput().strip());
            testCase.setHidden(payload.hidden());
            testCase.setDisplayOrder(i);
            next.add(testCase);
        }

        problem.getTestCases().clear();
        problem.getTestCases().addAll(next);
    }

    /**
     * Writes, rewrites or removes the problem's editorial.
     *
     * <p>Not cascaded from the problem: the association is owned by the editorial,
     * and "no editorial" is a normal, common state rather than a missing part.
     */
    private void applyEditorial(Problem problem, EditorialPayload payload) {
        Optional<Editorial> stored = editorialRepository.findByProblemId(problem.getId());

        if (payload == null) {
            stored.ifPresent(editorialRepository::delete);
            return;
        }

        Editorial editorial = stored.orElseGet(Editorial::new);
        editorial.setProblem(problem);
        editorial.setContentMarkdown(payload.contentMarkdown().strip());
        editorial.setTimeComplexity(blankToNull(payload.timeComplexity()));
        editorial.setSpaceComplexity(blankToNull(payload.spaceComplexity()));
        editorial.getSolutions().clear();
        if (payload.solutions() != null) {
            payload.solutions().forEach(editorial.getSolutions()::put);
        }

        editorialRepository.save(editorial);
    }

    /** The stored problem expressed as the request that would recreate it. */
    private static ProblemUpsertRequest asRequest(Authored authored, boolean published) {
        Problem problem = authored.problem();

        return new ProblemUpsertRequest(
                problem.getTitle(),
                problem.getSlug(),
                problem.getDifficulty(),
                problem.getDescription(),
                problem.getConstraintsMarkdown(),
                published,
                problem.isArchived(),
                problem.getTags().stream().map(Tag::getId).toList(),
                problem.getFunctionName(),
                problem.getReturnType(),
                problem.getParameters().stream()
                        .map(parameter -> new ProblemParameterPayload(parameter.getName(), parameter.getType()))
                        .toList(),
                problem.getExamples().stream()
                        .map(example -> new ProblemExamplePayload(
                                example.getId(), example.getInput(), example.getOutput(), example.getExplanation()))
                        .toList(),
                problem.getHints().stream().map(ProblemHint::getContent).toList(),
                problem.getTestCases().stream()
                        .map(testCase -> new TestCasePayload(
                                testCase.getId(),
                                testCase.getInput(),
                                testCase.getExpectedOutput(),
                                testCase.isHidden()))
                        .toList(),
                authored.editorial() == null
                        ? null
                        : new EditorialPayload(
                                authored.editorial().getContentMarkdown(),
                                authored.editorial().getTimeComplexity(),
                                authored.editorial().getSpaceComplexity(),
                                authored.editorial().getSolutions()));
    }

    // ── Small helpers ─────────────────────────────────────────────────────

    private Problem load(Long id) {
        return problemRepository.findById(id).orElseThrow(() -> NotFoundException.of("problem", id));
    }

    /** "Two Sum" → "Two Sum (copy)", then "(copy 2)" — the first title nothing else holds. */
    private String freeTitle(String title) {
        // Trimmed first so that copying a problem whose title already fills the
        // column does not produce one the column cannot hold.
        String base = title.length() > COPY_TITLE_BASE_MAX_LENGTH
                ? title.substring(0, COPY_TITLE_BASE_MAX_LENGTH).strip()
                : title;

        String candidate = base + " (copy)";
        for (int suffix = 2; problemRepository.existsByTitleIgnoreCase(candidate); suffix++) {
            candidate = "%s (copy %d)".formatted(base, suffix);
        }
        return candidate;
    }

    private String freeSlug(String slug) {
        String candidate = slug;
        for (int suffix = 2; problemRepository.existsBySlug(candidate); suffix++) {
            candidate = "%s-%d".formatted(slug, suffix);
        }
        return candidate;
    }

    private static void addExample(Problem problem, String input, String output, String explanation) {
        ProblemExample example = new ProblemExample();
        example.setProblem(problem);
        example.setInput(input);
        example.setOutput(output);
        example.setExplanation(explanation);
        example.setDisplayOrder(problem.getExamples().size());
        problem.getExamples().add(example);
    }

    private static void addHint(Problem problem, String content) {
        ProblemHint hint = new ProblemHint();
        hint.setProblem(problem);
        hint.setContent(content);
        hint.setDisplayOrder(problem.getHints().size());
        problem.getHints().add(hint);
    }

    private static void addTestCase(Problem problem, String input, String expectedOutput, boolean hidden) {
        TestCase testCase = new TestCase();
        testCase.setProblem(problem);
        testCase.setInput(input);
        testCase.setExpectedOutput(expectedOutput);
        testCase.setHidden(hidden);
        testCase.setDisplayOrder(problem.getTestCases().size());
        problem.getTestCases().add(testCase);
    }

    private static <T> Map<Long, T> byId(List<T> values, Function<T, Long> idOf) {
        Map<Long, T> byId = new HashMap<>();
        for (T value : values) {
            Long id = idOf.apply(value);
            if (id != null) {
                byId.put(id, value);
            }
        }
        return byId;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private static <T> List<T> nullToEmpty(List<T> values) {
        return values == null ? List.of() : values;
    }

    /** A problem with the two things stored beside it that the form also edits. */
    public record Authored(Problem problem, Editorial editorial, Map<Language, String> starterCode) {}

    /** How many cases a problem has, split the way the catalogue row shows them. */
    public record TestCaseCounts(int total, int hidden) {

        public int samples() {
            return total - hidden;
        }
    }
}
