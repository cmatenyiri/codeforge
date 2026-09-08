package com.codeforge.validation;

import com.codeforge.domain.Contest;
import com.codeforge.domain.Problem;
import com.codeforge.domain.Slugs;
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
 *   <li><b>Never, once it has started</b>: the questions and the clock. A sealed
 *       contest is being sat, or has been sat, against a fixed set of problems in
 *       a fixed window, and changing either would rewrite a round somebody has
 *       already competed in. This is the rule the whole snapshot mechanism exists
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
    private static final Set<String> RESERVED_SLUGS = Set.of("new", "upcoming", "running");

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

        boolean sealed = existing.map(Contest::isSealed).orElse(false);
        if (sealed) {
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

        if (existing.map(Contest::isSealed).orElse(false)) {
            List<Long> frozen = existing.orElseThrow().getProblems().stream()
                    .map(slot -> slot.getProblem().getId())
                    .toList();
            List<Long> sent = problems.stream().map(ContestProblemPayload::problemId).toList();

            errors.addIf(
                    !frozen.equals(sent),
                    "problems",
                    "validation.contest.problems.sealed",
                    "A contest that has started keeps the questions it ran with");
            // Points are still editable on a sealed contest, deliberately: they
            // rescale the standings rather than rewrite what anybody solved, and
            // an author who mis-weighted Q3 should be able to fix it and rejudge.
            validatePoints(problems, errors);
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
                // An unsolvable question is a wasted ninety minutes for the whole
                // field, and unlike a typo it cannot be fixed once the contest has
                // sealed. Checked at announcement rather than at save, so a draft
                // can be assembled before the problems behind it are finished.
                errors.addIf(
                        problem.getFunctionName() == null || problem.getReturnType() == null,
                        field,
                        "validation.contest.problem.noSignature",
                        "That problem has no solution signature yet");
                errors.addIf(
                        problem.getTestCases().isEmpty(),
                        field,
                        "validation.contest.problem.noTestCases",
                        "That problem has no test cases yet");
                errors.addIf(
                        problem.isArchived(),
                        field,
                        "validation.contest.problem.archived",
                        "That problem has been retired");
            }
        }

        errors.addIf(
                request.published() && problems.isEmpty(),
                "problems",
                "validation.contest.problems.required",
                "An announced contest needs at least one question");

        validatePoints(problems, errors);
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
