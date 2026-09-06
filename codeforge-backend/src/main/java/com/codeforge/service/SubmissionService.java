package com.codeforge.service;

import com.codeforge.domain.Difficulty;
import com.codeforge.domain.Language;
import com.codeforge.domain.Problem;
import com.codeforge.domain.Submission;
import com.codeforge.domain.SubmissionStatus;
import com.codeforge.exception.NotFoundException;
import com.codeforge.repository.ProblemRepository;
import com.codeforge.repository.SubmissionRepository;
import com.codeforge.repository.UserRepository;
import com.codeforge.security.SecurityUtils;
import com.codeforge.web.dto.user.UserStatsResponse;
import com.codeforge.web.dto.user.UserStatsResponse.DifficultyProgress;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Owns everything that has been submitted: recording a judged attempt, and
 * reading a user's history back.
 *
 * <p>Separate from {@link ExecutionService} because the two have opposite
 * transaction needs. Judging holds no database connection — a sandbox takes
 * seconds, and a pool cannot afford that — while recording the verdict is a
 * short write. Keeping them in different beans is also what makes the
 * {@code @Transactional} boundary here real: a self-call inside one bean would
 * never pass through the proxy that starts it.
 */
@Service
@RequiredArgsConstructor
public class SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final ProblemRepository problemRepository;
    private final UserRepository userRepository;

    /**
     * Writes a judged attempt to the caller's history.
     *
     * @return the saved submission's id and whether it was the caller's first
     *     acceptance of this problem — asked before the insert, because
     *     afterwards every accepted submission looks like a repeat
     */
    @Transactional
    @PreAuthorize("isAuthenticated()")
    public Recorded record(NewSubmission attempt) {
        Long userId = SecurityUtils.requireCurrentUserId();
        boolean accepted = attempt.status() == SubmissionStatus.ACCEPTED;
        boolean alreadySolved =
                submissionRepository.existsByUserIdAndProblemIdAndStatus(
                        userId, attempt.problemId(), SubmissionStatus.ACCEPTED);

        Submission submission = new Submission();
        submission.setUser(userRepository.getReferenceById(userId));
        submission.setProblem(problemRepository.getReferenceById(attempt.problemId()));
        submission.setLanguage(attempt.language());
        submission.setSourceCode(attempt.sourceCode());
        submission.setStatus(attempt.status());
        submission.setRuntimeMs(attempt.runtimeMs());
        submission.setMemoryKb(attempt.memoryKb());
        submission.setPassedTests(attempt.passed());
        submission.setTotalTests(attempt.total());
        submission.setFailureMessage(attempt.failureMessage());

        Submission saved = submissionRepository.save(submission);
        problemRepository.recordSubmissionOutcome(attempt.problemId(), accepted ? 1 : 0);

        return new Recorded(saved.getId(), accepted && !alreadySolved);
    }

    /** The caller's whole history, newest first. */
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public Page<Submission> listMine(Pageable pageable) {
        return submissionRepository.findForUser(SecurityUtils.requireCurrentUserId(), pageable);
    }

    /** The caller's history for one problem — the editor's "Submissions" tab. */
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public Page<Submission> listMineForProblem(String slug, Pageable pageable) {
        return submissionRepository.findForUserAndProblem(SecurityUtils.requireCurrentUserId(), slug, pageable);
    }

    /**
     * One submission of the caller's, with its source.
     *
     * <p>Scoped to the owner in the query rather than checked after loading, so
     * someone else's id is a 404 and not a 403 — which would confirm it exists.
     */
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public Submission getMine(Long id) {
        return submissionRepository
                .findOwned(id, SecurityUtils.requireCurrentUserId())
                .orElseThrow(() -> NotFoundException.of("submission", id));
    }

    /** Solved counts and acceptance, for the dashboard and the profile. */
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public UserStatsResponse stats() {
        Long userId = SecurityUtils.requireCurrentUserId();

        Map<Difficulty, Long> catalogue = byDifficulty(problemRepository.countByDifficulty());
        Map<Difficulty, Long> solved = byDifficulty(submissionRepository.countSolvedByDifficulty(userId));

        List<DifficultyProgress> progress = Arrays.stream(Difficulty.values())
                .map(difficulty -> new DifficultyProgress(
                        difficulty,
                        solved.getOrDefault(difficulty, 0L),
                        catalogue.getOrDefault(difficulty, 0L)))
                .toList();

        long submissions = submissionRepository.countByUserId(userId);
        long accepted = submissionRepository.countAcceptedByUserId(userId);

        return new UserStatsResponse(
                progress.stream().mapToLong(DifficultyProgress::solved).sum(),
                progress.stream().mapToLong(DifficultyProgress::total).sum(),
                submissions,
                accepted,
                submissions == 0 ? null : (double) accepted / submissions,
                progress);
    }

    private static Map<Difficulty, Long> byDifficulty(List<ProblemRepository.DifficultyTotal> rows) {
        return rows.stream()
                .collect(Collectors.toMap(
                        ProblemRepository.DifficultyTotal::getDifficulty,
                        ProblemRepository.DifficultyTotal::getTotal,
                        Long::sum,
                        () -> new EnumMap<>(Difficulty.class)));
    }

    /** A judged attempt, ready to be written down. */
    public record NewSubmission(
            Long problemId,
            Language language,
            String sourceCode,
            SubmissionStatus status,
            int passed,
            int total,
            Integer runtimeMs,
            Integer memoryKb,
            String failureMessage) {}

    /** @param firstAccepted true only for the submission that first solved the problem */
    public record Recorded(Long submissionId, boolean firstAccepted) {}
}
