package com.codeforge.repository;

import com.codeforge.domain.Difficulty;
import com.codeforge.domain.Submission;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    /**
     * The caller's own history. Fetch-joins the problem because every row shows
     * its title and difficulty, and {@code open-in-view} is off — a lazy load
     * would fail once the mapper runs outside the transaction.
     */
    @Query(
            value =
                    """
                    select s from Submission s
                    join fetch s.problem p
                    where s.user.id = :userId
                    order by s.createdAt desc
                    """,
            countQuery = "select count(s) from Submission s where s.user.id = :userId")
    Page<Submission> findForUser(@Param("userId") Long userId, Pageable pageable);

    @Query(
            value =
                    """
                    select s from Submission s
                    join fetch s.problem p
                    where s.user.id = :userId and p.slug = :slug
                    order by s.createdAt desc
                    """,
            countQuery =
                    """
                    select count(s) from Submission s
                    where s.user.id = :userId and s.problem.slug = :slug
                    """)
    Page<Submission> findForUserAndProblem(
            @Param("userId") Long userId, @Param("slug") String slug, Pageable pageable);

    @Query("""
            select s from Submission s
            join fetch s.problem p
            where s.id = :id and s.user.id = :userId
            """)
    Optional<Submission> findOwned(@Param("id") Long id, @Param("userId") Long userId);

    /** The most recent submission per problem is what the editor restores, so ordering matters. */
    Optional<Submission> findFirstByUserIdAndProblemSlugOrderByCreatedAtDesc(Long userId, String slug);

    /** Whether this user has any history with a problem — their way back into an unpublished one. */
    boolean existsByUserIdAndProblemId(Long userId, Long problemId);

    boolean existsByUserIdAndProblemIdAndStatus(
            Long userId, Long problemId, com.codeforge.domain.SubmissionStatus status);

    /** Problem ids this user has solved, for the solved ticks on the catalogue. */
    @Query(
            """
            select distinct s.problem.id from Submission s
            where s.user.id = :userId and s.status = com.codeforge.domain.SubmissionStatus.ACCEPTED
            """)
    Set<Long> findSolvedProblemIds(@Param("userId") Long userId);

    /** Every problem this user has submitted to, accepted or not. */
    @Query("select distinct s.problem.id from Submission s where s.user.id = :userId")
    Set<Long> findAttemptedProblemIds(@Param("userId") Long userId);

    long countByUserId(Long userId);

    /** How many submissions a problem has, so authoring can refuse to delete a solved one. */
    long countByProblemId(Long problemId);

    @Query("""
            select count(s) from Submission s
            where s.user.id = :userId and s.status = com.codeforge.domain.SubmissionStatus.ACCEPTED
            """)
    long countAcceptedByUserId(@Param("userId") Long userId);

    /**
     * Distinct problems solved at each difficulty, counted over the live
     * catalogue only.
     *
     * <p>Counts distinct problems, not submissions: solving the same problem
     * three times is one solve, and the progress bars would otherwise pass 100%.
     *
     * <p>The visibility filter has to match {@link ProblemRepository#countByDifficulty()}
     * exactly, because these two numbers are shown as one fraction. A problem
     * that leaves the catalogue — unpublished back to a draft, or archived —
     * leaves both sides of it, so "12 / 40" stays a statement about the same set
     * of problems. Without it, retiring a problem somebody had solved would
     * shrink the denominator alone and leave a progress ring reading 3 / 2.
     *
     * <p>Note that this deliberately does not touch the submissions themselves.
     * Those rows stay, the history still lists them, and the acceptance rate
     * still counts them: a submission is a fact about what the user did, while a
     * solved count is a claim about the catalogue as it stands today.
     */
    @Query("""
            select s.problem.difficulty as difficulty, count(distinct s.problem.id) as total
            from Submission s
            where s.user.id = :userId and s.status = com.codeforge.domain.SubmissionStatus.ACCEPTED
              and s.problem.archived = false and s.problem.published = true
            group by s.problem.difficulty
            """)
    List<ProblemRepository.DifficultyTotal> countSolvedByDifficulty(@Param("userId") Long userId);

    // ── Contests ──────────────────────────────────────────────────────────

    /**
     * Every submission that counted towards a contest, oldest first.
     *
     * <p>What a rejudge re-runs and what the standings are rebuilt from. The
     * order matters to both: penalties depend on which attempts came before the
     * accepted one, so replaying them out of order would score the same
     * submissions differently.
     */
    @Query("""
            select s from Submission s
            join fetch s.contestProblem cp
            join fetch s.user
            where s.contest.id = :contestId and s.countedInContest = true
            order by s.createdAt asc, s.id asc
            """)
    List<Submission> findContestSubmissions(@Param("contestId") Long contestId);

    /** The caller's attempts at one contest problem, newest first — the arena's history tab. */
    @Query(
            value =
                    """
                    select s from Submission s
                    join fetch s.problem
                    where s.user.id = :userId and s.contestProblem.id = :contestProblemId
                    order by s.createdAt desc
                    """,
            countQuery =
                    """
                    select count(s) from Submission s
                    where s.user.id = :userId and s.contestProblem.id = :contestProblemId
                    """)
    Page<Submission> findForUserAndContestProblem(
            @Param("userId") Long userId,
            @Param("contestProblemId") Long contestProblemId,
            Pageable pageable);

    /** How many submissions a contest has to re-run, so a rejudge can report progress. */
    @Query("""
            select count(s) from Submission s
            where s.contest.id = :contestId and s.countedInContest = true
            """)
    long countContestSubmissions(@Param("contestId") Long contestId);

