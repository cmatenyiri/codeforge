package com.codeforge.web.dto.interview;

import com.codeforge.domain.Difficulty;
import com.codeforge.domain.InterviewFormat;
import java.util.List;

/**
 * One format offered on the lobby screen.
 *
 * <p>The slot difficulties are sent rather than described in a translated
 * string, so the card can show "Easy, then Medium" without the client hard-coding
 * what each format contains.
 */
public record InterviewFormatResponse(
        InterviewFormat format, int durationMinutes, int problemCount, List<Difficulty> slots) {

    public static InterviewFormatResponse of(InterviewFormat format) {
        return new InterviewFormatResponse(
                format, format.durationMinutes(), format.problemCount(), format.slots());
    }
}
