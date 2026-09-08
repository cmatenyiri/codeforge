package com.codeforge.service;

import com.codeforge.domain.Difficulty;
import com.codeforge.domain.Interview;
import com.codeforge.domain.InterviewFormat;
import com.codeforge.domain.InterviewInsight;
import com.codeforge.domain.InterviewOutcome;
import com.codeforge.domain.InterviewProblem;
import com.codeforge.domain.InterviewStatus;
import com.codeforge.domain.Language;
import com.codeforge.domain.Problem;
import com.codeforge.domain.ProblemSnapshot;
import com.codeforge.domain.Submission;
import com.codeforge.exception.BusinessRuleException;
import com.codeforge.exception.NotFoundException;
import com.codeforge.repository.InterviewRepository;
import com.codeforge.repository.ProblemRepository.InterviewCandidate;
import com.codeforge.repository.ProblemRepository;
import com.codeforge.repository.SubmissionRepository;
import com.codeforge.repository.UserRepository;
import com.codeforge.security.SecurityUtils;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The mock interview's state machine: assembling a set, running the clock, and
 * turning the result into a debrief.
 *
 * <p>Judging code is deliberately not here — see {@link InterviewExecutionService},
 * which composes this with {@link ExecutionService} for the same reason
 * {@link ExecutionService} is separate from {@link SubmissionService}: a sandbox
 * takes seconds, and no database connection may be held across it.
 *
 * <h2>The clock</h2>
 *
 * <p>Every method that changes a running interview re-derives the remaining time
 * from {@code startedAt} and the stored duration. The browser's countdown is a
 * rendering of that number and has no authority over it, so a suspended laptop,
 * a skewed system clock or a devtools console cannot buy time. An interview
 * found past its deadline is closed on the spot, at the deadline rather than at
 * the moment of discovery — otherwise a candidate who closed the tab at minute
 * ten would come back to a round that had "run" for a week.
 */
@Service
@RequiredArgsConstructor
public class InterviewService {

    /**
     * How long a problem stays out of the pool after being drawn.
     *
     * <p>The set is sampled from the whole catalogue rather than from a separate
     * interview-only one — a hidden pool would cost twice the authoring, could
     * never be reviewed afterwards, and would still lose to a second browser tab.
     * This, plus the freshness tiers in the sampling query, is what buys the
     * "I have not seen this" feeling honestly.
     */
    private static final int RECENT_PROBLEM_COOLDOWN_DAYS = 14;

    /** SQL has no empty {@code in} list, so exclusion sets always carry this. */
    private static final long NO_SUCH_PROBLEM_ID = -1L;

    /** Share of the budget a full solve has to come in under to read as unhurried. */
    private static final int COMFORTABLE_TIME_PERCENT = 75;

    /** Below this, finishing everything is worth remarking on. */
    private static final int EARLY_FINISH_PERCENT = 60;

    /** A warm-up eating more of the round than this is the classic failure. */
    private static final int WARM_UP_BUDGET_PERCENT = 40;

    /** Submissions per solve above which the judge is being used as a compiler. */
    private static final int MANY_ATTEMPTS_PER_SOLVE = 3;

    /** A debrief nobody reads is not a debrief. */
    private static final int MAX_INSIGHTS = 4;

    private final InterviewRepository interviewRepository;
    private final ProblemRepository problemRepository;
    private final SubmissionRepository submissionRepository;
    private final UserRepository userRepository;

    // ── Lifecycle ─────────────────────────────────────────────────────────

