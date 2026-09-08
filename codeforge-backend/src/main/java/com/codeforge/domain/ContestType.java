package com.codeforge.domain;

/**
 * The shape of a contest: how long it runs and how many problems it asks.
 *
 * <p>Modelled on the round it simulates rather than on what is convenient to
 * schedule. Ninety minutes for four problems is the format the whole scoring
 * system is tuned around — long enough that the last problem is reachable,
 * short enough that finishing time is a real tiebreak rather than noise.
 *
 * <p>The defaults are what a new contest is pre-filled with, not a cage: an
 * author may change the duration and add or remove problems afterwards. What
 * the type actually fixes is the <em>cadence</em>, which is the thing a
 * participant plans around.
 */
public enum ContestType {

    /** The main event: four problems, weekly. */
    WEEKLY(90, 4),

    /** The same format on the off-week, so there is something every seven days. */
    BIWEEKLY(90, 4),

    /**
     * Anything else — a themed round, a longer set, a one-off.
     *
     * <p>Deliberately last, and deliberately vague. Without it every unusual
     * contest becomes a new enum constant, and the cadence the other two promise
     * stops meaning anything.
     */
    SPECIAL(90, 4);

    private final int defaultDurationMinutes;
    private final int defaultProblemCount;

    ContestType(int defaultDurationMinutes, int defaultProblemCount) {
        this.defaultDurationMinutes = defaultDurationMinutes;
        this.defaultProblemCount = defaultProblemCount;
    }

    public int defaultDurationMinutes() {
        return defaultDurationMinutes;
    }

    public int defaultProblemCount() {
        return defaultProblemCount;
    }
}
