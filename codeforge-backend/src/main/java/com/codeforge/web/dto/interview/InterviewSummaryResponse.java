package com.codeforge.web.dto.interview;

import com.codeforge.domain.InterviewFormat;
import com.codeforge.domain.InterviewOutcome;
import com.codeforge.domain.InterviewStatus;
import java.time.Instant;

/**
 * One row in the interview history.
 *
 * <p>Carries no slots: the list shows a date, a format and a score, and loading
 * every round's problems to render that would be a collection fetch per row.
 *
 * @param score solved problems, null for an interview that never finished
 */
public record InterviewSummaryResponse(
        Long id,
        InterviewFormat format,
        InterviewStatus status,
        InterviewOutcome outcome,
        Instant startedAt,
        Instant endedAt,
        int durationMinutes,
        Integer score,
        int total) {}