    /**
     * Assembles and starts a round.
     *
     * <p>One at a time per user: a second concurrent interview would let someone
     * hold a problem open in one tab while the clock on another runs down, and
     * there is no reading of "in progress" that survives two of them.
     */
    @Transactional
    @PreAuthorize("isAuthenticated()")
    public Interview start(InterviewFormat format) {
        Long userId = SecurityUtils.requireCurrentUserId();
        Instant now = Instant.now();

        closeExpired(userId, now);
        if (interviewRepository.existsByUserIdAndStatus(userId, InterviewStatus.IN_PROGRESS)) {
            throw new BusinessRuleException(
                    "error.interview.alreadyRunning", "An interview is already in progress");
        }

        Interview interview = new Interview();
        interview.setUser(userRepository.getReferenceById(userId));
        interview.setFormat(format);
        // Copied, not referenced: a later change to the format must not rewrite
        // the length of a round somebody has already sat.
        interview.setDurationMinutes(format.durationMinutes());
        interview.setStartedAt(now);

        Set<Long> onCooldown = interviewRepository.findProblemIdsUsedSince(
                userId, now.minus(Duration.ofDays(RECENT_PROBLEM_COOLDOWN_DAYS)));
        Set<Long> chosen = new LinkedHashSet<>();

        List<Difficulty> slots = format.slots();
        for (int position = 0; position < slots.size(); position++) {
            Long problemId = draw(slots.get(position), userId, chosen, onCooldown);
            chosen.add(problemId);

            InterviewProblem slot = new InterviewProblem();
            slot.setInterview(interview);
            slot.setProblem(problemRepository.getReferenceById(problemId));
            slot.setPosition(position);
            // Taken here, once, and never refreshed: everything the round shows
            // and everything it judges is decided at this moment, so an author
            // editing the problem later cannot reach a candidate who is already
            // sitting it. Same rule as the duration copied above.
            slot.setSnapshot(snapshot(problemId));
            interview.getProblems().add(slot);
        }

        return interviewRepository.save(interview);
    }

    /**
     * The caller's running interview.
     *
     * <p>Not read-only: this is the call the lobby makes on load, and it is where
     * a round abandoned to a closed tab gets closed out.
     */
    @Transactional
    @PreAuthorize("isAuthenticated()")
    public Interview current() {
        Long userId = SecurityUtils.requireCurrentUserId();
        closeExpired(userId, Instant.now());

        return interviewRepository
                .findActive(userId)
                .orElseThrow(() -> NotFoundException.of("interview", "current"));
    }

    /**
     * One of the caller's interviews, closed out first if its time has run.
     *
     * <p>This is what makes the buzzer real without a scheduler: the next read of
     * an expired round — a poll from the session screen, a visit to the report —
     * is what ends it.
     */
    @Transactional
    @PreAuthorize("isAuthenticated()")
    public Interview load(Long id) {
        Interview interview = owned(id);
        if (interview.isActive() && interview.isExpired(Instant.now())) {
            close(interview, interview.deadline());
        }
        return interview;
    }

    /** The caller's past rounds, newest first. */
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public Page<Interview> history(Pageable pageable) {
        return interviewRepository.findByUserIdOrderByStartedAtDesc(
                SecurityUtils.requireCurrentUserId(), pageable);
    }

    /**
     * Ends a round the candidate is done with.
     *
     * <p>Idempotent: finishing an interview the buzzer already closed is not an
     * error, because the two race every time a candidate hits the button as the
     * clock hits zero.
     */
    @Transactional
    @PreAuthorize("isAuthenticated()")
    public Interview finish(Long id) {
        Interview interview = owned(id);
        if (interview.getStatus() == InterviewStatus.ABANDONED) {
            throw new BusinessRuleException("error.interview.abandoned", "That interview was abandoned");
        }
        if (interview.isActive()) {
            Instant now = Instant.now();
            close(interview, interview.isExpired(now) ? interview.deadline() : now);
        }
        return interview;
    }

    /**
     * Throws the round away.
     *
     * <p>Distinct from finishing on purpose. A round quit two minutes in is not a
     * failed round, and scoring it as one would make the history unreadable —
     * which is the same reason it gets no outcome band.
     */
    @Transactional
    @PreAuthorize("isAuthenticated()")
    public Interview abandon(Long id) {
        Interview interview = owned(id);
        if (!interview.isActive()) {
            throw new BusinessRuleException("error.interview.notRunning", "That interview is not running");
        }

        interview.setStatus(InterviewStatus.ABANDONED);
        interview.setEndedAt(Instant.now());
        interview.setScore((int) interview.solvedCount());

        return interview;
    }

