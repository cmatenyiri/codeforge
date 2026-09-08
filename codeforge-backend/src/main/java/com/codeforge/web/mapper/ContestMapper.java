package com.codeforge.web.mapper;

import com.codeforge.domain.Contest;
import com.codeforge.domain.ContestParticipation;
import com.codeforge.domain.ContestParticipationProblem;
import com.codeforge.domain.ContestProblem;
import com.codeforge.domain.ContestRatingChange;
import com.codeforge.domain.ContestStatus;
import com.codeforge.domain.Language;
import com.codeforge.domain.Problem;
import com.codeforge.domain.ProblemSnapshot;
import com.codeforge.domain.ProblemState;
import com.codeforge.service.ContestAuthoringService.Counts;
import com.codeforge.service.ContestService.OpenProblem;
import com.codeforge.web.dto.contest.AdminContestDetailResponse;
import com.codeforge.web.dto.contest.AdminContestProblemResponse;
import com.codeforge.web.dto.contest.AdminContestSummaryResponse;
import com.codeforge.web.dto.contest.ContestDetailResponse;
import com.codeforge.web.dto.contest.ContestProblemResponse;
import com.codeforge.web.dto.contest.ContestProblemResultResponse;
import com.codeforge.web.dto.contest.ContestProblemSummaryResponse;
import com.codeforge.web.dto.contest.ContestResultResponse;
import com.codeforge.web.dto.contest.ContestSummaryResponse;
import com.codeforge.web.dto.problem.ProblemExampleResponse;
import com.codeforge.web.dto.problem.TestCaseResponse;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Entity → DTO for the contest screens.
 *
 * <p>Hand-written rather than a MapStruct interface, like {@link InterviewMapper}
 * and for the same reason: almost nothing here is a field copy. The status is
 * derived from a clock, the countdowns from the same reading of it, and the most
 * important thing this class does is decide what <em>not</em> to send — a
 * scheduled contest's problem titles, above all.
 *
 * <h2>The blanking rule</h2>
 *
 * <p>Before a contest starts, its questions exist, are numbered and are worth
 * known points, and none of that gives anything away. Their titles do:
 * "Minimum Window Substring" above Q3 is most of the answer, and a client that
 * received it and merely declined to draw it would be one devtools tab away from
 * handing it over. So the titles are omitted here, on the server, and the
 * placeholder is the absence rather than a client-side conditional.
 */
@Component
public class ContestMapper {

    /**
     * One row in the contest list.
     *
     * @param now taken once by the caller, so a row that mentions the clock more
     *     than once cannot contradict itself
     */
    public ContestSummaryResponse toSummary(
            Contest contest,
            Instant now,
            boolean registered,
            long registrationCount,
            long participantCount,
            Optional<ContestParticipation> mine,
            Optional<ContestRatingChange> myRating,
            int problemCount) {

        return new ContestSummaryResponse(
                contest.getId(),
                contest.getSlug(),
                contest.getTitle(),
                contest.getType(),
                contest.status(now),
                contest.getStartsAt(),
                contest.getEndsAt(),
                contest.getDurationMinutes(),
                problemCount,
                contest.isRated(),
                contest.getUnratedReason(),
                registrationCount,
                participantCount,
                contest.secondsUntilStart(now),
                contest.isRunning(now) ? contest.remainingSeconds(now) : 0,
                registered,
                mine.map(ContestParticipation::getRank).orElse(null),
                mine.map(ContestParticipation::getScore).orElse(null),
                myRating.map(ContestRatingChange::getDelta).orElse(null));
    }

