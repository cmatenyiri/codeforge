package com.codeforge.domain;

/**
 * The kinds of badge the daily challenge awards.
 *
 * <p>Both are computed from the record rather than stored: a badge is a
 * statement about what somebody did, and the days they did it are already
 * written down. Nothing has to be granted, nothing can be granted twice, and a
 * rejudge that changed a verdict changes the badges with it.
 */
public enum BadgeKind {

    /**
     * Every daily challenge in one month, cleared on the day.
     *
     * <p>The hard one, and the reason the daily challenge has a following at
     * all: one missed day and the month is gone until the next one starts.
     */
    MONTHLY,

    /**
     * Fifty days of daily challenges within one calendar year.
     *
     * <p>Days, not consecutive days. It rewards turning up often across a year,
     * which is a different thing from the streak and survives one bad week.
     */
    ANNUAL_50,

    /** The same at a hundred days. */
    ANNUAL_100,

    /** Every day of the year — the one almost nobody has. */
    ANNUAL_365
}
