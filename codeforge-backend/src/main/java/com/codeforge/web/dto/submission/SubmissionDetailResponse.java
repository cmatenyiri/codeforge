package com.codeforge.web.dto.submission;

import com.codeforge.domain.Difficulty;
import com.codeforge.domain.Language;
import com.codeforge.domain.SubmissionStatus;
import java.time.Instant;

/** A submission with the code that produced it, for the "view submission" panel. */
public record SubmissionDetailResponse(
        Long id,
        String problemSlug,
        String problemTitle,
        Difficulty difficulty,
        SubmissionStatus status,
        Language language,
        String sourceCode,
        String failureMessage,
        Integer runtimeMs,
        Integer memoryKb,
        Integer passedTests,
        Integer totalTests,
        Instant createdAt) {}
