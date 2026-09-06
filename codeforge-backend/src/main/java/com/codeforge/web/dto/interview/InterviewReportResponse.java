package com.codeforge.web.dto.interview;

import com.codeforge.domain.InterviewFormat;
import com.codeforge.domain.InterviewInsight;
import com.codeforge.domain.InterviewOutcome;
import com.codeforge.domain.InterviewStatus;
import java.time.Instant;
import java.util.List;

/**
 * What a finished interview is worth reading for.
 *
 * <p>Modelled on the debrief a real mock ends with rather than on a score
 * screen: a band, the round broken down problem by problem, and a few
 * observations about how the time was spent. Every number here is the
 * candidate's own — none of it is comparable between users, and nothing in this
 * response leaves the account that produced it.
 *
 * @param outcome the band, null while the interview is still running
 * @param elapsedSeconds how much of the budget was actually used — the whole
 *     round for an expired one, less for a round finished early
 * @param insights process observations, as codes the client translates. Ordered
 *     most useful first and capped, because a list of nine is a list nobody reads
 */
public record InterviewReportResponse(
        Long id,
        InterviewFormat format,
        InterviewStatus status,
        InterviewOutcome outcome,
        Instant startedAt,
        Instant endedAt,
        int durationMinutes,
        long elapsedSeconds,
        int solved,
        int total,
        int attempts,
        int hintsRevealed,
        Boolean usedOutsideHelp,
        List<InterviewProblemResultResponse> problems,
        List<InterviewInsight> insights) {}
