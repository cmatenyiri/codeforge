package com.codeforge.web.dto.interview;

import com.codeforge.domain.InterviewFormat;
import com.codeforge.domain.InterviewStatus;
import java.time.Instant;
import java.util.List;

/**
 * A running interview, as the session screen needs it.
 *
 * @param remainingSeconds computed on the server on every read. The browser
 *     counts down from it for the sake of a smooth clock, but it is this number
 *     that decides anything — a suspended laptop, a clock skew or a devtools
 *     console must not be able to buy time
 * @param status included even though this endpoint only ever serves a running
 *     interview: a session polled across the buzzer comes back finished, and
 *     that is the client's cue to go to the report
 * @param activePosition the problem the round is on, or null once every one has
 *     been solved or skipped and only the debrief is left
 */
public record InterviewSessionResponse(
        Long id,
        InterviewFormat format,
        InterviewStatus status,
        Instant startedAt,
        int durationMinutes,
        long remainingSeconds,
        Integer activePosition,
        List<InterviewSlotResponse> problems) {}
