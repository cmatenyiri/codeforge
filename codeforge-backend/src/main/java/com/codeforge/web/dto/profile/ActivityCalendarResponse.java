package com.codeforge.web.dto.profile;

import java.util.List;

/**
 * One window of the activity calendar, with the three numbers above it.
 *
 * <p>Two shapes, selected by the picker beside the heading:
 *
 * <ul>
 *   <li>a calendar year, when one is chosen — {@code year} is set and the grid
 *       runs January to December;
 *   <li>the rolling last twelve months, which is the default — {@code year} is
 *       null and the grid ends today.
 * </ul>
 *
 * <p>The counters are always computed over whichever window the grid is
 * showing, so "total active days" can never be describing a different span from
 * the squares underneath it.
 *
 * @param year null for the rolling window
 * @param days only the days with something on them — a year is 365 squares of
 *     which a typical profile lights perhaps sixty, and sending three hundred
 *     zeroes to draw nothing would be the larger half of the response
 * @param maxStreak the longest run of consecutive active days inside this window
 */
public record ActivityCalendarResponse(
        Integer year,
        String from,
        String to,
        long submissions,
        long activeDays,
        int maxStreak,
        List<ActivityDayResponse> days) {}
