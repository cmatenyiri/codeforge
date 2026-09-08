package com.codeforge.repository;

import com.codeforge.domain.ContestParticipation;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContestParticipationRepository extends JpaRepository<ContestParticipation, Long> {

    Optional<ContestParticipation> findByContestIdAndUserId(Long contestId, Long userId);

    /**
     * One participation with its per-problem rows, for the arena and for
     * attributing a verdict.
     *
     * <p>Fetch-joins the user because the caller's own row is rendered above the
     * standings, name and avatar included, and {@code open-in-view} is off — a
     * lazy load would fail once the mapper runs outside the transaction.
     */
    @Query("""
            select p from ContestParticipation p
            join fetch p.user
            left join fetch p.problems
            where p.contest.id = :contestId and p.user.id = :userId
            """)
    Optional<ContestParticipation> findWithProblems(
            @Param("contestId") Long contestId, @Param("userId") Long userId);

    boolean existsByContestIdAndUserId(Long contestId, Long userId);

    /** Everyone who entered, whether or not they turned up. */
    long countByContestId(Long contestId);

    /** Only those who actually submitted something — the field the standings rank. */
    long countByContestIdAndSubmissionCountGreaterThan(Long contestId, int threshold);

    @Modifying
    void deleteByContestIdAndUserId(Long contestId, Long userId);

    /**
     * Which of these contests the caller has entered, for the list page's
     * buttons.
     */
    @Query("""
            select p.contest.id from ContestParticipation p
            where p.user.id = :userId and p.contest.id in :contestIds
            """)
    Set<Long> findEnteredContestIds(
            @Param("userId") Long userId, @Param("contestIds") Iterable<Long> contestIds);

    /**
     * Entry totals for a page of contests, in one query.
     *
     * <p>A count per row would be twenty round trips to render a list of twenty
     * contests, which is the classic way a list page gets slow without anything
     * obviously wrong with it.
     */
    @Query("""
            select p.contest.id as contestId, count(p) as total from ContestParticipation p
            where p.contest.id in :contestIds
            group by p.contest.id
            """)
    List<ContestCount> countEnteredByContestIds(@Param("contestIds") Collection<Long> contestIds);

    /** The same, counting only those who submitted something. */
    @Query("""
            select p.contest.id as contestId, count(p) as total from ContestParticipation p
            where p.contest.id in :contestIds and p.submissionCount > 0
            group by p.contest.id
            """)
    List<ContestCount> countCompetedByContestIds(@Param("contestIds") Collection<Long> contestIds);

    /** Projection for the batched counts. */
    interface ContestCount {
        Long getContestId();

        long getTotal();
    }

    /**
     * A page of the standings, in ranking order.
     *
     * <p>Fetch-joins the user because every row shows a name and an avatar, and
     * {@code open-in-view} is off — a lazy load would fail once the mapper runs
     * outside the transaction. The per-problem rows come along too: they are the
     * grid, and fetching them per row would be a query per participant.
     *
     * <p>Ordered by the same expression the ranks were computed from, so a page
     * of standings and the rank numbers on it can never disagree.
     */
    @Query(
            value =
                    """
                    select distinct p from ContestParticipation p
                    join fetch p.user
                    left join fetch p.problems
                    where p.contest.id = :contestId
                      and (:includeAbsent = true or p.submissionCount > 0)
                    order by p.score desc, p.totalTimeSeconds asc, p.id asc
                    """,
            countQuery =
                    """
                    select count(p) from ContestParticipation p
                    where p.contest.id = :contestId
                      and (:includeAbsent = true or p.submissionCount > 0)
                    """)
    Page<ContestParticipation> findStandings(
            @Param("contestId") Long contestId,
            @Param("includeAbsent") boolean includeAbsent,
            Pageable pageable);

    /**
     * Every participation of a contest, in ranking order, without paging.
     *
     * <p>What the ranker and the rating pass read. Both have to see the whole
     * field at once — a rank is a position within all of it, and the expected
     * rank in the rating formula is a sum over every other competitor — so
     * neither can be done a page at a time.
     */
    @Query("""
            select p from ContestParticipation p
            join fetch p.user
            left join fetch p.problems
            where p.contest.id = :contestId
            order by p.score desc, p.totalTimeSeconds asc, p.id asc
            """)
    List<ContestParticipation> findAllRanked(@Param("contestId") Long contestId);

    /** The caller's contest history, most recent first. */
    @Query(
            value =
                    """
                    select p from ContestParticipation p
                    join fetch p.contest c
                    where p.user.id = :userId and c.published = true
                    order by c.startsAt desc
                    """,
            countQuery =
                    """
                    select count(p) from ContestParticipation p
                    where p.user.id = :userId and p.contest.published = true
                    """)
    Page<ContestParticipation> findHistory(@Param("userId") Long userId, Pageable pageable);

    /** How many contests this user has actually sat, for the profile header. */
    @Query("""
            select count(p) from ContestParticipation p
            where p.user.id = :userId and p.contest.published = true
            """)
    long countAttended(@Param("userId") Long userId);

    /**
     * How many competitors finished strictly ahead of a given score and time.
     *
     * <p>Plus one is the rank, by the definition a contest uses: everybody on the
     * same score and time shares a place, and the places behind them are consumed
     * — two people on 2nd are followed by 4th. Asked as a count rather than read
     * off a row so that a page of the live standings can be numbered correctly
     * even though the tie it belongs to may start on the page before.
     */
    @Query("""
            select count(p) from ContestParticipation p
            where p.contest.id = :contestId
              and (p.score > :score
                   or (p.score = :score and p.totalTimeSeconds < :totalTimeSeconds))
            """)
    long countAhead(
            @Param("contestId") Long contestId,
            @Param("score") int score,
            @Param("totalTimeSeconds") long totalTimeSeconds);

    /** Everything a contest recorded, dropped when the contest itself goes. */
    @Modifying
    void deleteByContestId(Long contestId);

    /**
     * Only the rows of people who competed.
     *
     * <p>What a rejudge clears before rebuilding. Entries with no submissions are
     * left alone: there is nothing in them for a rejudge to change, and removing
     * one would un-register somebody.
     */
    @Modifying
    void deleteByContestIdAndSubmissionCountGreaterThan(Long contestId, int threshold);

    /**
     * This user's participations across a set of contests.
     *
     * <p>The same batching as the rating changes: a page of past contests shows
     * the caller's rank against each, and that has to be one query.
     */
    @Query("""
            select p from ContestParticipation p
            where p.user.id = :userId and p.contest.id in :contestIds
            """)
    List<ContestParticipation> findForUserAndContests(
            @Param("userId") Long userId, @Param("contestIds") java.util.Collection<Long> contestIds);

}
