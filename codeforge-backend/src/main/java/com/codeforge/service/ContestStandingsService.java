package com.codeforge.service;

import com.codeforge.domain.Contest;
import com.codeforge.domain.ContestParticipation;
import com.codeforge.domain.ContestParticipationProblem;
import com.codeforge.domain.ContestProblem;
import com.codeforge.domain.Submission;
import com.codeforge.domain.SubmissionStatus;
import com.codeforge.repository.ContestParticipationRepository;
import com.codeforge.repository.ContestProblemRepository;
import com.codeforge.repository.SubmissionRepository;
import com.codeforge.repository.UserRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The scoreboard: turning submissions into a score, and scores into places.
 *
 * <p>Two ways in, and they have to agree exactly:
 *
 * <ul>
 *   <li>{@link #recordSubmission} updates one competitor's row as each verdict
 *       lands, which is what makes the leaderboard live.
 *   <li>{@link #recomputeStandings} throws all of that away and rebuilds every
 *       row from the submissions, which is what a rejudge needs.
 * </ul>
 *
 * <p>They agree because the second is the definition and the first is an
 * increment of it: both apply {@link #recompute}, and neither ever adjusts a
 * total in place. A running penalty that were incremented on each wrong answer
 * would be unrepairable — a rejudge turning a rejection into an acceptance would
 * have to know how much to take back — whereas a total re-derived from the
 * per-problem rows is right the first time and right again afterwards.
 *
 * <h2>Ranks</h2>
 *
 * <p>Assigned by the standard competition rule: your rank is one more than the
 * number of people strictly ahead of you, so a tie shares a place and consumes
 * the ones behind it. They are computed on read while a contest is live — the
 * ordering is indexed and the numbering is arithmetic — and written down once at
 * the end, because from then on they stop changing and the rating pass, the
 * profile history and the standings must all quote the same number.
 */
@Service
@RequiredArgsConstructor
public class ContestStandingsService {

    private final ContestParticipationRepository participationRepository;
    private final ContestProblemRepository contestProblemRepository;
    private final SubmissionRepository submissionRepository;
    private final UserRepository userRepository;

    // ── Recording ─────────────────────────────────────────────────────────

    /**
     * Attributes one judged submission to its competitor's scoreboard row.
     *
     * <p>Creating the participation on the first submission rather than at
     * registration is what keeps the standings a list of people who actually
     * turned up — see {@link com.codeforge.domain.ContestRegistration}.
     *
     * @param secondsIntoContest when it was submitted, measured from the start;
     *     the caller supplies it because the same clock reading has to decide
     *     both whether the submission counted and, if it did, at what time
     */
    @Transactional
    public void recordSubmission(
            Contest contest,
            ContestProblem contestProblem,
            Long userId,
            Long submissionId,
            boolean accepted,
            long secondsIntoContest) {

        // The row is already there: opening a running contest's problems requires
        // registering, and registering is what creates it. The fallback is a
        // belt-and-braces for a contest that ended and is being rebuilt, where
        // the entry may since have been removed.
        ContestParticipation participation = participationRepository
                .findWithProblems(contest.getId(), userId)
                .orElseGet(() -> {
                    ContestParticipation created = new ContestParticipation();
                    created.setContest(contest);
                    created.setUser(userRepository.getReferenceById(userId));
                    created.setRegisteredAt(Instant.now());
                    return participationRepository.save(created);
                });

        ContestParticipationProblem row = participation
                .problemAt(contestProblem.getPosition())
                .orElseGet(() -> {
                    ContestParticipationProblem created = new ContestParticipationProblem();
                    created.setParticipation(participation);
                    created.setContestProblem(contestProblem);
                    created.setPosition(contestProblem.getPosition());
                    participation.getProblems().add(created);
                    return created;
                });

        participation.setSubmissionCount(participation.getSubmissionCount() + 1);
        row.setAttempts(row.getAttempts() + 1);

        // Everything after a solve is free. The score is settled, and somebody
        // spending their last ten minutes on a tidier solution must not be
        // charged penalty for a problem they have already finished.
        if (!row.isSolved()) {
            if (accepted) {
                row.setSolved(true);
                row.setSolvedAtSeconds(secondsIntoContest);
                row.setSolvedBySubmission(submissionRepository.getReferenceById(submissionId));
            } else {
                row.setWrongAttempts(row.getWrongAttempts() + 1);
            }
        }

        recompute(participation, pointsByPosition(contest));
    }

    /**
     * Re-derives a competitor's three totals from their per-problem rows.
     *
     * <p>The single definition of what a contest score is, called by both the
     * live path and the rebuild. Note that the finish time is the latest solve
     * and not the sum of the per-problem times: a contest is ninety minutes of
     * wall clock, and what is being measured is the moment you were done.
     */
    private static void recompute(ContestParticipation participation, Map<Integer, Integer> points) {
        int score = 0;
        long finish = 0;
        long penalty = 0;

        for (ContestParticipationProblem row : participation.getProblems()) {
            if (!row.isSolved()) {
                continue;
            }
            score += points.getOrDefault(row.getPosition(), 0);
            finish = Math.max(finish, row.getSolvedAtSeconds() == null ? 0 : row.getSolvedAtSeconds());
            penalty += row.penaltySeconds();
        }

        participation.setScore(score);
        participation.setFinishSeconds(finish);
        participation.setPenaltySeconds(penalty);
        participation.refreshTotalTime();
    }

    private static Map<Integer, Integer> pointsByPosition(Contest contest) {
        return contest.getProblems().stream()
                .collect(Collectors.toMap(ContestProblem::getPosition, ContestProblem::getPoints));
    }

    // ── Rebuilding ────────────────────────────────────────────────────────

    /**
     * Rebuilds every row of a contest's standings from its submissions.
     *
     * <p>What a rejudge calls once it has re-run the verdicts. The submissions
     * are replayed oldest first because the penalty depends on the order — a
     * rejection only costs anything if it came before the acceptance — so
     * replaying them in any other order would score the same set of submissions
     * differently.
     *
     * <p>The old rows are deleted rather than adjusted. A rejudge can turn an
     * acceptance into a rejection as easily as the other way round, and there is
     * no sequence of increments that repairs that; rebuilding is both simpler and
     * the only version that is obviously correct.
     */
    @Transactional
    public void recomputeStandings(Contest contest) {
        // Only the rows of people who competed are rebuilt. An entry with no
        // submissions carries nothing a rejudge could change, and deleting it
        // would quietly un-register somebody who is owed a last place and a
        // rating drop for not turning up.
        participationRepository.deleteByContestIdAndSubmissionCountGreaterThan(contest.getId(), 0);
        // Forced out before the inserts below, which reuse the same
        // (contest, user) unique key.
        participationRepository.flush();

        Map<Long, ContestProblem> problemsById = contestProblemRepository
                .findByContestIdOrderByPositionAsc(contest.getId())
                .stream()
                .collect(Collectors.toMap(ContestProblem::getId, problem -> problem));
        Map<Integer, Integer> points = pointsByPosition(contest);

        Map<Long, ContestParticipation> byUser = new java.util.LinkedHashMap<>();

        for (Submission submission : submissionRepository.findContestSubmissions(contest.getId())) {
            ContestProblem contestProblem = problemsById.get(submission.getContestProblem().getId());
            if (contestProblem == null) {
                // The question was removed from the contest after the fact. Its
                // submissions are still real submissions, but there is no longer
                // anything for them to score against.
                continue;
            }

            Long userId = submission.getUser().getId();
            ContestParticipation participation = byUser.computeIfAbsent(userId, id -> {
                ContestParticipation created = new ContestParticipation();
                created.setContest(contest);
                created.setUser(submission.getUser());
                created.setRegisteredAt(contest.getStartsAt());
                return created;
            });

            ContestParticipationProblem row = participation
                    .problemAt(contestProblem.getPosition())
                    .orElseGet(() -> {
                        ContestParticipationProblem created = new ContestParticipationProblem();
                        created.setParticipation(participation);
                        created.setContestProblem(contestProblem);
                        created.setPosition(contestProblem.getPosition());
                        participation.getProblems().add(created);
                        return created;
                    });

            participation.setSubmissionCount(participation.getSubmissionCount() + 1);
            row.setAttempts(row.getAttempts() + 1);

            if (!row.isSolved()) {
                if (submission.getStatus() == SubmissionStatus.ACCEPTED) {
                    row.setSolved(true);
                    // The moment it was sent, read off the row, not the moment
                    // the judge finished with it — so a rebuild reproduces the
                    // finish times the live scoreboard showed rather than
                    // shifting them all by the judging latency.
                    row.setSolvedAtSeconds(
                            submission.getContestSeconds() != null
                                    ? submission.getContestSeconds()
                                    : contest.secondsIntoContest(submission.getCreatedAt()));
                    row.setSolvedBySubmission(submission);
                } else {
                    row.setWrongAttempts(row.getWrongAttempts() + 1);
                }
            }
        }

        byUser.values().forEach(participation -> recompute(participation, points));
        participationRepository.saveAll(byUser.values());

        assignRanks(contest);
    }

    /**
     * Writes the final places onto the rows.
     *
     * <p>Called once when a contest settles, and again after any rejudge. Until
     * then ranks are derived on read — see {@link #standings} — because they
     * change with every acceptance and storing them would mean a write to the
     * whole table on each one.
     */
    @Transactional
    public void assignRanks(Contest contest) {
        List<ContestParticipation> ranked = participationRepository.findAllRanked(contest.getId());

        // Score and finish time are the whole key — nothing else breaks a tie.
        // A no-show and somebody who submitted and scored nothing therefore share
        // a rank, because they share a score of 0 and a finish of 0:00.
        //
        // It is tempting to rank the one who tried above the one who did not, and
        // that is deliberately not done: equal results earn equal places, and the
        // deterrent against sitting a contest out is that an absence costs rating
        // at all, not that it costs a place more than failing does.
        int rank = 0;
        Integer previousScore = null;
        Long previousTime = null;

        for (int index = 0; index < ranked.size(); index++) {
            ContestParticipation participation = ranked.get(index);
            boolean tied = previousScore != null
                    && previousScore.intValue() == participation.getScore()
                    && previousTime.longValue() == participation.getTotalTimeSeconds();

            if (!tied) {
                rank = index + 1;
                previousScore = participation.getScore();
                previousTime = participation.getTotalTimeSeconds();
            }
            participation.setRank(rank);
        }
    }

    // ── Reading ───────────────────────────────────────────────────────────

    /**
     * A page of the standings, with the places filled in.
     *
     * <p>Ranks are taken from the stored column once a contest has settled and
     * derived from the ordering while it is still live. The derivation costs one
     * extra count for the top of the page — the tie a page starts in the middle
     * of may have begun several pages earlier, and only a count over the whole
     * table knows how far back.
     */
    @Transactional(readOnly = true)
    public Page<ContestParticipation> standings(Contest contest, Pageable pageable) {
        // While a contest is live the people who have not submitted yet are not
        // news — a board of five hundred registrants on zero buries the dozen
        // who are actually racing. Once it is over they belong there: a no-show
        // is a result, and it is the one the rating pass acts on.
        boolean includeAbsent = contest.hasEnded(Instant.now());
        Page<ContestParticipation> page =
                participationRepository.findStandings(contest.getId(), includeAbsent, pageable);
        List<ContestParticipation> rows = page.getContent();

        if (rows.isEmpty() || rows.getFirst().getRank() != null) {
            return page;
        }

        long offset = pageable.getOffset();
        int rank = (int) (participationRepository.countAhead(
                        contest.getId(), rows.getFirst().getScore(), rows.getFirst().getTotalTimeSeconds())
                + 1);

        for (int index = 0; index < rows.size(); index++) {
            ContestParticipation participation = rows.get(index);
            if (index > 0) {
                ContestParticipation previous = rows.get(index - 1);
                boolean tied = previous.getScore() == participation.getScore()
                        && previous.getTotalTimeSeconds() == participation.getTotalTimeSeconds();
                rank = tied ? rank : (int) (offset + index + 1);
            }
            // Set on the loaded entity but never flushed: this runs in a
            // read-only transaction, so it is a decoration for the mapper rather
            // than a write.
            participation.setRank(rank);
        }

        return page;
    }

    /** One competitor's row, for the banner above the standings and for their profile. */
    @Transactional(readOnly = true)
    public Optional<ContestParticipation> participationOf(Long contestId, Long userId) {
        return participationRepository.findWithProblems(contestId, userId).map(participation -> {
            if (participation.getRank() == null) {
                participation.setRank((int) (participationRepository.countAhead(
                                contestId, participation.getScore(), participation.getTotalTimeSeconds())
                        + 1));
            }
            return participation;
        });
    }

    @Transactional(readOnly = true)
    public long participantCount(Long contestId) {
        return participationRepository.countByContestId(contestId);
    }

    /** The per-problem solve counts under the standings: how hard each question turned out to be. */
    @Transactional(readOnly = true)
    public List<Integer> solveCounts(Contest contest) {
        List<Integer> counts = new ArrayList<>();
        List<ContestParticipation> all = participationRepository.findAllRanked(contest.getId());

        for (ContestProblem problem : contest.getProblems()) {
            counts.add((int) all.stream()
                    .filter(participation -> participation
                            .problemAt(problem.getPosition())
                            .map(ContestParticipationProblem::isSolved)
                            .orElse(false))
                    .count());
        }

        return counts;
    }
}
