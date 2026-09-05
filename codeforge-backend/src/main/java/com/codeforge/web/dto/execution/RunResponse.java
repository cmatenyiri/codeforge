package com.codeforge.web.dto.execution;

import com.codeforge.domain.SubmissionStatus;
import java.util.List;

/**
 * The result of running against the sample cases.
 *
 * @param status the run as a whole: the first non-accepted case decides it, so a
 *     mixed run reports the failure rather than a misleading average
 * @param compileOutput set only when nothing ran, and then the only thing worth showing
 * @param passed how many cases matched their expected output
 * @param runtimeMs the slowest case, which is the honest figure to show for a run
 */
public record RunResponse(
        SubmissionStatus status,
        String compileOutput,
        int passed,
        int total,
        Integer runtimeMs,
        Integer memoryKb,
        List<CaseResultResponse> results) {}
