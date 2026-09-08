package com.codeforge.service;

import com.codeforge.domain.Difficulty;
import com.codeforge.domain.Language;
import com.codeforge.domain.Streaks;
import com.codeforge.domain.User;
import com.codeforge.exception.NotFoundException;
import com.codeforge.repository.ContestRatingChangeRepository;
import com.codeforge.repository.ProblemRepository;
import com.codeforge.repository.SubmissionRepository;
import com.codeforge.repository.UserRepository;
import com.codeforge.web.dto.profile.ActivityCalendarResponse;
import com.codeforge.web.dto.profile.ActivityDayResponse;
import com.codeforge.web.dto.profile.LanguageStatResponse;
import com.codeforge.web.dto.profile.LeaderboardRowResponse;
import com.codeforge.web.dto.profile.PublicProfileResponse;
import com.codeforge.web.dto.profile.RatingPointResponse;
import com.codeforge.web.dto.profile.RecentSolveResponse;
import com.codeforge.web.dto.user.UserStatsResponse.DifficultyProgress;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Somebody else's profile, and the tables that put everybody in order.
 *
 * <p>A public profile is deliberately a different document from the signed-in
 * dashboard rather than the same one with fields removed. It carries what a
 * person is willing to be judged on — what they have solved, how they have
 * competed, how consistently they turn up — and none of the things that only
 * make sense to their owner: no email, no failed attempts, no drafts.
 *
 * <p>Everything here is read-only and derived on demand. Nothing about a profile
 * is stored as a profile, so there is no second copy of anybody's record to fall
 * out of step with the submissions it is a summary of.
 */
@Service
@RequiredArgsConstructor
public class ProfileService {


    /** Enough recent solves to show what somebody is working on, few enough to read. */
    private static final int RECENT_SOLVES = 15;

    private final UserRepository userRepository;
    private final SubmissionRepository submissionRepository;
    private final ProblemRepository problemRepository;
    private final ContestRatingChangeRepository ratingChangeRepository;
    private final DailyChallengeService dailyChallengeService;
    private final BadgeService badgeService;

    /**
     * One person's public profile.
     *
     * <p>Looked up by username rather than id: a profile URL is something people
     * type and share, and {@code /u/ada} is the only form of it worth having.
     */
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public PublicProfileResponse profile(String username, Integer year, Locale locale) {
        User user = userRepository
                .findByUsernameIgnoreCase(username)
                .orElseThrow(() -> NotFoundException.of("user", username));

        if (!user.isEnabled()) {
            // A disabled account is not a 403: it should not be discoverable as
            // an account that exists at all.
            throw NotFoundException.of("user", username);
        }

        Long userId = user.getId();
        List<DifficultyProgress> progress = progressOf(userId);
        long solved = progress.stream().mapToLong(DifficultyProgress::solved).sum();
        long submissions = submissionRepository.countByUserId(userId);
        long accepted = submissionRepository.countAcceptedByUserId(userId);

        // Only years with something in them are offered, and a requested year is
        // honoured only if it is one of them — a picker that can select an empty
        // grid is a picker that can be wrong. Anything else falls back to the
        // rolling window, which is what the profile opens on.
        List<Integer> years = submissionRepository.findActiveYears(userId);
        Integer selectedYear = year != null && years.contains(year) ? year : null;

        Streaks daily = dailyChallengeService.streakOf(userId);

        long solvedPoints = progress.stream()
                .mapToLong(entry -> entry.solved() * entry.difficulty().rank())
                .sum();

        return new PublicProfileResponse(
                userId,
                user.getUsername(),
                user.getAvatar().name(),
                user.getCreatedAt(),
                solved,
                progress.stream().mapToLong(DifficultyProgress::total).sum(),
                submissions,
                accepted,
                submissions == 0 ? null : (double) accepted / submissions,
                progress,
                // Absent rather than "last place" for somebody who has solved
                // nothing: they are not in the table, and a rank would claim they
                // were.
                solved == 0 ? null : userRepository.countSolvedRankedAbove(solvedPoints) + 1,
                solved == 0 ? null : userRepository.countSolvedRanked(),
                // The starting 1500 is an assumption, not a measurement. Showing
                // it for somebody who has never competed would put a number on a
                // thing nobody has observed.
                user.hasRating() ? user.getRating() : null,
                user.hasRating() ? user.getMaxRating() : null,
                user.hasRating() ? user.getContestsAttended() : null,
                user.hasRating() ? userRepository.countRatedAbove(user.getRating()) + 1 : null,
                user.hasRating() ? userRepository.countRated() : null,
                years,
                calendarOf(userId, selectedYear),
                languagesOf(userId),
                badgeService.badgesOf(userId, locale),
                daily.current(),
                daily.longest(),
                ratingHistoryOf(userId),
                recentSolvesOf(userId));
    }

    /** Solved-out-of-total at each difficulty, in easiest-first order. */
    private List<DifficultyProgress> progressOf(Long userId) {
        Map<Difficulty, Long> catalogue = byDifficulty(problemRepository.countByDifficulty());
        Map<Difficulty, Long> solved = byDifficulty(submissionRepository.countSolvedByDifficultyFor(userId));

        return Arrays.stream(Difficulty.values())
                .map(difficulty -> new DifficultyProgress(
                        difficulty,
                        solved.getOrDefault(difficulty, 0L),
                        catalogue.getOrDefault(difficulty, 0L)))
                .toList();
    }