    /** A contest's own page. */
    public ContestDetailResponse toDetail(
            Contest contest,
            Instant now,
            boolean registered,
            long registrationCount,
            long participantCount,
            Optional<ContestParticipation> mine,
            Optional<ContestRatingChange> myRating,
            List<Integer> solveCounts) {

        boolean started = contest.hasStarted(now);

        List<ContestProblemSummaryResponse> problems = contest.getProblems().stream()
                .map(slot -> toProblemSummary(
                        slot,
                        started,
                        mine.flatMap(participation -> participation.problemAt(slot.getPosition())),
                        solveCountAt(solveCounts, slot.getPosition())))
                .toList();

        return new ContestDetailResponse(
                contest.getId(),
                contest.getSlug(),
                contest.getTitle(),
                contest.getDescription(),
                contest.getType(),
                contest.status(now),
                contest.getStartsAt(),
                contest.getEndsAt(),
                contest.getDurationMinutes(),
                contest.isRated(),
                contest.getUnratedReason(),
                registered,
                registrationCount,
                participantCount,
                contest.secondsUntilStart(now),
                contest.isRunning(now) ? contest.remainingSeconds(now) : 0,
                contest.totalPoints(),
                problems,
                mine.map(participation -> toResult(participation, myRating)).orElse(null));
    }

    /**
     * One question in the tab strip.
     *
     * <p>The title and slug are null until the contest starts — see the blanking
     * rule above. The points are not: knowing Q4 is worth six tells a competitor
     * how to plan their ninety minutes, which is part of the contest rather than
     * a leak of it.
     */
    public ContestProblemSummaryResponse toProblemSummary(
            ContestProblem slot,
            boolean started,
            Optional<ContestParticipationProblem> mine,
            Integer solveCount) {

        ProblemSnapshot snapshot = slot.getSnapshot();

        return new ContestProblemSummaryResponse(
                slot.getPosition(),
                slot.label(),
                started ? snapshot.title() : null,
                started ? snapshot.slug() : null,
                started ? snapshot.difficulty() : null,
                slot.getPoints(),
                mine.map(ContestParticipationProblem::isSolved).orElse(false),
                mine.map(ContestParticipationProblem::getAttempts).orElse(0),
                solveCount);
    }

    /**
     * A question as it appears in the arena.
     *
     * <p>Every field comes off the frozen snapshot rather than the catalogue,
     * which is the whole reason a contest can be judged fairly while its problems
     * are being edited.
     */
    public ContestProblemResponse toProblem(
            OpenProblem open, Map<Language, String> starterCode, Instant now) {

        ProblemSnapshot snapshot = open.snapshot();
        Contest contest = open.contest();

        return new ContestProblemResponse(
                open.contestProblem().getPosition(),
                open.contestProblem().label(),
                snapshot.slug(),
                snapshot.title(),
                snapshot.difficulty(),
                open.contestProblem().getPoints(),
                snapshot.description(),
                snapshot.constraintsMarkdown(),
                snapshot.examples().stream()
                        .map(example -> new ProblemExampleResponse(
                                example.input(), example.output(), example.explanation()))
                        .toList(),
                snapshot.sampleTestCases().stream()
                        .map(testCase ->
                                new TestCaseResponse(testCase.id(), testCase.input(), testCase.expectedOutput()))
                        .toList(),
                (int) snapshot.hiddenTestCaseCount(),
                starterCode,
                open.solved(),
                open.attempts(),
                open.wrongAttempts(),
                contest.isRunning(now),
                contest.isRunning(now) ? contest.remainingSeconds(now) : 0,
                open.lastSubmission().map(source -> source.sourceCode()).orElse(null),
                open.lastSubmission().map(source -> source.language()).orElse(null));
    }

    /** One row of the standings, or one line of a profile's contest history. */
    public ContestResultResponse toResult(
            ContestParticipation participation, Optional<ContestRatingChange> ratingChange) {

        return new ContestResultResponse(
                participation.getUser().getId(),
                participation.getUser().getUsername(),
                participation.getUser().getAvatar().name(),
                participation.getRank(),
                participation.getScore(),
                participation.getFinishSeconds(),
                participation.getPenaltySeconds(),
                participation.getTotalTimeSeconds(),
                participation.getSubmissionCount(),
                participation.getProblems().stream().map(this::toProblemResult).toList(),
                ratingChange.map(ContestRatingChange::getRatingBefore).orElse(null),
                ratingChange.map(ContestRatingChange::getRatingAfter).orElse(null),
                ratingChange.map(ContestRatingChange::getDelta).orElse(null));
    }