/**
     * Unlinks a contest's submissions from it, without touching the submissions.
     *
     * <p>For a contest being deleted. The attempts themselves are facts about
     * the people who made them — they stay in the history and still count as
     * solves — but their attribution to a contest that no longer exists does
     * not. Only reachable when nobody competed, since a contest with
     * participations cannot be deleted at all; what this catches is practice on
     * a finished contest's problems, which sets the link without creating a
     * participation.
     */
    @Modifying
    @Query("""
            update Submission s
            set s.contest = null, s.contestProblem = null, s.countedInContest = false, s.contestSeconds = null
            where s.contest.id = :contestId
            """)
    void detachFromContest(@Param("contestId") Long contestId);

    // ── Activity calendar ─────────────────────────────────────────────────

    /**
     * Submissions per day for one user, for the heatmap on their profile.
     *
     * <p>Native and MySQL-shaped because the grouping is a date truncation, which
     * JPQL has no portable spelling for. Days with nothing are simply absent —
     * a year of squares is mostly empty for most people, and sending eleven
     * months of zeroes to draw nothing would be the larger half of the response.
     *
     * <p>Bucketed in UTC, which is what the column stores and what the connection
     * runs in. A calendar that re-bucketed per viewer would move somebody's
     * streak when they travelled.
     */
    @Query(
            nativeQuery = true,
            value =
                    """
                    select date_format(s.created_at, '%Y-%m-%d') as day,
                           count(*) as total,
                           sum(case when s.status = 'ACCEPTED' then 1 else 0 end) as accepted
                    from submissions s
                    where s.user_id = :userId
                      and s.created_at >= :from and s.created_at < :until
                    group by day
                    order by day asc
                    """)
    List<ActivityDay> findActivityBetween(
            @Param("userId") Long userId, @Param("from") Instant from, @Param("until") Instant until);

    /**
     * Every calendar year this user has submitted anything in, newest first.
     *
     * <p>What fills the calendar's year picker. Only years with something in
     * them are offered — a dropdown listing years somebody was not yet a member
     * for is a dropdown of empty grids.
     */
    @Query(
            nativeQuery = true,
            value =
                    """
                    select distinct year(s.created_at)
                    from submissions s where s.user_id = :userId
                    order by 1 desc
                    """)
    List<Integer> findActiveYears(@Param("userId") Long userId);

    /**
     * Distinct days this user has ever submitted on, newest first.
     *
     * <p>Read by the streak counter, which needs to look further back than the
     * calendar shows: a streak running for four hundred days is exactly the one
     * worth reporting, and a query capped at a year would cut it off at 365.
     */
    @Query(
            nativeQuery = true,
            value =
                    """
                    select distinct date_format(s.created_at, '%Y-%m-%d')
                    from submissions s where s.user_id = :userId
                    order by 1 desc
                    """)
    List<String> findActiveDays(@Param("userId") Long userId);

    // ── Public profile ────────────────────────────────────────────────────

    /**
     * The same difficulty breakdown as {@link #countSolvedByDifficulty}, for
     * somebody else's profile.
     *
     * <p>A separate method taking an explicit id rather than the caller's, so
     * that reading a public profile can never accidentally be written as a read
     * of the current user's own numbers.
     */
    @Query("""
            select s.problem.difficulty as difficulty, count(distinct s.problem.id) as total
            from Submission s
            where s.user.id = :userId and s.status = com.codeforge.domain.SubmissionStatus.ACCEPTED
              and s.problem.archived = false and s.problem.published = true
            group by s.problem.difficulty
            """)
    List<ProblemRepository.DifficultyTotal> countSolvedByDifficultyFor(@Param("userId") Long userId);

    /**
     * Someone's recent accepted solves, for the "recent" list on a public
     * profile.
     *
     * <p>Accepted only, and one row per problem. A public profile is a
     * highlights reel, not an audit log: listing failures would make it a record
     * of somebody's worst afternoon, and repeats would fill it with one problem
     * submitted six times.
     */
    @Query(
            nativeQuery = true,
            value =
                    """
                    select p.slug as slug, p.title as title, p.difficulty as difficulty,
                           max(s.created_at) as solvedAt
                    from submissions s
                    join problems p on p.id = s.problem_id
                    where s.user_id = :userId and s.status = 'ACCEPTED'
                      and p.archived = false and p.published = true
                    group by p.id, p.slug, p.title, p.difficulty
                    order by solvedAt desc
                    limit :limit
                    """)
    List<RecentSolve> findRecentSolves(@Param("userId") Long userId, @Param("limit") int limit);

    /**
     * Distinct problems solved in each language, most first.
     *
     * <p>A problem solved in two languages counts for both, which is the only
     * reading that makes the list useful: it answers "what do they write in?",
     * not "how do their solves divide up". Counted over the live catalogue for
     * the same reason every other solved count is — a retired problem stops
     * counting for everybody at once.
     */
    @Query(
            nativeQuery = true,
            value =
                    """
                    select s.language as language, count(distinct s.problem_id) as solved
                    from submissions s
                    join problems p on p.id = s.problem_id
                    where s.user_id = :userId and s.status = 'ACCEPTED'
                      and p.archived = false and p.published = true
                    group by s.language
                    order by solved desc, s.language asc
                    """)
    List<LanguageCount> countSolvedByLanguage(@Param("userId") Long userId);

    /** Projection for {@link #countSolvedByLanguage}. */
    interface LanguageCount {
        String getLanguage();

        long getSolved();
    }

    /** Projection for {@link #findActivityBetween}. */
    interface ActivityDay {
        /** ISO {@code yyyy-MM-dd}, in UTC. */
        String getDay();

        long getTotal();

        long getAccepted();
    }

    /** Projection for {@link #findRecentSolves}. */
    interface RecentSolve {
        String getSlug();

        String getTitle();

        String getDifficulty();

        Instant getSolvedAt();
    }
}