    /**
     * Records the candidate's own answer to "did you look anything up?".
     *
     * <p>Accepted after the fact and at any time, including a correction days
     * later. It adjusts no score and gates nothing — the point is a history its
     * owner can still trust, not enforcement, which a self-guided mock could not
     * do anyway.
     */
    @Transactional
    @PreAuthorize("isAuthenticated()")
    public Interview recordSelfReport(Long id, Boolean usedOutsideHelp) {
        Interview interview = owned(id);
        interview.setUsedOutsideHelp(usedOutsideHelp);
        return interview;
    }

    // ── During the round ──────────────────────────────────────────────────

    /**
     * Puts one problem of the set on screen.
     *
     * <p>A read that writes, deliberately: the first open is when the clock on
     * that problem starts, and asking the client to announce it separately would
     * be one more round trip and one more thing to get wrong. It is stamped once
     * — coming back to a problem must not reset what it has already cost.
     */
    @Transactional
    @PreAuthorize("isAuthenticated()")
    public OpenSlot openSlot(Long id, int position) {
        Interview interview = load(id);
        InterviewProblem slot = slotAt(interview, position);

        requireReachable(interview, position);

        if (interview.isActive() && slot.getOpenedAt() == null) {
            slot.setOpenedAt(Instant.now());
        }

        boolean editable = interview.isActive() && !slot.isResolved();

        // Loaded only for a closed problem, and only when it is opened — never on
        // the session poll, which reads this same interview every few seconds and
        // has no use for a source blob.
        Submission judged = editable ? null : slot.displayedSubmission();

        return new OpenSlot(
                interview.getStatus(),
                slot.getSnapshot(),
                position,
                slot.isWarmUp(),
                slot.isSolved(),
                slot.isSkipped(),
                slot.getAttempts(),
                slot.getHintsRevealed(),
                editable,
                judged == null ? null : judged.getSourceCode(),
                judged == null ? null : judged.getLanguage());
    }

    /**
     * Opens the next hint on a slot and counts it.
     *
     * <p>Counted rather than blocked. Hints are the interviewer's nudge, and a
     * round that needed two of them is a different round from one that needed
     * none — which the report says, without ever refusing to help.
     *
     * <p>The reveal is server-side for that reason: a client handed all the hints
     * and asked to hide them would be scoring nothing.
     */
    @Transactional
    @PreAuthorize("isAuthenticated()")
    public RevealedHints revealNextHint(Long id, int position) {
        Interview interview = requireRunning(load(id));
        InterviewProblem slot = requireActiveSlot(interview, position);

        List<String> hints = slot.getSnapshot().hints();
        if (slot.getHintsRevealed() >= hints.size()) {
            throw new BusinessRuleException("error.interview.noMoreHints", "There are no further hints");
        }
        slot.setHintsRevealed(slot.getHintsRevealed() + 1);

        // Sliced here rather than by the caller: the unrevealed ones are the
        // thing being paid for, and they must not leave the server at all.
        return new RevealedHints(
                hints.subList(0, slot.getHintsRevealed()), hints.size(), slot.getHintsRevealed());
    }

    /**
     * Moves off a problem for good, to spend what is left of the round on the
     * next one.
     *
     * <p>One-way, which is the whole point. Knowing when to abandon a question
     * you are not going to crack is the skill a two-problem round exists to
     * train, and a decision you can walk back is not a decision. It is also what
     * an interviewer does — they move you on rather than leaving you stuck, so a
     * candidate is never trapped on a problem they cannot solve.
     */
    @Transactional
    @PreAuthorize("isAuthenticated()")
    public Interview skip(Long id, int position) {
        Interview interview = requireRunning(load(id));
        InterviewProblem slot = requireActiveSlot(interview, position);

        slot.setSkipped(true);

        return interview;
    }

    /**
     * The frozen problem to judge against, having checked the round may still be
     * worked on.
     *
     * <p>Its own short transaction because the judging that follows must hold no
     * connection at all — which is also why the snapshot is handed out whole
     * rather than re-read later: by the time the judge returns, this transaction
     * is long closed.
     */
    @Transactional
    @PreAuthorize("isAuthenticated()")
    public ProblemSnapshot requireRunningSnapshot(Long id, int position) {
        Interview interview = requireRunning(load(id));
        return requireActiveSlot(interview, position).getSnapshot();
    }

