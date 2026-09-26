package com.codeforge.validation;

import com.codeforge.domain.Contest;
import com.codeforge.domain.ContestProblem;
import com.codeforge.domain.Problem;
import com.codeforge.domain.Slugs;
import com.codeforge.domain.TestCase;
import com.codeforge.repository.ContestRepository;
import com.codeforge.repository.ProblemRepository;
import com.codeforge.web.dto.contest.ContestProblemPayload;
import com.codeforge.web.dto.contest.ContestUpsertRequest;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Validates an authored contest.
 *
 * <p>Three kinds of rule, and the difference between them is the design of the
 * authoring flow:
 *
 * <ul>
 *   <li><b>Always</b>: shape and uniqueness. A title fits its column, a slug is a
 *       slug, a duration is a plausible number of minutes, the same problem is
 *       not asked twice.
 *   <li><b>Only when announcing</b>: completeness. Questions, each of them
 *       actually solvable, and a start time that has not already gone by. A draft
 *       is allowed to be half-written — that is what a draft is for.
 *   <li><b>Never, once announced</b>: the slug. It is the contest's address, and
 *       from the announcement on it has been handed out — see
 *       {@link #validateSlug}.
 *   <li><b>Never, once it has started</b>: the questions, their points and the
 *       clock. A started contest is being sat, or has been sat, against a fixed
 *       set of problems worth fixed points in a fixed window, and changing any of
 *       them would rewrite a round somebody has already competed in. "Started"
 *       is the clock's call — see {@link Contest#isLocked} — not whether anybody
 *       has opened it yet. This is the rule the whole snapshot mechanism exists
 *       to enforce, and refusing the edit outright is the honest version of it —
 *       silently ignoring the change would leave the author believing it landed.
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class ContestUpsertRequestValidator {

    /** Long enough for anything reasonable, short enough that a typo cannot book a month. */
    private static final int MAX_DURATION_MINUTES = 24 * 60;

    /** Below this a contest is not a contest, it is a race condition. */
    private static final int MIN_DURATION_MINUTES = 5;

    /** A contest may not take a slug the contest routes already mean something by. */
    private static final Set<String> RESERVED_SLUGS = Set.of("new", "upcoming", "running", "past");

    private final ContestRepository contestRepository;
    private final ProblemRepository problemRepository;

    /**
     * @param existing the contest being edited, or empty when creating one —
     *     uniqueness has to ignore the row it is checking on behalf of, and the
     *     sealed rules have nothing to protect on a contest that does not exist
     */
    public void validate(ContestUpsertRequest request, Optional<Contest> existing) {
        ValidationErrors errors = new ValidationErrors();

        validateTitle(request.title(), existing, errors);
        validateSlug(request, existing, errors);
        validateSchedule(request, existing, errors);
        validateProblems(request, existing, errors);

        errors.throwIfAny();
    }

    private void validateTitle(String title, Optional<Contest> existing, ValidationErrors errors) {
        String value = ValidationRules.trimToNull(title);

        if (value == null) {
            errors.add("title", "validation.contest.title.required", "A title is required");
            return;
        }
        if (value.length() > ValidationRules.PROBLEM_TITLE_MAX_LENGTH) {
            errors.add("title", "validation.contest.title.length", "Title is too long");
            return;
        }

        boolean taken = existing
                .map(contest -> contestRepository.existsByTitleIgnoreCaseAndIdNot(value, contest.getId()))
                .orElseGet(() -> contestRepository.existsByTitleIgnoreCase(value));
        errors.addIf(taken, "title", "validation.contest.title.taken", "Another contest already has that title");
    }

    private void validateSlug(
            ContestUpsertRequest request, Optional<Contest> existing, ValidationErrors errors) {

        // Fixed from the announcement on. The address is what has been handed
        // out by then — in the announcement, in registrants' bookmarks, and in
        // every open tab of a live contest, whose requests all name it — and a
        // rename would break each of them, with nothing to redirect the old one.
        // Blank is not a rename: it keeps the slug the contest already has,
        // rather than deriving a new one from a title that may have changed.
        Optional<Contest> announced = existing.filter(Contest::isPublished);
        if (announced.isPresent()) {
            String sent = ValidationRules.trimToNull(request.slug());
            errors.addIf(
                    sent != null && !sent.equals(announced.get().getSlug()),
                    "slug",
                    "validation.contest.slug.announced",
                    "The URL is fixed once the contest is announced");
            return;
        }

        // Blank derives one from the title, which is what an author wants right
        // up until a rename would break the links they have already shared.
        String value = ValidationRules.trimToNull(request.slug());
        if (value == null) {
            value = Slugs.slugify(ValidationRules.trimToNull(request.title()) == null ? "" : request.title());
        }

        if (value.isBlank()) {
            errors.add("slug", "validation.contest.slug.required", "A URL slug is required");
            return;
        }
        if (value.length() > ValidationRules.PROBLEM_SLUG_MAX_LENGTH) {
            errors.add("slug", "validation.contest.slug.length", "Slug is too long");
            return;
        }
        if (!ValidationRules.SLUG_PATTERN.matcher(value).matches()) {
            errors.add("slug", "validation.contest.slug.format", "Use lowercase letters, digits and single hyphens");
            return;
        }
        if (RESERVED_SLUGS.contains(value)) {
            errors.add("slug", "validation.contest.slug.reserved", "That slug is reserved");
            return;
        }

        String candidate = value;
        boolean taken = existing
                .map(contest -> contestRepository.existsBySlugAndIdNot(candidate, contest.getId()))
                .orElseGet(() -> contestRepository.existsBySlug(candidate));
        errors.addIf(taken, "slug", "validation.contest.slug.taken", "Another contest already uses that slug");
    }

    private void validateSchedule(
            ContestUpsertRequest request, Optional<Contest> existing, ValidationErrors errors) {

        if (request.startsAt() == null) {
            errors.add("startsAt", "validation.contest.startsAt.required", "A start time is required");
        }
        if (request.durationMinutes() < MIN_DURATION_MINUTES
                || request.durationMinutes() > MAX_DURATION_MINUTES) {
            errors.add(
                    "durationMinutes",
                    "validation.contest.duration.range",
                    "A contest runs between " + MIN_DURATION_MINUTES + " and " + MAX_DURATION_MINUTES + " minutes");
        }

        boolean locked = existing.map(contest -> contest.isLocked(Instant.now())).orElse(false);
        if (locked) {
            Contest contest = existing.orElseThrow();
            // The clock a field competed against is part of the result. Moving
            // either end of it after the fact would re-score finish times that
            // have already been ranked, and possibly rated.
            errors.addIf(
                    !contest.getStartsAt().equals(request.startsAt()),
                    "startsAt",
                    "validation.contest.startsAt.sealed",
                    "A contest that has started keeps the start time it ran with");
            errors.addIf(
                    contest.getDurationMinutes() != request.durationMinutes(),
                    "durationMinutes",
                    "validation.contest.duration.sealed",
                    "A contest that has started keeps the duration it ran with");
            return;
        }

        // Announcing a contest into the past would seal it the instant it saved,
        // freezing whatever happened to be written at that moment.
        if (request.published() && request.startsAt() != null && request.startsAt().isBefore(Instant.now())) {
            errors.add(
                    "startsAt",
                    "validation.contest.startsAt.past",
                    "An announced contest has to start in the future");
        }
    }

    private void validateProblems(
            ContestUpsertRequest request, Optional<Contest> existing, ValidationErrors errors) {

        List<ContestProblemPayload> problems = request.problems() == null ? List.of() : request.problems();

        if (existing.map(contest -> contest.isLocked(Instant.now())).orElse(false)) {
            List<Long> frozen = existing.orElseThrow().getProblems().stream()
                    .map(slot -> slot.getProblem().getId())
                    .toList();
            List<Long> sent = problems.stream().map(ContestProblemPayload::problemId).toList();

            if (!frozen.equals(sent)) {
                errors.add(
                        "problems",
                        "validation.contest.problems.sealed",
                        "A contest that has started keeps the questions it ran with");
                return;
            }
            // The points too. Everybody competed for the points that were
            // announced, and re-weighting a question afterwards would rank the
            // field by rules nobody played under. A question that turns out to
            // have been worth the wrong amount is a reason to make the contest
            // unrated, not to change the scoring after the fact. A point value the
            // request leaves out is left as it is.
            List<ContestProblem> slots = existing.orElseThrow().getProblems();
            boolean repointed = false;
            for (int position = 0; position < problems.size(); position++) {
                Integer points = problems.get(position).points();
                repointed |= points != null && points != slots.get(position).getPoints();
            }
            errors.addIf(
                    repointed,
                    "problems",
                    "validation.contest.problems.pointsSealed",
                    "A contest that has started keeps the points it ran with");
            return;
        }

        Set<Long> seen = new HashSet<>();
        for (int index = 0; index < problems.size(); index++) {
            ContestProblemPayload payload = problems.get(index);
            String field = "problems[" + index + "].problemId";

            if (payload.problemId() == null) {
                errors.add(field, "validation.contest.problem.required", "Choose a problem");
                continue;
            }
            if (!seen.add(payload.problemId())) {
                errors.add(field, "validation.contest.problem.duplicate", "That problem is already in this contest");
                continue;
            }

            Problem problem = problemRepository.findById(payload.problemId()).orElse(null);
            if (problem == null) {
                errors.add(field, "validation.contest.problem.unknown", "That problem no longer exists");
                continue;
            }
            if (request.published()) {
                validateAnnounceable(problem, existing, field, errors);
            }
        }

        errors.addIf(
                request.published() && problems.isEmpty(),
                "problems",
                "validation.contest.problems.required",
                "An announced contest needs at least one question");

        validatePoints(problems, errors);
    }

    /**
     * What announcing asks of each question.
     *
     * <p>Checked at announcement rather than at save, so a draft contest can be
     * assembled before the problems behind it are finished. From announcement
     * on, the contest holds each problem out of the catalogue and publishes it
     * itself when it ends, which sets the two halves of this:
     *
     * <ul>
     *   <li>The problem has to be new: never public, not merely private now. One
     *       in the catalogue can be read — editorial and all — by anybody before
     *       the contest starts; one that has been in it and was taken back out
     *       has been read already, and whoever solved it still has their code.
     *       And one another contest holds would be published at the end of
     *       whichever round finished first.
     *   <li>It has to be exactly what publishing it by hand would demand — the
     *       same four rules {@link ProblemUpsertRequestValidator} applies —
     *       because nobody will be watching when it is. An unsolvable question
     *       is also a wasted ninety minutes for the whole field, and unlike a
     *       typo it cannot be fixed once the contest has sealed.
     * </ul>
     */
    private void validateAnnounceable(
            Problem problem, Optional<Contest> existing, String field, ValidationErrors errors) {

        if (problem.isArchived()) {
            errors.add(field, "validation.contest.problem.archived", "That problem has been retired");
            return;
        }
        if (problem.isPublished()) {
            errors.add(
                    field,
                    "validation.contest.problem.public",
                    "That problem is already public, so anyone could read it before the contest");
        } else {
            errors.addIf(
                    problem.getFirstPublishedAt() != null,
                    field,
                    "validation.contest.problem.previouslyPublic",
                    "That problem has been public before, so it is not new to everyone");
        }
        errors.addIf(
                contestRepository.isHoldingProblemOutside(problem.getId(), existing.map(Contest::getId).orElse(null)),
                field,
                "validation.contest.problem.held",
                "Another announced contest is already using that problem");

        errors.addIf(
                problem.getFunctionName() == null || problem.getReturnType() == null,
                field,
                "validation.contest.problem.noSignature",
                "That problem has no solution signature yet");
        errors.addIf(
                problem.getExamples().isEmpty(),
                field,
                "validation.contest.problem.noExamples",
                "That problem has no example yet");
        if (problem.getTestCases().isEmpty()) {
            errors.add(field, "validation.contest.problem.noTestCases", "That problem has no test cases yet");
        } else {
            errors.addIf(
                    problem.getTestCases().stream().allMatch(TestCase::isHidden),
                    field,
                    "validation.contest.problem.noSamples",
                    "That problem has no sample case to run against");
        }
    }

    private static void validatePoints(List<ContestProblemPayload> problems, ValidationErrors errors) {
        for (int index = 0; index < problems.size(); index++) {
            Integer points = problems.get(index).points();
            errors.addIf(
                    points != null && (points < 1 || points > 100),
                    "problems[" + index + "].points",
                    "validation.contest.problem.points",
                    "A question is worth between 1 and 100 points");
        }
    }
}
