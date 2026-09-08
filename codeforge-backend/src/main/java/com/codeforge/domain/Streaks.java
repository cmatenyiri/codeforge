package com.codeforge.domain;

import java.time.LocalDate;
import java.util.List;

/**
 * The current and longest runs of consecutive days in a set of dates.
 *
 * <p>Shared because the product counts two different streaks the same way: the
 * activity calendar's, over every day somebody submitted anything, and the daily
 * challenge's, over every day they solved that day's problem. Only the set of
 * dates differs — the arithmetic, and the awkward question of what "current"
 * means before today is over, are identical.
 *
 * @param current the run ending at the most recent date, or zero when that date
 *     is too long ago to still be running
 * @param longest the longest run anywhere in the set; never smaller than
 *     {@link #current}, which it contains
 */
public record Streaks(int current, int longest) {

    public static final Streaks NONE = new Streaks(0, 0);

    /**
     * Computes both runs.
     *
     * <p>A day missing from the middle ends a run. A missing <em>today</em> does
     * not: somebody looking at their profile over breakfast has not yet had the
     * chance to do anything, and telling them an eighty-day run had ended would
     * be both wrong and, briefly, awful. So a run is still current if it reaches
     * yesterday, and it only dies once a whole day has passed unanswered.
     *
     * @param dates distinct days, newest first
     * @param today the day to measure "still running" against, in the same zone
     *     the dates were bucketed in
     */
    public static Streaks of(List<LocalDate> dates, LocalDate today) {
        if (dates.isEmpty()) {
            return NONE;
        }

        int current = 0;
        if (!dates.getFirst().isBefore(today.minusDays(1))) {
            current = 1;
            for (int i = 1; i < dates.size(); i++) {
                if (!dates.get(i).equals(dates.get(i - 1).minusDays(1))) {
                    break;
                }
                current++;
            }
        }

        int longest = 1;
        int run = 1;
        for (int i = 1; i < dates.size(); i++) {
            run = dates.get(i).equals(dates.get(i - 1).minusDays(1)) ? run + 1 : 1;
            longest = Math.max(longest, run);
        }

        return new Streaks(current, Math.max(longest, current));
    }

    /** Parses ISO day strings — the shape both day queries come back in. */
    public static Streaks ofIsoDates(List<String> dates, LocalDate today) {
        return of(dates.stream().map(LocalDate::parse).toList(), today);
    }
}
