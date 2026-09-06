package com.codeforge.domain;

public enum Difficulty {
    EASY(1),
    MEDIUM(2),
    HARD(3);

    /**
     * Sort order, persisted alongside the problem.
     *
     * <p>The enum is stored as a string, so ordering by the column alphabetically
     * gives EASY, HARD, MEDIUM. Sorting a catalogue by difficulty has to mean
     * "easiest first", which is what this rank is for.
     */
    private final int rank;

    Difficulty(int rank) {
        this.rank = rank;
    }

    public int rank() {
        return rank;
    }
}
