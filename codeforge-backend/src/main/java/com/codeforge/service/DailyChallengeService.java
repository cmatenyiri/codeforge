package com.codeforge.service;

import com.codeforge.domain.DailyChallenge;
import com.codeforge.domain.Problem;
import com.codeforge.domain.Streaks;
import com.codeforge.exception.BusinessRuleException;
import com.codeforge.exception.NotFoundException;
import com.codeforge.repository.DailyChallengeRepository;
import com.codeforge.repository.ProblemRepository;
import com.codeforge.repository.SubmissionRepository;
import com.codeforge.security.SecurityUtils;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * The problem of the day, and the streak it drives.
 *
 * <h2>Why the row is written down</h2>
 *
 * <p>A date's problem is chosen once and then never changes. It would be less
 * code to derive it from the catalogue on demand — hash the date, index the
 * list — but that answer moves every time a problem is published or retired,
 * and a streak is a claim about what somebody did on a particular day. If
 * Tuesday's problem can become a different problem on Thursday, nobody's streak
 * can be checked, including by the person who earned it.
 *
 * <p>So the rotation only ever decides a date that has no row yet, and the
 * result is persisted. From then on the catalogue can change freely underneath
 * it.
 *
 * <h2>What counts for the streak</h2>
 *
 * <p>Solving the day's problem <em>on that day</em>, in UTC. Coming back to
 * Tuesday's problem on Thursday solves the problem — it counts on the catalogue
 * and in the activity calendar like any other solve — but it does not extend the
 * streak, because a streak that could be filled in afterwards would measure
 * nothing but persistence at backfilling.
 *
 * <p>UTC rather than the viewer's zone, so the challenge turns over at one
 * instant worldwide and two people comparing streaks are counting the same days.
 */
@Service
@RequiredArgsConstructor
public class DailyChallengeService {

    /**
     * How long a problem stays out of the rotation after being the daily.
     *
     * <p>Long enough that a catalogue of any reasonable size never repeats
     * within it, and harmless when the catalogue is smaller than the window —
     * the pool falls back to everything rather than refusing to pick.
     */
    private static final int COOLDOWN_DAYS = 60;

    /**
     * How far ahead a date may be asked for.
     *
     * <p>Materialising a date writes its problem down for good, so an unbounded
     * lookahead would let one request fix the next decade of daily challenges
     * against today's catalogue.
     */
    private static final int MAX_LOOKAHEAD_DAYS = 30;

    private final DailyChallengeRepository dailyChallengeRepository;
    private final ProblemRepository problemRepository;
    private final SubmissionRepository submissionRepository;

    /** Today, on the one clock the whole feature runs on. */
    public static LocalDate today() {
        return LocalDate.now(ZoneOffset.UTC);
    }

    // ── Reading ───────────────────────────────────────────────────────────

    /**
     * The challenge for a date, choosing one if the date has none yet.
     *
     * <p>Its own new transaction so the write commits even when the caller is
     * only reading — this is a read that writes, like a contest sealing itself,
     * and for the same reason: nothing runs on a schedule, so the first request
     * of the day is what decides the day.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public DailyChallenge forDate(LocalDate date) {
        if (date.isAfter(today().plusDays(MAX_LOOKAHEAD_DAYS))) {
            throw new BusinessRuleException(
                    "error.daily.tooFarAhead", "That date is too far in the future");
        }

        return dailyChallengeRepository
                .findByDate(date)
                .orElseGet(() -> dailyChallengeRepository.save(rotate(date)));
    }

    @Transactional
    @PreAuthorize("isAuthenticated()")
    public DailyChallenge todaysChallenge() {
        DailyChallenge challenge = forDate(today());
        // Pulled in here because the response names the topics and open-in-view
        // is off — a lazy load would fail once the mapper runs outside this
        // transaction.
        challenge.getProblem().getTags().size();

        return challenge;
    }

    /**
     * A month of challenges, for the calendar widget.
     *
     * <p>Only dates up to today are materialised. Filling a whole month ahead
     * would fix tomorrow's problem — and next Friday's — against whatever the
     * catalogue happens to hold right now, which is a decision no widget render
     * should be making.
     */
    @Transactional
    @PreAuthorize("isAuthenticated()")
    public Month month(YearMonth month) {
        LocalDate from = month.atDay(1);
        LocalDate to = month.atEndOfMonth();
        LocalDate today = today();

        for (LocalDate date = from; !date.isAfter(to) && !date.isAfter(today); date = date.plusDays(1)) {
            forDate(date);
        }

        List<DailyChallenge> challenges = dailyChallengeRepository.findBetween(from, to);
        Set<String> solved = new LinkedHashSet<>(dailyChallengeRepository.findSolvedDatesBetween(
                SecurityUtils.requireCurrentUserId(), from, to));

        return new Month(month, challenges, solved);
    }

    /**
     * The caller's daily-challenge streak.
     *
     * <p>Current and longest, over every day they solved the daily on the day.
     */
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public Streaks streak() {
        return streakOf(SecurityUtils.requireCurrentUserId());
    }

    @Transactional(readOnly = true)
    public Streaks streakOf(Long userId) {
        return Streaks.ofIsoDates(dailyChallengeRepository.findSolvedDates(userId), today());
    }

