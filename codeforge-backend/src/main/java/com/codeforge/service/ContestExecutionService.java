package com.codeforge.service;

import com.codeforge.domain.Language;
import com.codeforge.domain.SubmissionStatus;
import com.codeforge.service.ContestService.JudgingTarget;
import com.codeforge.web.dto.execution.RunResponse;
import com.codeforge.web.dto.submission.SubmissionResultResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

/**
 * Running and submitting from inside a contest.
 *
 * <p>Its own bean, and pointedly not {@code @Transactional}, for the same reason
 * {@link ExecutionService} is separate from {@link SubmissionService}: judging
 * takes seconds in a sandbox, and no database connection may be held across it.
 * The two short writes that bracket it — checking the clock, attributing the
 * verdict — are separate transactions on {@link ContestService}, which is also
 * what makes their {@code @Transactional} boundaries real rather than self-calls
 * that never reach the proxy.
 *
 * <p>What is judged is the copy frozen when the contest started, never the live
 * catalogue row. That is the fairness guarantee the whole contest rests on: an
 * author editing a problem — rewriting a test case, renaming the function —
 * cannot reach anybody currently sitting it, and everyone in the field is
 * measured against exactly the same cases.
 *
 * <h2>Practising a finished contest</h2>
 *
 * <p>Submissions keep being accepted after the clock runs out, and are judged
 * against the same frozen cases — a finished contest's problems are the most
 * useful thing it leaves behind. They simply do not count: the attempt is
 * recorded as an ordinary submission, solves the problem in the catalogue like
 * any other, and never touches the standings. Deciding that at submission time
 * rather than inferring it later is what lets a rejudge months afterwards reach
 * the same answer about which attempts were in the contest and which were not.
 */
@Service
@RequiredArgsConstructor
public class ContestExecutionService {

    private final ContestService contestService;
    private final ExecutionService executionService;

    /** Sample cases only, nothing recorded — the fast loop, inside the clock. */
    @PreAuthorize("isAuthenticated()")
    public RunResponse run(String slug, int position, Language language, String sourceCode) {
        JudgingTarget target = contestService.requireJudgingTarget(slug, position);

        return executionService.runSnapshot(target.snapshot(), language, sourceCode);
    }

    /**
     * Judges every case, records the attempt, and scores it if the contest was
     * still running when it was sent.
     */
    @PreAuthorize("isAuthenticated()")
    public SubmissionResultResponse submit(
            String slug, int position, Language language, String sourceCode) {

        JudgingTarget target = contestService.requireJudgingTarget(slug, position);
        SubmissionResultResponse result =
                executionService.submitSnapshot(target.snapshot(), language, sourceCode);

        contestService.recordAttempt(
                target, result.submissionId(), result.status() == SubmissionStatus.ACCEPTED);

        return result;
    }
}
