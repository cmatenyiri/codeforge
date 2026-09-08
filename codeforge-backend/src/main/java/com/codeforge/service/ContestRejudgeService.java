package com.codeforge.service;

import com.codeforge.domain.ContestProblem;
import com.codeforge.domain.ProblemSnapshot;
import com.codeforge.service.SubmissionService.RejudgeItem;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

/**
 * Re-running a finished contest against corrected test cases.
 *
 * <p>What this is for: a contest is judged against problems frozen the instant
 * it started, which is what makes it fair — but it also means a mistake in those
 * problems is frozen along with them. A hidden case with a wrong expected output
 * rejects every correct answer to that question for ninety minutes, and the
 * standings that come out are a measurement of who happened to match the bug.
 *
 * <p>Fixing the problem afterwards changes nothing on its own, precisely because
 * the contest stopped reading the live catalogue. A rejudge is the deliberate
 * act of pointing the frozen copy back at the corrected one and asking what the
 * result should have been:
 *
 * <ol>
 *   <li>Re-freeze the questions from the live catalogue — the one operation
 *       allowed to overwrite a sealed snapshot.
 *   <li>Re-run every submission that counted, in the order it was made, and
 *       write the new verdict over the old one.
 *   <li>Rebuild the standings from those verdicts.
 *   <li>If the contest had already been rated, replay the rating ledger from it
 *       forward.
 * </ol>
 *
 * <p>None of that fits in a request. A contest with two thousand competitors is
 * tens of thousands of sandbox runs, so this hands back immediately and reports
 * progress on the contest row for the authoring screen to poll.
 *
 * <h2>Transactions</h2>
 *
 * <p>There is no transaction spanning the loop, and there must not be: a
 * sandbox run takes seconds, and a connection held across thousands of them
 * would exhaust the pool for everybody else. Each step is its own short write on
 * {@link ContestAuthoringService} or {@link SubmissionService} — separate beans,
 * which is also what makes those {@code @Transactional} boundaries real rather
 * than self-calls that never reach the proxy.
 */
@Service
@RequiredArgsConstructor
public class ContestRejudgeService {

    private static final Logger log = LoggerFactory.getLogger(ContestRejudgeService.class);

    /** How often progress is written back; often enough to watch, rarely enough not to matter. */
    private static final int PROGRESS_EVERY = 10;

    /** Enough of a failure to act on, short enough for the column it is stored in. */
    private static final int MAX_ERROR_LENGTH = 500;

    private final ContestAuthoringService authoringService;
    private final SubmissionService submissionService;
    private final ExecutionService executionService;

    /**
     * Re-judges a contest in the background.
     *
     * <p>Returns as soon as the job is queued. The caller has already marked the
     * contest as rejudging, so a screen that polls immediately sees RUNNING
     * rather than a state that looks like nothing happened.
     */
    @Async("contestTaskExecutor")
    @PreAuthorize("hasRole('ADMIN')")
    public void rejudge(Long contestId) {
        try {
            List<Long> submissionIds = authoringService.resealAndCollect(contestId);
            authoringService.markRejudgeRunning(contestId, submissionIds.size());

            Map<Long, ProblemSnapshot> snapshots = authoringService.problemsOf(contestId).stream()
                    .collect(Collectors.toMap(ContestProblem::getId, ContestProblem::getSnapshot));

            int done = 0;
            for (Long submissionId : submissionIds) {
                rejudgeOne(submissionId, snapshots);
                done++;

                if (done % PROGRESS_EVERY == 0) {
                    authoringService.markRejudgeProgress(contestId, done);
                }
            }
            authoringService.markRejudgeProgress(contestId, done);

            authoringService.settleAfterRejudge(contestId);
            authoringService.markRejudgeFinished(contestId, null);

            log.info("Rejudged contest {}: {} submissions", contestId, submissionIds.size());
        } catch (RuntimeException e) {
            log.error("Rejudge failed for contest {}", contestId, e);
            authoringService.markRejudgeFinished(contestId, truncate(String.valueOf(e.getMessage())));
        }
    }

    /**
     * Re-runs one submission and stores the verdict.
     *
     * <p>A submission whose question has since been removed from the contest is
     * skipped rather than failed: it is still a real submission, but there is no
     * longer anything for it to be judged against, and the standings rebuild
     * ignores it for the same reason.
     */
    private void rejudgeOne(Long submissionId, Map<Long, ProblemSnapshot> snapshots) {
        RejudgeItem item = submissionService.loadForRejudge(submissionId);
        ProblemSnapshot snapshot = item.contestProblemId() == null ? null : snapshots.get(item.contestProblemId());

        if (snapshot == null) {
            return;
        }

        submissionService.applyRejudgedVerdict(
                submissionId,
                executionService.judgeSnapshot(snapshot, item.language(), item.sourceCode()));
    }

    private static String truncate(String value) {
        return value.length() <= MAX_ERROR_LENGTH ? value : value.substring(0, MAX_ERROR_LENGTH);
    }
}
