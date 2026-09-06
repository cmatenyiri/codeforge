package com.codeforge.web.dto.interview;

/**
 * The candidate's own answer to "did you look anything up?", set from the
 * report screen.
 *
 * <p>Null is a valid value and means "not saying". Nothing is enforced and
 * nothing is penalised: a self-guided mock has no stakes to cheat for, so the
 * only thing worth building is a way for the one person who reads the history
 * to keep it honest.
 */
public record SelfReportRequest(Boolean usedOutsideHelp) {}