    /**
     * Attributes a judged submission to its slot.
     *
     * <p>Loads the interview <em>without</em> closing an expired one first, which
     * is the whole subtlety here: a submission legally started with ten seconds
     * left may be judged for a minute afterwards, and the candidate should keep
     * it. The round is closed immediately after, so nothing else gets in.
     */
    @Transactional
    @PreAuthorize("isAuthenticated()")
    public void recordAttempt(Long id, int position, Long submissionId, boolean accepted) {
        Interview interview = owned(id);
        InterviewProblem slot = slotAt(interview, position);

        slot.setAttempts(slot.getAttempts() + 1);
        // Recorded on every attempt, not just the accepted one: this is what a
        // closed problem displays, and it has to be the code the judge saw
        // rather than whatever the editor's draft has drifted to since.
        slot.setLastSubmission(submissionRepository.getReferenceById(submissionId));

        boolean newlySolved = accepted && !slot.isSolved();
        if (newlySolved) {
            slot.setSolved(true);
            slot.setSkipped(false);
            slot.setSolvedAt(Instant.now());
            slot.setSolvedBySubmission(submissionRepository.getReferenceById(submissionId));
        }

        if (interview.isActive() && interview.isExpired(Instant.now())) {
            close(interview, interview.deadline());
        } else if (newlySolved && interview.getStatus() == InterviewStatus.COMPLETED) {
            // The buzzer beat the judge. Something else — a poll from the session
            // screen, another tab — closed the round while this submission was
            // still in the sandbox, freezing a score that did not yet include it.
            // The submission was legally started, so it counts, and the frozen
            // score has to be frozen again over the top.
            interview.setScore((int) interview.solvedCount());
            interview.setOutcome(outcomeOf(interview, interview.getEndedAt()));
        }
    }

    // ── The debrief ───────────────────────────────────────────────────────

    /**
     * Process observations about a finished round, most useful first.
     *
     * <p>All of them are about <em>how</em> rather than <em>whether</em>: the
     * score already says whether. What a candidate working alone cannot see is
     * that the opener took half the round, or that four submissions went in on a
     * problem they had not re-read.
     */
    public List<InterviewInsight> insights(Interview interview) {
        List<InterviewProblem> slots = interview.getProblems();
        if (slots.isEmpty()) {
            return List.of();
        }

        int total = slots.size();
        int solved = (int) interview.solvedCount();
        int attempts = slots.stream().mapToInt(InterviewProblem::getAttempts).sum();
        int hints = slots.stream().mapToInt(InterviewProblem::getHintsRevealed).sum();
        long budget = (long) interview.getDurationMinutes() * 60;
        long elapsed = interview.elapsedSeconds(Instant.now());
        boolean ranOut = interview.getEndedAt() != null && !interview.getEndedAt().isBefore(interview.deadline());

        // Ordered by what a candidate can act on, negatives before compliments,
        // and truncated — so a round with six things to say leads with the ones
        // that would change the next round.
        Map<InterviewInsight, Boolean> candidates = new LinkedHashMap<>();
        candidates.put(InterviewInsight.NO_SUBMISSION, attempts == 0);
        candidates.put(InterviewInsight.RAN_OUT_OF_TIME, ranOut && solved < total);
        candidates.put(InterviewInsight.SLOW_WARM_UP, slowWarmUp(slots, budget));
        candidates.put(
                InterviewInsight.MANY_ATTEMPTS, solved > 0 && attempts >= solved * MANY_ATTEMPTS_PER_SOLVE);
        candidates.put(InterviewInsight.HINTS_USED, hints > 0);
        candidates.put(InterviewInsight.SKIPPED_PROBLEM, slots.stream().anyMatch(InterviewProblem::isSkipped));
        candidates.put(
                InterviewInsight.CLEAN_RUN, solved == total && hints == 0 && attempts == total);
        candidates.put(
                InterviewInsight.FINISHED_EARLY,
                solved == total && !ranOut && elapsed <= budget * EARLY_FINISH_PERCENT / 100);
        candidates.put(
                InterviewInsight.ALL_SOLVED,
                solved == total && !(hints == 0 && attempts == total));

        return candidates.entrySet().stream()
                .filter(Map.Entry::getValue)
                .map(Map.Entry::getKey)
                .limit(MAX_INSIGHTS)
                .toList();
    }

