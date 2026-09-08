package com.codeforge.web.dto.profile;

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
 * @param rating absent for an account that has never sat a rated contest: the
 *     1500 they nominally carry is a placeholder, and showing it would claim a
 *     measurement that has not been made
 * @param globalRank position on the weighted solved table, absent until they
 *     have solved something
 * @param streak consecutive days up to today with at least one submission
 * @param maxStreak the longest such run they have ever had
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
        int streak,
        int maxStreak,
        long activeDays,
        List<ActivityDayResponse> activity,
        List<RatingPointResponse> ratingHistory,
        List<RecentSolveResponse> recentSolves) {}
