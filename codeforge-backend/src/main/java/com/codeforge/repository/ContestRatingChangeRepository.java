package com.codeforge.repository;

import com.codeforge.domain.Contest;
import com.codeforge.domain.ContestRatingChange;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContestRatingChangeRepository extends JpaRepository<ContestRatingChange, Long> {

    /**
     * One user's whole rating history, oldest first — the profile's rating graph.
     *
     * <p>Fetch-joins the contest because every point on the graph is labelled
     * with the contest that caused it.
     */
    @Query("""
            select rc from ContestRatingChange rc
            join fetch rc.contest c
            where rc.user.id = :userId
            order by c.startsAt asc
            """)
    List<ContestRatingChange> findHistory(@Param("userId") Long userId);

    List<ContestRatingChange> findByContestId(Long contestId);

    boolean existsByContestId(Long contestId);

    /**
     * Drops a contest's contribution to the ledger.
     *
     * <p>Half of withdrawing a contest — the other half is replaying every rated
     * contest since, because a rating is a running total and later ones were
     * computed on top of these.
     */
    @Modifying
    void deleteByContestId(Long contestId);

    /**
     * How many rated contests this user had sat before a given one.
     *
     * <p>Recounted from the ledger during a replay rather than read off the
     * user's denormalised counter, which at that moment is being rebuilt.
     */
    @Query("""
            select count(rc) from ContestRatingChange rc
            where rc.user.id = :userId and rc.contest.startsAt < :before
            """)
    long countAttendedBefore(
            @Param("userId") Long userId, @Param("before") Instant before);

    /**
     * This user's rating changes from before a moment, most recent first.
     *
     * <p>A replay reads the first row of this to learn what their rating was
     * before the contests being recomputed — which is the only honest starting
     * point, since the stored balance on the user already has the changes being
     * withdrawn baked into it.
     */
    @Query("""
            select rc from ContestRatingChange rc
            join fetch rc.contest c
            where rc.user.id = :userId and c.startsAt < :before
            order by c.startsAt desc
            """)
    List<ContestRatingChange> findBefore(
            @Param("userId") Long userId,
            @Param("before") Instant before,
            Pageable pageable);

    /** The peak this user had reached before a moment, ignoring everything since. */
    @Query("""
            select max(rc.ratingAfter) from ContestRatingChange rc
            where rc.user.id = :userId and rc.contest.startsAt < :before
            """)
    Double maxRatingBefore(
            @Param("userId") Long userId, @Param("before") Instant before);

    /**
     * Every contest from a moment onwards that has left rows in the ledger.
     *
     * <p>Not the same set as "rated contests from that moment": a contest just
     * declared unrated still has its rows, and they are exactly what has to be
     * purged. Asking the ledger what it contains — rather than asking the
     * contests what they claim to be — is what makes a replay converge on the
     * same answer no matter which flag was flipped to start it.
     */
    @Query("""
            select distinct rc.contest from ContestRatingChange rc
            where rc.contest.startsAt >= :from
            """)
    List<Contest> findContestsWithChangesFrom(@Param("from") Instant from);

    Optional<ContestRatingChange> findByContestIdAndUserId(Long contestId, Long userId);

    /**
     * This user's rating changes across a set of contests.
     *
     * <p>Asked for a whole page of the contest list at once, so that showing
     * "+27" against each past contest costs one query rather than one per row.
     */
    @Query("""
            select rc from ContestRatingChange rc
            where rc.user.id = :userId and rc.contest.id in :contestIds
            """)
    List<ContestRatingChange> findForUserAndContests(
            @Param("userId") Long userId, @Param("contestIds") Collection<Long> contestIds);

    /**
     * The rating changes for a set of users in one contest.
     *
     * <p>Asked for a whole page of standings at once. The alternative — a lookup
     * per row — is twenty-five queries to render one screen, and the standings
     * are the most-read page a contest has.
     */
    @Query("""
            select rc from ContestRatingChange rc
            where rc.contest.id = :contestId and rc.user.id in :userIds
            """)
    List<ContestRatingChange> findForContestAndUsers(
            @Param("contestId") Long contestId, @Param("userIds") Collection<Long> userIds);
}
