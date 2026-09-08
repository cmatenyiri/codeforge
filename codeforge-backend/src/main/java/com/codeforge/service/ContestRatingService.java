package com.codeforge.service;

import com.codeforge.domain.Contest;
import com.codeforge.domain.ContestParticipation;
import com.codeforge.domain.ContestRatingChange;
import com.codeforge.domain.User;
import com.codeforge.repository.ContestParticipationRepository;
import com.codeforge.repository.ContestRatingChangeRepository;
import com.codeforge.repository.ContestRepository;
import com.codeforge.repository.UserRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The contest rating: computing it, applying it, and taking it back.
 *
 * <h2>The algorithm</h2>
 *
 * <p>Elo, adapted from a two-player game to a field of thousands. Ordinary Elo
 * asks "did you beat this one opponent?"; a contest asks "where did you finish
 * among everybody?", and the adaptation is to treat a contest as every pairwise
 * match played at once.
 *
 * <p>For each competitor <i>i</i>, in four steps:
 *
 * <ol>
 *   <li><b>Expected rank.</b> Against each other competitor <i>j</i>, the
 *       probability that <i>j</i> finishes ahead is the logistic
 *       {@code 1 / (1 + 10^((R_i − R_j)/400))}. Summing that over the whole
 *       field and adding one gives the rank <i>i</i> would be expected to take —
 *       a 1500 in a field of 1500s expects the middle, and the same 1500 in a
 *       field of 2000s expects the bottom.
 *   <li><b>Blend it with what happened.</b> {@code M = sqrt(expected × actual)},
 *       the geometric mean. Not the actual rank alone: one contest is a noisy
 *       measurement, and reacting to all of it would make ratings swing on a
 *       single bad morning. The geometric mean rather than the arithmetic one
 *       because ranks are a ratio scale — finishing 10th instead of 20th is the
 *       same kind of achievement as 100th instead of 200th, and an arithmetic
 *       mean would call the second one ten times larger.
 *   <li><b>Turn that rank back into a rating.</b> The <em>seed</em>: the rating
 *       at which a competitor would have been expected to finish exactly at
 *       {@code M}. Expected rank falls monotonically as rating rises, so this
 *       inverts by binary search.
 *   <li><b>Move halfway.</b> {@code Δ = (seed − R) / 2}, damped further by how
 *       many contests the competitor has already sat.
 * </ol>
 *
 * <p>The halving and the damping are the same idea applied twice: a rating is a
 * running estimate, and no single result should be trusted enough to replace it.
 * A newcomer's first three contests move their rating at full weight, because
 * the 1500 they started on is a placeholder and carries no information; by the
 * tenth it moves at a fifth of that, because by then the number is worth more
 * than any one morning's result.
 *
 * <h2>Why every change is written down</h2>
 *
 * <p>A rating is a running total, so a mistake in one contest is baked into
 * every contest after it. That makes "this contest was wrong" — a broken test
 * case found after the fact, or an admin declaring the round unrated — a
 * question about history, not about the current number: it cannot be repaired by
 * subtracting a delta, because later contests were computed against ratings that
 * included it.
 *
 * <p>So {@link ContestRatingChange} records every movement with the rating it
 * started from, and withdrawing a contest means deleting its rows, rewinding
 * everybody it touched to where they stood before it, and replaying every rated
 * contest since in the order it happened. See {@link #replayFrom}.
 */
@Service
@RequiredArgsConstructor
public class ContestRatingService {

    /**
     * The logistic width, in rating points.
     *
     * <p>400 is Elo's own constant and means one thing: a competitor 400 points
     * above another is expected to finish ahead of them about ten times out of
     * eleven. Everything else on the scale — that 1500 is average, that the
     * spread of a mature field runs a few hundred points wide — follows from it.
     */
    private static final double SCALE = 400.0;

    /** Nobody's rating is outside this; the seed search only has to cover it. */
    private static final double MIN_RATING = 1.0;

    private static final double MAX_RATING = 10_000.0;

    /**
     * Binary search steps for the seed rating.
     *
     * <p>Sixty halvings of a ten-thousand-point interval is far below the
     * precision of anything downstream, and it is a fixed cost rather than a
     * convergence test that could fail to terminate on a pathological field.
     */
    private static final int SEED_SEARCH_STEPS = 60;

    /**
     * How much of the computed delta is applied, by contests already sat.
     *
     * <p>Indexed by the number of rated contests behind the competitor. The first
     * three land at full weight — a starting 1500 is an assumption, not a
     * measurement, and it should be replaced quickly — after which the taper
     * makes an established rating progressively harder to move.
     */
    private static final double[] ATTENDANCE_FACTORS = {1.0, 1.0, 1.0, 0.9, 0.8, 0.7, 0.6, 0.5, 0.4, 0.3};

    /** The floor the taper settles on, for everyone past the table above. */
    private static final double SETTLED_FACTOR = 0.2;

    private final ContestRepository contestRepository;
    private final ContestParticipationRepository participationRepository;
    private final ContestRatingChangeRepository ratingChangeRepository;
    private final UserRepository userRepository;

    // ── Applying ──────────────────────────────────────────────────────────

    /**
     * Scores a finished contest and moves everybody's rating.
     *
     * <p>Idempotent by refusal rather than by re-running: a contest whose ratings
     * are already applied has to be withdrawn first, because applying twice would
     * count the same result twice.
     *
     * @return how many competitors were rated
     */
    @Transactional
    public int applyRatings(Contest contest) {
        if (contest.getRatingsAppliedAt() != null) {
            return 0;
        }

        int rated = rate(contest);
        contest.setRatingsAppliedAt(Instant.now());

        return rated;
    }

    /**
     * Computes and writes the rating changes for one contest.
     *
     * <p>The step a replay repeats, and the reason it is separate from
     * {@link #applyRatings}: a replay has already decided that these contests are
     * being recomputed, and must not be stopped by the guard that keeps an
     * ordinary caller from applying twice.
     */
    private int rate(Contest contest) {
        List<ContestParticipation> ranked = participationRepository.findAllRanked(contest.getId());
        if (ranked.isEmpty()) {
            return 0;
        }

        List<Competitor> field = ranked.stream()
                .map(participation -> new Competitor(
                        participation.getUser(),
                        participation.getUser().getRating(),
                        // A rank is only null if the standings were never
                        // computed, which cannot happen for a contest reaching
                        // this method; falling back to last place keeps the
                        // arithmetic total rather than throwing.
                        participation.getRank() == null ? ranked.size() : participation.getRank()))
                .toList();

        List<Double> deltas = computeDeltas(field);
        Instant appliedAt = Instant.now();

        for (int i = 0; i < field.size(); i++) {
            Competitor competitor = field.get(i);
            User user = competitor.user();
            double before = competitor.rating();
            double after = before + deltas.get(i);

            ContestRatingChange change = new ContestRatingChange();
            change.setContest(contest);
            change.setUser(user);
            change.setRatingBefore(before);
            change.setRatingAfter(after);
            change.setDelta(after - before);
            change.setRank(competitor.rank());
            change.setParticipantCount(field.size());
            change.setAttendedBefore(user.getContestsAttended());
            change.setAppliedAt(appliedAt);
            ratingChangeRepository.save(change);

            user.setRating(after);
            user.setMaxRating(Math.max(user.getMaxRating(), after));
            user.setContestsAttended(user.getContestsAttended() + 1);
        }

        return field.size();
    }

    /**
     * The heart of it: one delta per competitor, in the order they were given.
     *
     * <p>Pure arithmetic over the field — no entities, no database — so the
     * algorithm can be read, and reasoned about, without any of the bookkeeping
     * around it.
     */
    private static List<Double> computeDeltas(List<Competitor> field) {
        // Competitors sharing a rating are indistinguishable to every sum below,
        // and at the start of a platform's life most of the field is on exactly
        // 1500. Collapsing them turns each expected-rank evaluation from a walk
        // over the whole field into a walk over its distinct ratings, which is
        // what keeps the seed search — the one part of this that runs a
        // logarithmic number of times per competitor — affordable.
        Map<Double, Integer> byRating = new LinkedHashMap<>();
        for (Competitor competitor : field) {
            byRating.merge(competitor.rating(), 1, Integer::sum);
        }
        double[] ratings = byRating.keySet().stream().mapToDouble(Double::doubleValue).toArray();
        int[] counts = byRating.values().stream().mapToInt(Integer::intValue).toArray();

        List<Double> deltas = new ArrayList<>(field.size());

        for (Competitor competitor : field) {
            // Excluding the competitor from their own field: you cannot finish
            // ahead of yourself, and including the term would add a constant ½
            // to every expected rank.
            double expected = expectedRank(competitor.rating(), ratings, counts) - selfTerm();
            double blended = Math.sqrt(expected * competitor.rank());
            double seed = seedRating(blended, ratings, counts, competitor.rating());

            double delta = (seed - competitor.rating()) / 2 * attendanceFactor(competitor.user().getContestsAttended());
            deltas.add(delta);
        }

        return deltas;
    }

    /**
     * Where a rating would be expected to finish in this field.
     *
     * <p>One plus the expected number of competitors ahead of it. Falls as the
     * rating rises, which is what lets {@link #seedRating} invert it.
     */
    private static double expectedRank(double rating, double[] ratings, int[] counts) {
        double ahead = 0;
        for (int i = 0; i < ratings.length; i++) {
            ahead += counts[i] / (1 + Math.pow(10, (rating - ratings[i]) / SCALE));
        }
        return 1 + ahead;
    }

    /**
     * A competitor's own contribution to the sum above.
     *
     * <p>Always exactly one half — the logistic of a rating against itself — and
     * named rather than written as {@code 0.5} because what makes it a half is
     * that a competitor is an even match for themselves, not a coincidence.
     */
    private static double selfTerm() {
        return 0.5;
    }

    /**
     * The rating whose expected rank in this field is {@code targetRank}.
     *
     * <p>Binary search rather than an algebraic inverse because there is none:
     * the expected rank is a sum of logistics over every distinct rating present,
     * which cannot be solved for the rating in closed form. It is monotonically
     * decreasing, though, which is all a bisection needs.
     *
     * @param ownRating excluded from the field for the same reason as above, so
     *     that the seed is measured against the same opposition the expected rank
     *     was
     */
    private static double seedRating(double targetRank, double[] ratings, int[] counts, double ownRating) {
        double low = MIN_RATING;
        double high = MAX_RATING;

        for (int step = 0; step < SEED_SEARCH_STEPS; step++) {
            double mid = (low + high) / 2;
            double rank = expectedRank(mid, ratings, counts) - ownContribution(mid, ownRating);

            // Higher rating means a better (smaller) expected rank, so overshooting
            // the target rank means the guess was too low.
            if (rank > targetRank) {
                low = mid;
            } else {
                high = mid;
            }
        }

        return (low + high) / 2;
    }

    /** The term a hypothetical rating contributes against the competitor's own entry. */
    private static double ownContribution(double candidate, double ownRating) {
        return 1 / (1 + Math.pow(10, (candidate - ownRating) / SCALE));
    }

    /** How much of the computed delta actually lands, by contests already sat. */
    private static double attendanceFactor(int attended) {
        return attended < ATTENDANCE_FACTORS.length ? ATTENDANCE_FACTORS[attended] : SETTLED_FACTOR;
    }

    // ── Withdrawing and replaying ─────────────────────────────────────────

    /**
     * Takes back one contest's effect on everybody's rating.
     *
     * <p>What "make this contest unrated" means once it has already been rated,
     * and what a rejudge has to do before it can re-score. Because later contests
     * were computed on top of the ratings this one produced, taking it back is
     * not a subtraction: everybody it touched is rewound to where they stood
     * before it, and every rated contest since is replayed in order.
     */
    @Transactional
    public void withdrawRatings(Contest contest) {
        replayFrom(contest.getStartsAt());
    }

    /**
     * Recomputes the rating ledger from a moment onwards.
     *
     * <p>The one operation behind every way a rating can need correcting —
     * withdrawing a contest, restoring one, or re-scoring after a rejudge. All
     * three flip a flag and then call this, which is what keeps them from each
     * needing their own idea of what "undo" means.
     *
     * <p>Four phases, and the order is what makes it correct:
     *
     * <ol>
     *   <li>Ask the <em>ledger</em> which contests from that moment on have left
     *       rows behind, and the <em>contests</em> which of them should have rows
     *       when this is over. Those two sets differ by exactly the contest whose
     *       rated flag has just changed, which is why both are needed.
     *   <li>Rewind everybody either set touches to the rating, peak and
     *       attendance they had immediately before the moment — read from the
     *       ledger, because the balance on the user already includes everything
     *       being undone.
     *   <li>Purge the old rows.
     *   <li>Replay the contests that should be rated, in the order they happened,
     *       because a rating is a running total and each one is computed against
     *       the ratings the ones before it produced.
     * </ol>
     *
     * <p>Anybody who never competed in the affected window is not touched at all:
     * their rating cannot depend on contests they did not sit.
     */
    @Transactional
    public void replayFrom(Instant from) {
        Instant now = Instant.now();
        List<Contest> toApply = contestRepository.findRatedEndedFrom(from, now);
        List<Contest> toPurge = ratingChangeRepository.findContestsWithChangesFrom(from);

        Set<User> touched = new LinkedHashSet<>();
        for (Contest contest : toPurge) {
            ratingChangeRepository
                    .findByContestId(contest.getId())
                    .forEach(change -> touched.add(change.getUser()));
        }
        for (Contest contest : toApply) {
            participationRepository
                    .findAllRanked(contest.getId())
                    .forEach(participation -> touched.add(participation.getUser()));
        }

        for (User user : touched) {
            rewind(user, from);
        }
        for (Contest contest : toPurge) {
            ratingChangeRepository.deleteByContestId(contest.getId());
            contest.setRatingsAppliedAt(null);
        }
        // Forced out before the replay, so the deletes land ahead of the inserts
        // that reuse the same (contest, user) unique key.
        ratingChangeRepository.flush();

        for (Contest contest : toApply) {
            rate(contest);
            contest.setRatingsAppliedAt(now);
        }
    }

    /** Puts one user back to the state the ledger says they were in before a moment. */
    private void rewind(User user, Instant before) {
        List<ContestRatingChange> previous =
                ratingChangeRepository.findBefore(user.getId(), before, PageRequest.of(0, 1));

        double rating = previous.isEmpty() ? User.INITIAL_RATING : previous.getFirst().getRatingAfter();
        Double peak = ratingChangeRepository.maxRatingBefore(user.getId(), before);
        long attended = ratingChangeRepository.countAttendedBefore(user.getId(), before);

        user.setRating(rating);
        user.setMaxRating(peak == null ? User.INITIAL_RATING : Math.max(User.INITIAL_RATING, peak));
        user.setContestsAttended((int) attended);
    }

    /** One entry in the field being rated: who, at what rating, finishing where. */
    private record Competitor(User user, double rating, int rank) {}
}
