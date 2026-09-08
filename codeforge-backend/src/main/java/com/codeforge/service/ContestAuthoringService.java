package com.codeforge.service;

import com.codeforge.domain.Contest;
import com.codeforge.domain.ContestProblem;
import com.codeforge.domain.ContestType;
import com.codeforge.domain.RejudgeState;
import com.codeforge.domain.Slugs;
import com.codeforge.exception.BusinessRuleException;
import com.codeforge.exception.NotFoundException;
import com.codeforge.repository.ContestParticipationRepository;
import com.codeforge.repository.ContestProblemRepository;
import com.codeforge.repository.ContestRegistrationRepository;
import com.codeforge.repository.ContestRepository;
import com.codeforge.repository.ProblemRepository;
import com.codeforge.repository.SubmissionRepository;
import com.codeforge.repository.UserRepository;
import com.codeforge.security.SecurityUtils;
import com.codeforge.validation.ContestUpsertRequestValidator;
import com.codeforge.validation.ValidationRules;
import com.codeforge.web.dto.contest.ContestProblemPayload;
import com.codeforge.web.dto.contest.ContestUpsertRequest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Writing contests, announcing them, and settling them afterwards.
 *
 * <p>Split from {@link ContestService} the same way problem authoring is split
 * from the catalogue: the two have opposite defaults about what may be seen and
 * what may be changed, and a single service with an "am I an admin" branch
 * running through it is one mistake away from letting a competitor edit the
 * contest they are sitting.
 *
 * <h2>What can still be changed, and when</h2>
 *
 * <p>Everything, until the contest starts. After that the questions and the
 * clock are settled — the validator refuses them outright rather than ignoring
 * them — and what is left are the three deliberate acts of repair:
 *
 * <ul>
 *   <li>{@link #setRated} withdraws the contest's effect on everybody's rating,
 *       for when the standings turn out not to have measured anything.
 *   <li>{@link ContestRejudgeService} re-runs every submission against corrected
 *       test cases and rebuilds the standings from what they should have said.
 *   <li>{@link #applyRatings} settles a contest whose result is trusted.
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class ContestAuthoringService {

    private final ContestRepository contestRepository;
    private final ContestRegistrationRepository registrationRepository;
    private final ContestParticipationRepository participationRepository;
    private final ContestProblemRepository contestProblemRepository;
    private final ProblemRepository problemRepository;
    private final SubmissionRepository submissionRepository;
    private final UserRepository userRepository;
    private final ContestService contestService;
    private final ContestStandingsService standingsService;
    private final ContestRatingService ratingService;
    private final ContestUpsertRequestValidator validator;

    // ── Reading ───────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public Page<Contest> search(String search, Pageable pageable) {
        // The questions are deliberately not loaded: each carries a frozen
        // snapshot with every test case in it, and the list only needs to say how
        // many there are — which counts() answers with one query per row instead
        // of megabytes of judge input.
        return contestRepository.searchForAuthor(search, pageable);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public Contest get(Long id) {
        Contest contest = contestRepository
                .findWithProblems(id)
                .orElseThrow(() -> NotFoundException.of("contest", id));

        contest.getProblems().forEach(slot -> slot.getProblem().getTestCases().size());

        return contest;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public Counts counts(Long contestId) {
        return new Counts(
                registrationRepository.countByContestId(contestId),
                participationRepository.countByContestId(contestId),
                contestProblemRepository.countByContestId(contestId));
    }

    // ── Writing ───────────────────────────────────────────────────────────

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public Long create(ContestUpsertRequest request) {
        validator.validate(request, Optional.empty());

        Contest contest = new Contest();
        contest.setCreatedBy(userRepository.getReferenceById(SecurityUtils.requireCurrentUserId()));
        apply(contest, request);

        return contestRepository.save(contest).getId();
    }

    /**
     * Replaces the contest with the document the form sent.
     *
     * <p>The questions are rebuilt from scratch each time rather than diffed.
     * They are a short ordered list whose identity is entirely positional, and a
     * diff would exist only to preserve row ids that nothing refers to — while a
     * sealed contest cannot reach this code at all, because the validator has
     * already refused any change to them.
     */
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void update(Long id, ContestUpsertRequest request) {
        Contest contest = contestRepository
                .findWithProblems(id)
                .orElseThrow(() -> NotFoundException.of("contest", id));

        validator.validate(request, Optional.of(contest));
        apply(contest, request);
    }

    private void apply(Contest contest, ContestUpsertRequest request) {
        contest.setTitle(ValidationRules.trimToNull(request.title()));
        contest.setSlug(slugOf(request));
        contest.setDescription(ValidationRules.trimToNull(request.description()));
        contest.setType(request.type() == null ? ContestType.WEEKLY : request.type());
        contest.setStartsAt(request.startsAt());
        contest.setDurationMinutes(request.durationMinutes());
        contest.setPublished(request.published());

        // The rated flag is only writable here while the contest is still ahead
        // of us. Once it has run, changing it moves real ratings, and that goes
        // through setRated so the ledger is replayed rather than silently
        // contradicted.
        if (!contest.isSealed()) {
            contest.setRated(request.rated());
            if (request.rated()) {
                contest.setUnratedReason(null);
            }
        }

        List<ContestProblemPayload> payloads = request.problems() == null ? List.of() : request.problems();
        if (!contest.isSealed()) {
            rebuildProblems(contest, payloads);
        } else {
            // Points remain editable: they rescale the standings rather than
            // change what anybody solved, and correcting a mis-weighted question
            // is exactly what a rejudge is then for.
            for (int position = 0; position < payloads.size() && position < contest.getProblems().size(); position++) {
                Integer points = payloads.get(position).points();
                if (points != null) {
                    contest.getProblems().get(position).setPoints(points);
                }
            }
        }
    }

    private void rebuildProblems(Contest contest, List<ContestProblemPayload> payloads) {
        contest.getProblems().clear();

        for (int position = 0; position < payloads.size(); position++) {
            ContestProblemPayload payload = payloads.get(position);

            ContestProblem slot = new ContestProblem();
            slot.setContest(contest);
            slot.setProblem(problemRepository.getReferenceById(payload.problemId()));
            slot.setPosition(position);
            slot.setPoints(
                    payload.points() == null ? ContestProblem.defaultPointsFor(position) : payload.points());
            contest.getProblems().add(slot);
        }

        // A snapshot straight away, even though the one that will actually be
        // used is re-taken when the contest seals. It keeps the column non-null,
        // and it is what lets an author preview the contest exactly as a
        // competitor will see it.
        contestService.snapshotProblems(contest);
    }

    private static String slugOf(ContestUpsertRequest request) {
        String slug = ValidationRules.trimToNull(request.slug());
        return slug != null ? slug : Slugs.slugify(request.title());
    }

    /**
     * Announces a contest, or takes the announcement back.
     *
     * <p>Unannouncing is refused once it has started. Hiding a contest people are
     * sitting — or have already sat, and been rated on — would not undo any of
     * it; it would only make the result unreadable to the people it happened to.
     */
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void setPublished(Long id, boolean published) {
        Contest contest = get(id);

        if (!published && contest.hasStarted(Instant.now())) {
            throw new BusinessRuleException(
                    "error.contest.started", "A contest that has started cannot be unannounced");
        }
        contest.setPublished(published);
    }

    /**
     * Deletes a contest.
     *
     * <p>Refused once anybody has competed in it. A finished contest is part of
     * their history and, if it was rated, part of the arithmetic behind their
     * current rating — deleting it would leave a rating nothing explains. The
     * reversible alternative is to withdraw the rating and say why.
     */
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(Long id) {
        Contest contest = get(id);

        if (participationRepository.countByContestId(id) > 0) {
            throw new BusinessRuleException(
                    "error.contest.hasParticipants", "People have competed in this contest");
        }
        contestRepository.delete(contest);
    }

    // ── Settling ──────────────────────────────────────────────────────────

    /**
     * Freezes the places and moves everybody's rating.
     *
     * <p>Deliberately a decision rather than a timer. The window between a
     * contest ending and its ratings landing is when a broken test case gets
     * found, and it is far cheaper to rejudge a contest that has not been rated
     * than one that has — so a human confirms the result is worth acting on.
     *
     * @return how many competitors were rated; zero for an unrated contest
     */
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public int applyRatings(Long id) {
        Contest contest = get(id);
        Instant now = Instant.now();

        if (!contest.hasEnded(now)) {
            throw new BusinessRuleException("error.contest.notEnded", "That contest is still running");
        }

        // The places have to be settled before anything reads them: until now
        // they have been derived on each read of the standings, and the rating
        // pass needs a number that will not move again.
        standingsService.assignRanks(contest);

        if (!contest.isRated()) {
            return 0;
        }
        return ratingService.applyRatings(contest);
    }

    /**
     * Withdraws a contest's rating, or restores it.
     *
     * <p>The repair for a contest that turned out not to measure anything — a
     * test case that rejected correct answers, a statement nobody could read the
     * same way twice. Both directions go through the ledger replay in
     * {@link ContestRatingService#replayFrom}, because a rating is a running
     * total: every contest since was computed on top of this one, so changing it
     * means recomputing them, not adjusting a number.
     */
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void setRated(Long id, boolean rated, String reason) {
        Contest contest = get(id);

        if (contest.isRated() == rated) {
            if (!rated) {
                contest.setUnratedReason(ValidationRules.trimToNull(reason));
            }
            return;
        }

        contest.setRated(rated);
        contest.setUnratedReason(rated ? null : ValidationRules.trimToNull(reason));

        if (contest.hasEnded(Instant.now())) {
            // Only a contest that has already run has anything in the ledger to
            // withdraw, or anything settled enough to rate.
            ratingService.replayFrom(contest.getStartsAt());
        }
    }

    // ── Rejudge bookkeeping ───────────────────────────────────────────────
    // Small transactional writes, called by ContestRejudgeService from a
    // background thread. They live here rather than there because a rejudge must
    // hold no transaction across the sandbox, and a @Transactional method called
    // from inside its own bean would never reach the proxy that starts one.

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void markRejudgeRunning(Long contestId, int total) {
        Contest contest = contestRepository
                .findById(contestId)
                .orElseThrow(() -> NotFoundException.of("contest", contestId));

        contest.setRejudgeState(RejudgeState.RUNNING);
        contest.setRejudgeStartedAt(Instant.now());
        contest.setRejudgeFinishedAt(null);
        contest.setRejudgeTotal(total);
        contest.setRejudgeDone(0);
        contest.setRejudgeError(null);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void markRejudgeProgress(Long contestId, int done) {
        contestRepository.findById(contestId).ifPresent(contest -> contest.setRejudgeDone(done));
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void markRejudgeFinished(Long contestId, String error) {
        contestRepository.findById(contestId).ifPresent(contest -> {
            contest.setRejudgeState(error == null ? RejudgeState.COMPLETED : RejudgeState.FAILED);
            contest.setRejudgeFinishedAt(Instant.now());
            contest.setRejudgeError(error);
        });
    }

    /**
     * Re-freezes the questions from the live catalogue, and returns what has to
     * be re-run.
     *
     * <p>The first step of a rejudge and the reason it works: the corrected test
     * cases are sitting on the live problem, and this is the one operation
     * allowed to overwrite a sealed snapshot with them.
     */
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public List<Long> resealAndCollect(Long contestId) {
        Contest contest = contestRepository
                .findWithProblems(contestId)
                .orElseThrow(() -> NotFoundException.of("contest", contestId));

        contestService.snapshotProblems(contest);

        return submissionRepository.findContestSubmissions(contestId).stream()
                .map(submission -> submission.getId())
                .toList();
    }

    /** The frozen questions, keyed by id, so a rejudge can look one up per submission. */
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public List<ContestProblem> problemsOf(Long contestId) {
        Contest contest = contestRepository
                .findWithProblems(contestId)
                .orElseThrow(() -> NotFoundException.of("contest", contestId));

        return new ArrayList<>(contest.getProblems());
    }

    /**
     * Rebuilds the standings, and the ratings if they had already been applied.
     *
     * <p>The last step of a rejudge. Ratings are only replayed when this contest
     * had already moved them — before that there is nothing in the ledger to
     * correct, which is exactly why rejudging early is so much cheaper.
     */
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void settleAfterRejudge(Long contestId) {
        Contest contest = contestRepository
                .findWithProblems(contestId)
                .orElseThrow(() -> NotFoundException.of("contest", contestId));

        standingsService.recomputeStandings(contest);

        if (contest.getRatingsAppliedAt() != null) {
            ratingService.replayFrom(contest.getStartsAt());
        }
    }

    /** Registration, participation and question totals, for a row in the authoring list. */
    public record Counts(long registrations, long participants, long problems) {}
}
