package com.codeforge.web.dto.submission;

import com.codeforge.domain.Difficulty;
import com.codeforge.domain.Language;
import com.codeforge.domain.SubmissionStatus;
import java.time.Instant;

/**
 * One row in a submission history.
 *
 * <p>Carries the problem's slug and title so a history can be rendered — and
 * linked — without a lookup per row.
 */
public record SubmissionSummaryResponse(
        Long id,
        String problemSlug,
        String problemTitle,
        Difficulty difficulty,
        SubmissionStatus status,
        Language language,
        Integer runtimeMs,
        Integer memoryKb,
        Integer passedTests,
        Integer totalTests,
        Instant createdAt) {}