    private static Map<Difficulty, Long> byDifficulty(List<ProblemRepository.DifficultyTotal> rows) {
        return rows.stream()
                .collect(Collectors.toMap(
                        ProblemRepository.DifficultyTotal::getDifficulty,
                        ProblemRepository.DifficultyTotal::getTotal,
                        Long::sum,
                        () -> new EnumMap<>(Difficulty.class)));
    }

    /**
     * One window of daily submission counts, plus the numbers above it.
     *
     * <p>A calendar year when one is asked for, otherwise the rolling twelve
     * months ending today — which is the default because it is the honest answer
     * to "how have they been lately". January the second is a bad day to be told
     * you have two active days.
     *
     * <p>Every counter is derived from the same set of days as the grid, so they
     * cannot end up describing different spans.
     */
    private ActivityCalendarResponse calendarOf(Long userId, Integer year) {
        LocalDate today = DailyChallengeService.today();
        LocalDate fromDay = year == null ? today.minusYears(1).plusDays(1) : LocalDate.of(year, 1, 1);
        LocalDate toDay = year == null ? today : LocalDate.of(year, 12, 31);

        Instant from = fromDay.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant until = toDay.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        List<ActivityDayResponse> days = submissionRepository.findActivityBetween(userId, from, until).stream()
                .map(day -> new ActivityDayResponse(day.getDay(), day.getTotal(), day.getAccepted()))
                .toList();

        // Newest first, which is the order Streaks walks in.
        List<LocalDate> dates = days.stream()
                .map(day -> LocalDate.parse(day.date()))
                .sorted(Comparator.reverseOrder())
                .toList();

        return new ActivityCalendarResponse(
                year,
                fromDay.toString(),
                toDay.toString(),
                days.stream().mapToLong(ActivityDayResponse::submissions).sum(),
                days.size(),
                Streaks.of(dates, today).longest(),
                days);
    }

    /**
     * The calendar on its own, for the year picker.
     *
     * <p>Its own endpoint so changing the window re-fetches a grid of squares
     * rather than the whole profile — the contest history and the rating graph
     * do not change when you look at 2024.
     *
     * @param year null for the rolling twelve months
     */
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public ActivityCalendarResponse calendar(String username, Integer year) {
        User user = userRepository
                .findByUsernameIgnoreCase(username)
                .orElseThrow(() -> NotFoundException.of("user", username));

        return calendarOf(user.getId(), year);
    }

    /**
     * Distinct problems solved in each language, most first.
     *
     * <p>Empty for somebody who has solved nothing, which the profile renders as
     * an absent section rather than an empty list with a heading.
     */
    private List<LanguageStatResponse> languagesOf(Long userId) {
        return submissionRepository.countSolvedByLanguage(userId).stream()
                .map(row -> new LanguageStatResponse(Language.valueOf(row.getLanguage()), row.getSolved()))
                .toList();
    }

    /** Every rating movement, oldest first — the graph and the contest history in one list. */
    private List<RatingPointResponse> ratingHistoryOf(Long userId) {
        return ratingChangeRepository.findHistory(userId).stream()
                .map(change -> new RatingPointResponse(
                        change.getContest().getSlug(),
                        change.getContest().getTitle(),
                        change.getContest().getStartsAt(),
                        change.getRank(),
                        change.getParticipantCount(),
                        change.getRatingBefore(),
                        change.getRatingAfter(),
                        change.getDelta()))
                .toList();
    }

    private List<RecentSolveResponse> recentSolvesOf(Long userId) {
        return submissionRepository.findRecentSolves(userId, RECENT_SOLVES).stream()
                .map(solve -> new RecentSolveResponse(
                        solve.getSlug(),
                        solve.getTitle(),
                        Difficulty.valueOf(solve.getDifficulty()),
                        solve.getSolvedAt()))
                .toList();
    }

    // ── Leaderboards ──────────────────────────────────────────────────────

    /**
     * The rating table.
     *
     * <p>Ranks are the row's position and nothing cleverer. Two people on exactly
     * the same rating to fifteen decimal places is not a tie worth modelling,
     * unlike a contest, where sharing a place is the common case.
     */
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public Board ratingLeaderboard(int page, int size) {
        List<User> users = userRepository.findRatingLeaderboard(PageRequest.of(page, size));
        long offset = (long) page * size;

        List<LeaderboardRowResponse> rows = new ArrayList<>(users.size());
        for (int index = 0; index < users.size(); index++) {
            User user = users.get(index);
            rows.add(new LeaderboardRowResponse(
                    offset + index + 1,
                    user.getId(),
                    user.getUsername(),
                    user.getAvatar().name(),
                    user.getRating(),
                    user.getContestsAttended(),
                    null,
                    null));
        }

        return new Board(rows, userRepository.countRated());
    }

    /** The weighted solved table: easy problems count, hard ones count more. */
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public Board solvedLeaderboard(int page, int size) {
        long offset = (long) page * size;
        List<UserRepository.SolvedRanking> ranked =
                userRepository.findSolvedLeaderboard(size, (int) offset);

        List<LeaderboardRowResponse> rows = new ArrayList<>(ranked.size());
        for (int index = 0; index < ranked.size(); index++) {
            UserRepository.SolvedRanking row = ranked.get(index);
            rows.add(new LeaderboardRowResponse(
                    offset + index + 1,
                    row.getUserId(),
                    row.getUsername(),
                    row.getAvatar(),
                    null,
                    null,
                    row.getSolved(),
                    row.getPoints()));
        }

        return new Board(rows, userRepository.countSolvedRanked());
    }

    /** A page of a global table, with how many rows the whole table has. */
    public record Board(List<LeaderboardRowResponse> rows, long total) {}
}