    /**
     * The opener is meant to be the cheap one. Spending most of the round on it
     * is the single most common way a two-problem interview is failed, and it is
     * invisible without a stopwatch — so it is worth its own line in the report.
     */
    private static boolean slowWarmUp(List<InterviewProblem> slots, long budget) {
        return slots.stream()
                .filter(InterviewProblem::isWarmUp)
                .anyMatch(slot -> {
                    Integer spent = slot.timeToSolveSeconds();
                    return spent != null && spent > budget * WARM_UP_BUDGET_PERCENT / 100;
                });
    }

    // ── Sampling ──────────────────────────────────────────────────────────

    /**
     * One problem for a slot.
     *
     * <p>Two passes. The first honours the cooldown; the second drops it, because
     * a catalogue too small to avoid a repeat should still be able to start a
     * round rather than refuse one. Within each pass the wanted difficulty is
     * tried first and neighbours after it, so a thin catalogue degrades to a
     * slightly-off question instead of no interview at all.
     */
    private Long draw(Difficulty wanted, Long userId, Set<Long> alreadyChosen, Set<Long> onCooldown) {
        Set<Long> withCooldown = new LinkedHashSet<>(alreadyChosen);
        withCooldown.addAll(onCooldown);

        for (Set<Long> excluded : List.of(withCooldown, alreadyChosen)) {
            for (Difficulty difficulty : difficultyPreference(wanted)) {
                Long picked = sample(difficulty, userId, excluded);
                if (picked != null) {
                    return picked;
                }
            }
        }

        throw new BusinessRuleException(
                "error.interview.noProblems", "The catalogue has no problems an interview could use");
    }

    /**
     * A random problem from the freshest tier available.
     *
     * <p>"Freshest" is the point: unattempted before attempted, attempted before
     * solved. Preferring what the candidate has never seen is what makes a mock
     * drawn from the public catalogue still feel like a mock, and it improves on
     * its own every time a problem is added.
     */
    private Long sample(Difficulty difficulty, Long userId, Set<Long> excluded) {
        Collection<Long> exclusions = excluded.isEmpty() ? List.of(NO_SUCH_PROBLEM_ID) : excluded;
        List<InterviewCandidate> candidates =
                problemRepository.findInterviewCandidates(difficulty, userId, exclusions);

        if (candidates.isEmpty()) {
            return null;
        }

        int freshest = candidates.stream()
                .mapToInt(InterviewCandidate::getFamiliarity)
                .min()
                .orElseThrow();
        List<Long> pool = new ArrayList<>(candidates.stream()
                .filter(candidate -> candidate.getFamiliarity() == freshest)
                .map(InterviewCandidate::getId)
                .toList());

        return pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
    }

    /** Wanted difficulty first, then the nearest neighbours. */
    private static List<Difficulty> difficultyPreference(Difficulty wanted) {
        return switch (wanted) {
            case EASY -> List.of(Difficulty.EASY, Difficulty.MEDIUM, Difficulty.HARD);
            case MEDIUM -> List.of(Difficulty.MEDIUM, Difficulty.EASY, Difficulty.HARD);
            case HARD -> List.of(Difficulty.HARD, Difficulty.MEDIUM, Difficulty.EASY);
        };
    }

    // ── Internals ─────────────────────────────────────────────────────────

    /**
     * Scoped to the owner in the query rather than checked after loading, so
     * someone else's id is a 404 and not a 403 — which would confirm it exists.
     */
    /**
     * Reads a problem in full so it can be frozen onto a slot.
     *
     * <p>Every collection the round will need is pulled in here, separately
     * rather than as one fetch join — three List associations in a single join is
     * Hibernate's MultipleBagFetchException, the same reason the authoring screen
     * loads them one at a time.
     */
    private ProblemSnapshot snapshot(Long problemId) {
        Problem problem = problemRepository
                .findById(problemId)
                .orElseThrow(() -> NotFoundException.of("problem", String.valueOf(problemId)));

        problem.getParameters().size();
        problem.getExamples().size();
        problem.getHints().size();
        problem.getTestCases().size();

        return ProblemSnapshot.of(problem);
    }

