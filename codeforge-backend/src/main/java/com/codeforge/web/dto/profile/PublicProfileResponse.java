package com.codeforge.web.dto.profile;

import com.codeforge.web.dto.badge.BadgeResponse;
import com.codeforge.web.dto.user.UserStatsResponse.DifficultyProgress;
import java.time.Instant;
import java.util.List;

/**
 * Somebody's profile as anyone may see it.
 *
 * <p>Deliberately not the same shape as the caller's own dashboard. There is no
 * email, no submission history of failed attempts, and no draft problems — a
 * public profile is what somebody chose to be judged on, which is their solves,
 * their contests and their consistency.
 *
 * <p>The three panels the client draws map onto three groups here: what they
 * have solved, how they have competed, and how often they turn up. Nothing is
 * duplicated between them, so a number can only be wrong in one place.
 *
 * @param rating absent for an account that has never sat a rated contest: the
 *     1500 they nominally carry is a placeholder, and showing it would claim a
 *     measurement that has not been made
 * @param globalRank position on the weighted solved table, absent until they
 *     have solved something
 * @param activeYears the years the calendar's picker may offer, newest first;
 *     the picker also has a rolling option, which is what a profile opens on
 * @param languages distinct problems solved per language, most first
 * @param badges what the daily challenge has awarded them, newest first
 * @param calendar one year of activity — whichever the caller asked for, or the
 *     current one
 * @param dailyStreak consecutive days solving the daily challenge on the day
 *     itself. A different measurement from the calendar's {@code maxStreak},
 *     which counts any submission at all
 */
public record PublicProfileResponse(
        Long id,
        String username,
        String avatar,
        Instant joinedAt,
        long solved,
        long totalProblems,
        long submissions,
        long acceptedSubmissions,
        Double acceptanceRate,
        List<DifficultyProgress> progress,
        Long globalRank,
        Long globalRankTotal,
        Double rating,
        Double maxRating,
        Integer contestsAttended,
        Long ratingRank,
        Long ratingRankTotal,
        List<Integer> activeYears,
        ActivityCalendarResponse calendar,
        List<LanguageStatResponse> languages,
        List<BadgeResponse> badges,
        int dailyStreak,
        int dailyMaxStreak,
        List<RatingPointResponse> ratingHistory,
        List<RecentSolveResponse> recentSolves) {}