    public ContestProblemResultResponse toProblemResult(ContestParticipationProblem row) {
        return new ContestProblemResultResponse(
                row.getPosition(),
                "Q" + (row.getPosition() + 1),
                row.isSolved(),
                row.getSolvedAtSeconds(),
                row.getWrongAttempts(),
                row.getAttempts());
    }

    // ── Authoring ─────────────────────────────────────────────────────────

    public AdminContestSummaryResponse toAdminSummary(Contest contest, Instant now, Counts counts) {
        return new AdminContestSummaryResponse(
                contest.getId(),
                contest.getSlug(),
                contest.getTitle(),
                contest.getType(),
                contest.status(now),
                contest.getStartsAt(),
                contest.getDurationMinutes(),
                (int) counts.problems(),
                contest.isPublished(),
                contest.isRated(),
                contest.isSealed(),
                contest.getRatingsAppliedAt() != null,
                counts.registrations(),
                counts.participants(),
                contest.getRejudgeState(),
                contest.getUpdatedAt());
    }

    /**
     * A contest in full, for the authoring form.
     *
     * <p>Unlike every other contest response, the questions here are named
     * whatever the clock says: an author has to see what they are scheduling, and
     * they are the person who wrote it.
     */
    public AdminContestDetailResponse toAdminDetail(Contest contest, Instant now, Counts counts) {
        return new AdminContestDetailResponse(
                contest.getId(),
                contest.getSlug(),
                contest.getTitle(),
                contest.getDescription(),
                contest.getType(),
                contest.status(now),
                contest.getStartsAt(),
                contest.getEndsAt(),
                contest.getDurationMinutes(),
                contest.isPublished(),
                contest.isRated(),
                contest.getUnratedReason(),
                contest.isSealed(),
                contest.getSealedAt(),
                contest.getRatingsAppliedAt(),
                counts.registrations(),
                counts.participants(),
                contest.getProblems().stream().map(this::toAdminProblem).toList(),
                contest.getRejudgeState(),
                contest.getRejudgeStartedAt(),
                contest.getRejudgeFinishedAt(),
                contest.getRejudgeTotal(),
                contest.getRejudgeDone(),
                contest.getRejudgeError(),
                contest.getCreatedAt(),
                contest.getUpdatedAt());
    }

    /**
     * One question in the authoring form.
     *
     * <p>Reads the <em>live</em> problem rather than the snapshot, which is the
     * opposite of everywhere else and deliberately so: an author is editing what
     * the contest will ask, and needs to see the catalogue as it stands — whether
     * the problem is still a draft, whether it has cases yet — not the copy that
     * was frozen when they last saved.
     */
    public AdminContestProblemResponse toAdminProblem(ContestProblem slot) {
        Problem problem = slot.getProblem();

        return new AdminContestProblemResponse(
                slot.getPosition(),
                slot.label(),
                problem.getId(),
                problem.getSlug(),
                problem.getTitle(),
                problem.getDifficulty(),
                ProblemState.of(problem),
                slot.getPoints(),
                problem.getFunctionName() != null && problem.getReturnType() != null,
                problem.getTestCases().size());
    }

    /** True once a contest's places have stopped moving. */
    public static boolean isFinalised(Contest contest, Instant now) {
        return contest.status(now) == ContestStatus.FINALIZED
                || contest.status(now) == ContestStatus.ENDED;
    }

    private static Integer solveCountAt(List<Integer> counts, int position) {
        return position < counts.size() ? counts.get(position) : null;
    }
}
