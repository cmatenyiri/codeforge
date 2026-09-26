package com.codeforge.realtime;

/**
 * A mock interview changed: a verdict, a hint, a skip, or the round closing.
 *
 * <p>Pushed to the round's own topic, where the only listeners are its owner's
 * open tabs — so a round finished in one tab closes in the others, and a solve
 * in one ticks in all of them.
 */
public record InterviewChanged(Long interviewId) {}
