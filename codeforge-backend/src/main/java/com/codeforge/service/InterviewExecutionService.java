package com.codeforge.service;

import com.codeforge.domain.Language;
import com.codeforge.domain.SubmissionStatus;
import com.codeforge.web.dto.execution.RunResponse;
import com.codeforge.web.dto.submission.SubmissionResultResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

/**
 * Running and submitting from inside an interview.
 *
 * <p>Its own bean, and pointedly not {@code @Transactional}, for the same reason
 * {@link ExecutionService} is separate from {@link SubmissionService}: judging
 * takes seconds in a sandbox, and no database connection may be held across it.
 * The two short writes that bracket it — checking the clock, attributing the
 * verdict — are separate transactions on {@link InterviewService}, which is also
 * what makes their {@code @Transactional} boundaries real rather than
 * self-calls that never reach the proxy.
 *
 * <p>The submission itself is recorded exactly like any other. An interview is a
 * different way to be handed a problem, not a different kind of solving: the
 * attempt belongs in the history, and an accepted one genuinely solves the
 * problem in the catalogue.
 */
@Service
@RequiredArgsConstructor
public class InterviewExecutionService {

    private final InterviewService interviewService;
    private final ExecutionService executionService;

    /** Sample cases only, nothing recorded — the fast loop, inside the clock. */
    @PreAuthorize("isAuthenticated()")
    public RunResponse run(Long interviewId, int position, Language language, String sourceCode) {
        String slug = interviewService.requireRunningSlug(interviewId, position);

        return executionService.run(slug, language, sourceCode);
    }

    /**
     * Judges every case and attributes the verdict to the slot.
     *
     * <p>The clock is checked once, before judging. A submission started with ten
     * seconds left counts even though the judge hands its verdict back a minute
     * after the buzzer — the alternative punishes a candidate for the sandbox
     * being slow. {@code recordAttempt} closes the round straight afterwards, so
     * that grace is worth exactly one submission.
     */
    @PreAuthorize("isAuthenticated()")
    public SubmissionResultResponse submit(
            Long interviewId, int position, Language language, String sourceCode) {

        String slug = interviewService.requireRunningSlug(interviewId, position);
        SubmissionResultResponse result = executionService.submit(slug, language, sourceCode);

        interviewService.recordAttempt(
                interviewId, position, result.submissionId(), result.status() == SubmissionStatus.ACCEPTED);

        return result;
    }
}
