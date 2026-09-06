package com.codeforge.web.dto.submission;

import com.codeforge.domain.SubmissionStatus;
import com.codeforge.web.dto.execution.CaseResultResponse;
import java.util.List;

/**
 * The verdict on a submission, returned the moment it is judged.
 *
 * @param submissionId the recorded row, so the client can link straight to it
 * @param status the whole submission: accepted only when every case passed
 * @param passed how many of {@code total} cases matched their expected output
 * @param hiddenTotal how many of the cases were hidden — shown as "n of m
 *     hidden cases", which is the honest way to explain a verdict the solver
 *     cannot reproduce from the samples
 * @param firstAccepted true when this is the first time the caller has solved
 *     the problem, so the UI can mark it newly solved rather than re-solved
 * @param results one entry per case in judging order; hidden ones carry a
 *     verdict and nothing else
 */
public record SubmissionResultResponse(
        Long submissionId,
        SubmissionStatus status,
        String compileOutput,
        String failureMessage,
        int passed,
        int total,
        int hiddenTotal,
        Integer runtimeMs,
        Integer memoryKb,
        boolean firstAccepted,
        List<CaseResultResponse> results) {}