    /** Whether the caller has already solved today's, for the tick on the widget. */
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public boolean hasSolvedToday() {
        LocalDate today = today();

        return !dailyChallengeRepository
                .findSolvedDatesBetween(SecurityUtils.requireCurrentUserId(), today, today)
                .isEmpty();
    }

    // ── Authoring ─────────────────────────────────────────────────────────

    /**
     * Pins a specific problem to a future date.
     *
     * <p>Refused from the moment a date arrives, today included. That is
     * stricter than it first looks necessary, and it is the whole point:
     * "solved the daily on the day" is decided by joining this row to the
     * submissions made on that date, so swapping the problem at noon rewrites
     * who is considered to have done it. Somebody who cleared the morning's
     * problem silently loses the day, and somebody who happened to solve the
     * replacement for unrelated reasons is silently credited with it. A streak
     * that can be granted or revoked by an edit is not a record of anything.
     *
     * <p>The cost is that a broken daily cannot be swapped out on the day. That
     * is the right trade: one bad day is a bad day, whereas a mutable "today"
     * makes every streak provisional.
     */
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public DailyChallenge pin(LocalDate date, Long problemId) {
        requireFuture(date);

        Problem problem = problemRepository
                .findById(problemId)
                .orElseThrow(() -> NotFoundException.of("problem", problemId));

        if (!problem.isPublished() || problem.isArchived()) {
            throw new BusinessRuleException(
                    "error.daily.notPublished", "A daily challenge has to be a released problem");
        }

        DailyChallenge challenge = dailyChallengeRepository.findByDate(date).orElseGet(() -> {
            DailyChallenge created = new DailyChallenge();
            created.setDate(date);
            return created;
        });
        challenge.setProblem(problem);
        challenge.setPinned(true);

        return dailyChallengeRepository.save(challenge);
    }

    /** Hands a date that has not arrived back to the rotation. */
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void unpin(LocalDate date) {
        requireFuture(date);
        dailyChallengeRepository.findByDate(date).ifPresent(dailyChallengeRepository::delete);
    }

    /**
     * A date whose problem is still an author's to decide.
     *
     * <p>Anything from today backwards is settled — see {@link #pin} for why
     * today counts as settled rather than as the last editable day.
     *
     * <p>The two refusals are reported apart because they mean different things
     * to whoever hit them: a past day is finished and its problem is history,
     * while today is mid-flight and people are solving it right now. Collapsing
     * them into one message leaves an author reading "this day has started"
     * about a Tuesday three weeks ago.
     */
    private static void requireFuture(LocalDate date) {
        LocalDate today = today();

        if (date.isBefore(today)) {
            throw new BusinessRuleException(
                    "error.daily.past", "That day is over and keeps the problem it ran with");
        }
        if (!date.isAfter(today)) {
            throw new BusinessRuleException(
                    "error.daily.settled", "Today is under way and keeps the problem it started with");
        }
    }

    // ── The rotation ──────────────────────────────────────────────────────

    /**
     * Picks a date's problem.
     *
     * <p>Deterministic given the same pool, which is what makes it reproducible
     * while a date is still being decided, and irrelevant afterwards because the
     * answer is written down. Problems used in the last {@link #COOLDOWN_DAYS}
     * are held back, so a catalogue larger than the window never repeats inside
     * it; a smaller catalogue falls back to the whole list rather than refusing
     * to pick a problem for the day.
     */
    private DailyChallenge rotate(LocalDate date) {
        List<Long> candidates = problemRepository.findDailyCandidates();
        if (candidates.isEmpty()) {
            throw new BusinessRuleException(
                    "error.daily.noProblems", "The catalogue has no problem a daily challenge could use");
        }

        Set<Long> recent = Set.copyOf(dailyChallengeRepository.findProblemIdsSince(date.minusDays(COOLDOWN_DAYS)));
        List<Long> pool = new ArrayList<>(candidates.stream().filter(id -> !recent.contains(id)).toList());
        if (pool.isEmpty()) {
            pool = candidates;
        }

        DailyChallenge challenge = new DailyChallenge();
        challenge.setDate(date);
        challenge.setProblem(problemRepository.getReferenceById(pool.get(index(date, pool.size()))));
        challenge.setPinned(false);

        return challenge;
    }

    /**
     * An index into the pool, spread across it rather than walking it in order.
     *
     * <p>A plain {@code epochDay % size} would hand out the catalogue in id
     * order, which after a week is an obvious pattern and after a month is a
     * spoiler. Mixing the bits of the day number first — the finalising step of
     * SplitMix64 — makes consecutive days land in unrelated places while staying
     * completely determined by the date.
     */
    private static int index(LocalDate date, int size) {
        long bits = date.toEpochDay() * 0x9E3779B97F4A7C15L;
        bits = (bits ^ (bits >>> 30)) * 0xBF58476D1CE4E5B9L;
        bits = (bits ^ (bits >>> 27)) * 0x94D049BB133111EBL;
        bits = bits ^ (bits >>> 31);

        return (int) Math.floorMod(bits, size);
    }

    /**
     * One month of the widget's calendar.
     *
     * @param solvedDates ISO days the caller solved the daily on the day itself
     */
    public record Month(YearMonth month, List<DailyChallenge> challenges, Set<String> solvedDates) {}
}
