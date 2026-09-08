package com.codeforge.service;

import com.codeforge.domain.Contest;
import com.codeforge.domain.ContestParticipation;
import com.codeforge.domain.ContestParticipationProblem;
import com.codeforge.domain.ContestProblem;
import com.codeforge.domain.ContestRatingChange;
import com.codeforge.domain.ContestStatus;
import com.codeforge.domain.Language;
import com.codeforge.domain.Problem;
import com.codeforge.domain.ProblemSnapshot;
import com.codeforge.domain.Submission;
import com.codeforge.exception.BusinessRuleException;
import com.codeforge.exception.NotFoundException;
import com.codeforge.repository.ContestParticipationRepository;
import com.codeforge.repository.ContestProblemRepository;
import com.codeforge.repository.ContestRatingChangeRepository;
import com.codeforge.repository.ContestRepository;
import com.codeforge.repository.ProblemRepository;
import com.codeforge.repository.SubmissionRepository;
import com.codeforge.repository.UserRepository;
import com.codeforge.security.SecurityUtils;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * A contest as the people sitting it experience it: finding one, signing up,
 * reading the problems once the clock starts, and having attempts attributed.
 *
 * <p>Authoring lives in {@link ContestAuthoringService}, scoring in
 * {@link ContestStandingsService}, and judging in {@link ContestExecutionService}
 * — the last for the same reason {@link ExecutionService} is separate from
 * {@link SubmissionService}: a sandbox takes seconds and no database connection
 * may be held across it.
 *
 * <h2>The clock is the server's</h2>
 *
 * <p>Every method that reads or changes a contest re-derives the status from
 * {@code startsAt}, the duration and the current instant. A browser's countdown
 * is a rendering of a number this class hands out; it decides nothing. A tab
 * left open across a laptop suspend, a skewed system clock or a devtools console
 * cannot open a problem a second early or submit a second late.
 *
 * <h2>Sealing</h2>
 *
 * <p>Nothing has to run on a schedule for a contest to start. The first request
 * that touches a contest whose start time has passed is the one that freezes its
 * problems — see {@link #sealIfDue} — which is the same trick the mock interview
 * uses to end a round nobody came back to.
 */
@Service
@RequiredArgsConstructor
public class ContestService {

    /** How many upcoming contests the lobby shows. */
    private static final int UPCOMING_LIMIT = 5;

    private final ContestRepository contestRepository;
    private final ContestParticipationRepository participationRepository;
    private final ContestProblemRepository contestProblemRepository;
    private final ProblemRepository problemRepository;
    private final SubmissionRepository submissionRepository;
    private final UserRepository userRepository;
    private final ContestRatingChangeRepository ratingChangeRepository;
    private final ContestStandingsService standingsService;

    // ── Browsing ──────────────────────────────────────────────────────────

    /** Announced contests, newest first. Drafts are not in this query at all. */
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public Page<Contest> list(Pageable pageable) {
        return contestRepository.findPublished(pageable);
    }

    /** The next few, soonest first — the rail at the top of the lobby. */
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public List<Contest> upcoming() {
        return contestRepository.findUpcoming(
                Instant.now(), org.springframework.data.domain.PageRequest.of(0, UPCOMING_LIMIT));
    }

    /** Anything under way at this moment, so the lobby can lead with it. */
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public List<Contest> running() {
        return contestRepository.findRunning(Instant.now());
    }

    /**
     * One contest by its slug, sealed first if its start time has passed.
     *
     * <p>Not read-only, because sealing is a write. This is the call every
     * contest screen makes on load, and it is where the problems get frozen.
     */
    @Transactional
    @PreAuthorize("isAuthenticated()")
    public Contest getBySlug(String slug) {
        Contest contest = contestRepository
                .findWithProblemsBySlug(slug)
                .orElseThrow(() -> NotFoundException.of("contest", slug));

        // A draft is nobody's business but its author's, and a 404 rather than a
        // 403 so that an unannounced contest cannot be found by guessing a URL.
        if (!contest.isPublished() && !SecurityUtils.isAdmin()) {
            throw NotFoundException.of("contest", slug);
        }

        sealIfDue(contest.getId());

        // Re-read so the caller sees the snapshots the seal has just written,
        // rather than the copies this persistence context loaded before it.
        return contestRepository
                .findWithProblemsBySlug(slug)
                .orElseThrow(() -> NotFoundException.of("contest", slug));
    }

    /** Which of these contests the caller has signed up for. */
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public Set<Long> registeredContestIds(List<Long> contestIds) {
        if (contestIds.isEmpty()) {
            return Set.of();
        }
        return participationRepository.findEnteredContestIds(
                SecurityUtils.requireCurrentUserId(), contestIds);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public boolean isRegistered(Long contestId) {
        return participationRepository.existsByContestIdAndUserId(
                contestId, SecurityUtils.requireCurrentUserId());
    }

    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public long registrationCount(Long contestId) {
        return participationRepository.countByContestId(contestId);
    }

/**
     * Everything a list of contests needs beyond the contests themselves,
     * gathered in a fixed number of queries.
     *
     * <p>Five, whatever the page size: who the caller registered for, where they
     * placed, what it did to their rating, and the two crowd counts. Asking each
     * of those per row is how a list page that looks instant with three contests
     * takes a second with thirty.
     */
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public SummaryContext summaryContext(List<Contest> contests) {
        if (contests.isEmpty()) {
            return new SummaryContext(Set.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of());
        }

        Long userId = SecurityUtils.requireCurrentUserId();
        List<Long> ids = contests.stream().map(Contest::getId).toList();

        return new SummaryContext(
                participationRepository.findEnteredContestIds(userId, ids),
                participationRepository.findForUserAndContests(userId, ids).stream()
                        .collect(java.util.stream.Collectors.toMap(
                                participation -> participation.getContest().getId(), participation -> participation)),
                ratingChangeRepository.findForUserAndContests(userId, ids).stream()
                        .collect(java.util.stream.Collectors.toMap(
                                change -> change.getContest().getId(), change -> change)),
                participationRepository.countEnteredByContestIds(ids).stream()
                        .collect(java.util.stream.Collectors.toMap(
                                ContestParticipationRepository.ContestCount::getContestId,
                                ContestParticipationRepository.ContestCount::getTotal)),
                participationRepository.countCompetedByContestIds(ids).stream()
                        .collect(java.util.stream.Collectors.toMap(
                                ContestParticipationRepository.ContestCount::getContestId,
                                ContestParticipationRepository.ContestCount::getTotal)),
                contestProblemRepository.countByContestIds(ids).stream()
                        .collect(java.util.stream.Collectors.toMap(
                                ContestParticipationRepository.ContestCount::getContestId,
                                ContestParticipationRepository.ContestCount::getTotal)));
    }

/**
     * The rating changes for the people on one page of the standings, keyed by
     * user.
     *
     * <p>Empty until the contest has been settled, which is exactly when the
     * standings stop showing the column.
     */
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public Map<Long, ContestRatingChange> ratingChangesFor(Long contestId, List<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return ratingChangeRepository.findForContestAndUsers(contestId, userIds).stream()
                .collect(java.util.stream.Collectors.toMap(
                        change -> change.getUser().getId(), change -> change));
    }

    /** The caller's own line on one contest, and what it did to their rating. */
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public Optional<ContestRatingChange> myRatingChange(Long contestId) {
        return ratingChangeRepository.findByContestIdAndUserId(
                contestId, SecurityUtils.requireCurrentUserId());
    }

    /**
     * Per-contest extras for a list page.
     *
     * @param registered contest ids the caller has signed up for
     * @param mine their participation, present only where they actually submitted
     * @param ratings what each contest did to their rating, present only once the
     *     contest has been settled
     */
    public record SummaryContext(
            Set<Long> registered,
            Map<Long, ContestParticipation> mine,
            Map<Long, ContestRatingChange> ratings,
            Map<Long, Long> registrationCounts,
            Map<Long, Long> participantCounts,
            Map<Long, Long> problemCounts) {

        public boolean isRegistered(Long contestId) {
            return registered.contains(contestId);
        }

        public Optional<ContestParticipation> participation(Long contestId) {
            return Optional.ofNullable(mine.get(contestId));
        }

        public Optional<ContestRatingChange> rating(Long contestId) {
            return Optional.ofNullable(ratings.get(contestId));
        }

        public long registrations(Long contestId) {
            return registrationCounts.getOrDefault(contestId, 0L);
        }

        public long participants(Long contestId) {
            return participantCounts.getOrDefault(contestId, 0L);
        }

        public int problems(Long contestId) {
            return problemCounts.getOrDefault(contestId, 0L).intValue();
        }
    }

    // ── Registration ──────────────────────────────────────────────────────

    /**
     * Signs the caller up.
     *
     * <p>Allowed right up until the contest ends, not only before it starts.
     * Turning somebody away at minute three would be enforcing a deadline that
     * protects nothing: they have already lost the three minutes, and the score
     * is what it is. Registering does not by itself put anyone on the standings —
     * that takes a submission.
     */
    @Transactional
    @PreAuthorize("isAuthenticated()")
    public void register(String slug) {
        Contest contest = requirePublished(slug);
        Long userId = SecurityUtils.requireCurrentUserId();

        if (contest.hasEnded(Instant.now())) {
            throw new BusinessRuleException("error.contest.ended", "That contest is over");
        }
        if (participationRepository.existsByContestIdAndUserId(contest.getId(), userId)) {
            return;
        }

        // The row that will carry their result, opened empty. Registering is the
        // act that puts somebody in a contest, so it is the act that creates the
        // record — see ContestParticipation for why there is only one.
        ContestParticipation entry = new ContestParticipation();
        entry.setContest(contest);
        entry.setUser(userRepository.getReferenceById(userId));
        entry.setRegisteredAt(Instant.now());
        participationRepository.save(entry);
    }

    /**
     * Withdraws a registration.
     *
     * <p>Refused once the contest is under way. Before the start it is a change
     * of plan; afterwards it would be a way to leave a contest you were doing
     * badly in, and a rating that can be opted out of after seeing the result is
     * not a rating.
     */
    @Transactional
    @PreAuthorize("isAuthenticated()")
    public void unregister(String slug) {
        Contest contest = requirePublished(slug);

        if (contest.hasStarted(Instant.now())) {
            throw new BusinessRuleException(
                    "error.contest.started", "That contest has already started");
        }
        participationRepository.deleteByContestIdAndUserId(
                contest.getId(), SecurityUtils.requireCurrentUserId());
    }

    // ── Inside the contest ────────────────────────────────────────────────

    /**
     * One question of a contest, from the frozen copy.
     *
     * <p>Unreadable until the contest starts, which is the whole point of a
     * synchronised round: everybody gets the problems at the same instant, and
     * there is no request that returns one early.
     *
     * <p>Readable for good afterwards, though. A finished contest's problems are
     * the most useful thing it leaves behind, and practising them is what most
     * people do with a round they could not sit — see
     * {@link ContestExecutionService} for why those attempts never touch the
     * standings.
     */
    @Transactional
    @PreAuthorize("isAuthenticated()")
    public OpenProblem openProblem(String slug, int position) {
        Contest contest = getBySlug(slug);
        Instant now = Instant.now();

        requireStarted(contest, now);
        requireEntitled(contest, now);

        ContestProblem contestProblem = problemAt(contest, position);
        Optional<ContestParticipation> participation =
                participationRepository.findWithProblems(contest.getId(), SecurityUtils.requireCurrentUserId());
        Optional<ContestParticipationProblem> row =
                participation.flatMap(entry -> entry.problemAt(position));

        return new OpenProblem(
                contest,
                contestProblem,
                contestProblem.getSnapshot(),
                row.map(ContestParticipationProblem::isSolved).orElse(false),
                row.map(ContestParticipationProblem::getAttempts).orElse(0),
                row.map(ContestParticipationProblem::getWrongAttempts).orElse(0),
                lastSubmissionSource(contestProblem.getId()));
    }

    /**
     * The frozen problem to judge against, having checked the contest may still
     * be submitted to.
     *
     * <p>Its own short transaction, because the judging that follows must hold no
     * connection at all — which is also why the snapshot is handed out whole
     * rather than re-read afterwards: by the time the sandbox returns, this
     * transaction is long closed.
     */
    @Transactional
    @PreAuthorize("isAuthenticated()")
    public JudgingTarget requireJudgingTarget(String slug, int position) {
        Contest contest = getBySlug(slug);
        Instant now = Instant.now();

        requireStarted(contest, now);
        requireEntitled(contest, now);
        ContestProblem contestProblem = problemAt(contest, position);

        return new JudgingTarget(
                contest.getId(),
                contestProblem.getId(),
                contestProblem.getSnapshot(),
                // Decided here, once, against the same clock reading that let the
                // request through — so a submission cannot be counted by one
                // check and rejected by another a millisecond later.
                contest.isRunning(now),
                contest.secondsIntoContest(now));
    }


    /**
     * Attributes a judged submission to the contest, and to the scoreboard when
     * it counted.
     *
     * <p>Both writes in one transaction, because a submission stamped as counted
     * whose score never landed — or the reverse — would leave the standings and
     * the submissions disagreeing about what happened, and the rebuild a rejudge
     * performs reads the stamps.
     *
     * <p>Whether it counted was decided in {@link #requireJudgingTarget}, before
     * the sandbox ran, against the clock reading that admitted the request. A
     * submission legally started with ten seconds left is judged for a minute
     * afterwards and still counts — the alternative punishes a competitor for the
     * judge being slow, which is not something they can do anything about.
     */
    @Transactional
    @PreAuthorize("isAuthenticated()")
    public void recordAttempt(JudgingTarget target, Long submissionId, boolean accepted) {
        Contest contest = contestRepository
                .findWithProblems(target.contestId())
                .orElseThrow(() -> NotFoundException.of("contest", target.contestId()));
        ContestProblem contestProblem = contest.getProblems().stream()
                .filter(problem -> problem.getId().equals(target.contestProblemId()))
                .findFirst()
                .orElseThrow(() -> NotFoundException.of("contestProblem", target.contestProblemId()));

        Submission submission = submissionRepository.getReferenceById(submissionId);
        submission.setContest(contest);
        submission.setContestProblem(contestProblem);
        submission.setCountedInContest(target.counted());
        // Stamped from the same clock reading that decided whether it counted,
        // taken before the judge ran — see Submission#getContestSeconds.
        submission.setContestSeconds(target.secondsIntoContest());

        if (target.counted()) {
            standingsService.recordSubmission(
                    contest,
                    contestProblem,
                    SecurityUtils.requireCurrentUserId(),
                    submissionId,
                    accepted,
                    target.secondsIntoContest());
        }
    }

    // ── Sealing ───────────────────────────────────────────────────────────

    /**
     * Freezes a contest's problems, if its start time has passed and it has not
     * been frozen already.
     *
     * <p>Its own transaction, and a new one even when called from inside another,
     * so that the row lock is taken and released around this and nothing else.
     * The lock is what makes the first-request-wins race safe when a thousand
     * people load the contest in the same second — see
     * {@link ContestRepository#findForSealing}.
     *
     * <p>The snapshots are re-taken here rather than trusted from whenever the
     * contest was last saved, so that an author fixing a typo at 09:55 has fixed
     * it for the contest that starts at 10:00. From this instant they are never
     * written again except by a rejudge.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sealIfDue(Long contestId) {
        Contest contest = contestRepository.findForSealing(contestId).orElse(null);
        if (contest == null || contest.isSealed() || !contest.isPublished()) {
            return;
        }
        Instant now = Instant.now();
        if (!contest.hasStarted(now)) {
            return;
        }

        snapshotProblems(contest);
        contest.setSealedAt(now);
    }

    /**
     * Re-takes every snapshot from the live catalogue.
     *
     * <p>Shared with authoring, which calls it whenever a contest's questions
     * change while it is still a draft, and with the rejudge, for which replacing
     * the frozen cases is the entire operation.
     */
    @Transactional
    public void snapshotProblems(Contest contest) {
        for (ContestProblem contestProblem : contest.getProblems()) {
            contestProblem.setSnapshot(snapshotOf(contestProblem.getProblem().getId()));
        }
    }

    /**
     * Reads a problem in full so it can be frozen.
     *
     * <p>Every collection is pulled in separately rather than as one fetch join —
     * three List associations in a single join is Hibernate's
     * MultipleBagFetchException, the same reason the authoring screen loads them
     * one at a time.
     */
    private ProblemSnapshot snapshotOf(Long problemId) {
        Problem problem = problemRepository
                .findById(problemId)
                .orElseThrow(() -> NotFoundException.of("problem", String.valueOf(problemId)));

        problem.getParameters().size();
        problem.getExamples().size();
        problem.getHints().size();
        problem.getTestCases().size();

        return ProblemSnapshot.of(problem);
    }

    // ── Internals ─────────────────────────────────────────────────────────

    private Contest requirePublished(String slug) {
        Contest contest = contestRepository
                .findBySlug(slug)
                .orElseThrow(() -> NotFoundException.of("contest", slug));

        if (!contest.isPublished()) {
            throw NotFoundException.of("contest", slug);
        }
        return contest;
    }

    /**
     * Refuses a live contest's problems to anybody who has not entered it.
     *
     * <p>The gate that makes registration mean something. Reading the problems
     * <em>is</em> competing — knowing what is being asked is the advantage — so
     * letting an unregistered visitor browse a running contest would leave the
     * decision to compete until after they had seen whether it looked easy.
     * Registering first is what turns it into a commitment.
     *
     * <p>It lifts the moment the contest ends: practising a finished round is
     * the most useful thing it leaves behind, and there is nothing left to
     * protect. Authors are exempt throughout, because they wrote the questions
     * and have to be able to check a live contest is rendering.
     */
    private void requireEntitled(Contest contest, Instant now) {
        if (contest.hasEnded(now) || SecurityUtils.isAdmin()) {
            return;
        }
        if (!participationRepository.existsByContestIdAndUserId(
                contest.getId(), SecurityUtils.requireCurrentUserId())) {
            throw new BusinessRuleException(
                    "error.contest.notRegistered", "Register for this contest to open its problems");
        }
    }

    private static void requireStarted(Contest contest, Instant now) {
        if (!contest.hasStarted(now)) {
            throw new BusinessRuleException(
                    "error.contest.notStarted", "That contest has not started yet");
        }
        if (contest.status(now) == ContestStatus.DRAFT) {
            throw new BusinessRuleException("error.contest.draft", "That contest is not announced yet");
        }
    }

    private static ContestProblem problemAt(Contest contest, int position) {
        return contest.problemAt(position)
                .orElseThrow(() -> NotFoundException.of("contestProblem", position));
    }

    /**
     * The caller's most recent attempt at this question, so reopening the tab
     * restores the code the judge last saw.
     *
     * <p>A local draft would be lost by a reload, a different browser or cleared
     * site data, and a contest is exactly the wrong ninety minutes to discover
     * that.
     */
    private Optional<Source> lastSubmissionSource(Long contestProblemId) {
        return submissionRepository
                .findForUserAndContestProblem(
                        SecurityUtils.requireCurrentUserId(),
                        contestProblemId,
                        org.springframework.data.domain.PageRequest.of(0, 1))
                .getContent()
                .stream()
                .findFirst()
                .map(submission -> new Source(submission.getSourceCode(), submission.getLanguage()));
    }

    /** A question put on screen, with what the caller has already done to it. */
    public record OpenProblem(
            Contest contest,
            ContestProblem contestProblem,
            ProblemSnapshot snapshot,
            boolean solved,
            int attempts,
            int wrongAttempts,
            Optional<Source> lastSubmission) {}

    /** The code the judge last saw for a question, and the language it was in. */
    public record Source(String sourceCode, Language language) {}

    /**
     * Everything the judge needs, decided in one transaction against one reading
     * of the clock.
     *
     * @param counted whether this attempt scores. False once the contest is over,
     *     which is how practising a finished round stays possible without it
     *     rewriting standings that have already been rated
     * @param secondsIntoContest when the attempt was made, for the finish time
     */
    public record JudgingTarget(
            Long contestId,
            Long contestProblemId,
            ProblemSnapshot snapshot,
            boolean counted,
            long secondsIntoContest) {}
}
