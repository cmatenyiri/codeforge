package com.codeforge.web.dto.interview;

import java.util.List;

/**
 * The hints revealed on one slot so far, after asking for one more.
 *
 * <p>Returns the whole revealed prefix rather than just the new hint, so the
 * panel can render from one field and a repeated click cannot desynchronise it
 * from the count the server is scoring against.
 */
public record InterviewHintResponse(int hintCount, int hintsRevealed, List<String> hints) {}
