package com.codeforge.domain;

import java.util.List;

/**
 * The shape of a mock interview: how long it runs, and what it asks.
 *
 * <p>Both formats are modelled on the round they simulate rather than on what is
 * convenient to generate. A real coding round is one or two problems in
 * forty-odd minutes — never four — and when there are two, the first is the
 * gentler one an interviewer opens with.
 *
 * <p>Two problems is the default for a reason: with one, the score is a coin
 * flip that mostly measures whether the candidate had seen it before, and
 * nothing tests the budgeting of time, which is what people actually fail these
 * rounds on. Three or more would push the session past the point where a
 * self-guided practice run gets finished, and an abandoned session produces no
 * report at all.
 */
public enum InterviewFormat {

    /** One problem, the length of a single-question screen. */
    WARM_UP(20, List.of(Difficulty.EASY)),

    /**
     * The default: an easier opener, then the real question, in the
     * three-quarters of an hour a technical screen is scheduled for.
     */
    STANDARD(45, List.of(Difficulty.EASY, Difficulty.MEDIUM)),

    /** The same shape, pitched at an on-site's second round. */
    HARD(45, List.of(Difficulty.MEDIUM, Difficulty.HARD));

    private final int durationMinutes;
    private final List<Difficulty> slots;

    InterviewFormat(int durationMinutes, List<Difficulty> slots) {
        this.durationMinutes = durationMinutes;
        this.slots = List.copyOf(slots);
    }

    public int durationMinutes() {
        return durationMinutes;
    }

    /** The difficulty wanted at each position, warm-up first. */
    public List<Difficulty> slots() {
        return slots;
    }

    public int problemCount() {
        return slots.size();
    }
}