    private Interview owned(Long id) {
        return interviewRepository
                .findOwned(id, SecurityUtils.requireCurrentUserId())
                .orElseThrow(() -> NotFoundException.of("interview", id));
    }

    /** Freezes the score and the band. Nothing about a closed round changes again. */
    private void close(Interview interview, Instant endedAt) {
        interview.setStatus(InterviewStatus.COMPLETED);
        interview.setEndedAt(endedAt);
        interview.setScore((int) interview.solvedCount());
        interview.setOutcome(outcomeOf(interview, endedAt));
    }

    /** Sweeps rounds left running by a closed tab, at the deadline they expired on. */
    private void closeExpired(Long userId, Instant now) {
        interviewRepository.findByUserIdAndStatus(userId, InterviewStatus.IN_PROGRESS).stream()
                .filter(interview -> interview.isExpired(now))
                .forEach(interview -> close(interview, interview.deadline()));
    }

    /**
     * Four coarse bands, not a percentage: what a candidate can act on is
     * "solved, but with a hint and with a minute to spare", and a number invites
     * optimising the number.
     */
    private static InterviewOutcome outcomeOf(Interview interview, Instant endedAt) {
        List<InterviewProblem> slots = interview.getProblems();
        int solved = (int) interview.solvedCount();

        if (slots.isEmpty() || solved == 0) {
            return InterviewOutcome.NO_SOLVE;
        }
        if (solved < slots.size()) {
            return InterviewOutcome.PARTIAL;
        }

        boolean unaided = slots.stream().allMatch(slot -> slot.getHintsRevealed() == 0);
        long budget = (long) interview.getDurationMinutes() * 60;
        boolean unhurried = interview.elapsedSeconds(endedAt) <= budget * COMFORTABLE_TIME_PERCENT / 100;

        return unaided && unhurried ? InterviewOutcome.STRONG : InterviewOutcome.SOLID;
    }

    /**
     * The problem the round is currently on.
     *
     * <p>Anything that changes a slot goes through here, so the sequence is a
     * property of the server rather than of the client's tab strip: a request
     * aimed at a problem that has been moved on from, or at one not yet reached,
     * is refused whatever the UI happens to be showing.
     */
    private static InterviewProblem requireActiveSlot(Interview interview, int position) {
        InterviewProblem slot = slotAt(interview, position);

        if (slot.isResolved()) {
            throw new BusinessRuleException(
                    "error.interview.problemClosed", "You have already moved on from that problem");
        }
        requireReachable(interview, position);

        return slot;
    }

    /** A problem is readable once reached; the ones after it are not yet in play. */
    private static void requireReachable(Interview interview, int position) {
        int active = interview.activePosition().orElse(interview.getProblems().size());
        if (position > active) {
            throw new BusinessRuleException(
                    "error.interview.problemLocked", "Finish the current problem before opening the next one");
        }
    }

    private static Interview requireRunning(Interview interview) {
        if (!interview.isActive()) {
            throw new BusinessRuleException("error.interview.finished", "That interview has finished");
        }
        return interview;
    }

    private static InterviewProblem slotAt(Interview interview, int position) {
        return interview
                .slotAt(position)
                .orElseThrow(() -> NotFoundException.of("interviewProblem", position));
    }

    /**
     * A slot put on screen.
     *
     * @param status the interview's, so a client polling across the buzzer learns
     *     the round is over from the same call that asked for the problem
     * @param editable false for a problem already moved on from — it can still be
     *     read, but nothing more may be sent against it
     * @param submittedSourceCode the code the judge actually saw, present only
     *     once the problem is closed. Null when it was skipped without ever
     *     being submitted, which is the one case with nothing authoritative to
     *     show
     */
    public record OpenSlot(
            InterviewStatus status,
            ProblemSnapshot snapshot,
            int position,
            boolean warmUp,
            boolean solved,
            boolean skipped,
            int attempts,
            int hintsRevealed,
            boolean editable,
            String submittedSourceCode,
            Language submittedLanguage) {}

    /**
     * @param hints the revealed prefix, in order — never the whole list
     * @param available how many the problem had when the round began, so the
     *     button knows when to stop
     */
    public record RevealedHints(List<String> hints, int available, int revealed) {}
}
