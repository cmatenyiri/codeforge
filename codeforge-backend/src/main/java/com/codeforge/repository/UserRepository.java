package com.codeforge.repository;

import com.codeforge.domain.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCase(String username);

    /** Uniqueness check for a rename: the caller's own row must not count as a clash. */
    boolean existsByUsernameIgnoreCaseAndIdNot(String username, Long id);

    boolean existsByEmailIgnoreCase(String email);

    // ── Rating leaderboard ────────────────────────────────────────────────
    // Ordinary JPQL, because the rating is a column: the ledger in
    // ContestRatingChange is the audit trail, and User.rating is the running
    // balance kept in step with it precisely so these two queries can be an
    // indexed ORDER BY rather than an aggregate over every contest ever held.

    /**
     * The rating table, strongest first.
     *
     * <p>Only accounts that have actually sat a rated contest appear. Everyone
     * else is on the starting 1500, which is a placeholder rather than a
     * measurement — listing them would put tens of thousands of people who have
     * never competed in the middle of the table.
     */
    @Query("""
            select u from User u
            where u.enabled = true and u.contestsAttended > 0
            order by u.rating desc, u.id asc
            """)
    List<User> findRatingLeaderboard(Pageable pageable);

    @Query("select count(u) from User u where u.enabled = true and u.contestsAttended > 0")
    long countRated();

    /**
     * How many rated competitors sit strictly above this rating.
     *
     * <p>Plus one is the rank. Asked this way rather than by finding the user's
     * row in a sorted list because the list is the whole table, and a profile
     * only needs one number from it.
     */
    @Query("""
            select count(u) from User u
            where u.enabled = true and u.contestsAttended > 0 and u.rating > :rating
            """)
    long countRatedAbove(@Param("rating") double rating);

    // ── Solved leaderboard ────────────────────────────────────────────────
    // Native, and deliberately not denormalised onto the user.
    //
    // The number has to be "distinct problems solved, counted over the live
    // catalogue only" — the same definition the progress rings and the profile
    // use — and that is not a counter that can be incremented on acceptance:
    // archiving a problem silently changes it for everybody who had solved it.
    // A stored count would drift the moment a problem left the catalogue, and a
    // leaderboard disagreeing with the profile it links to is worse than a
    // slightly more expensive query. MySQL-specific by the same licence the rest
    // of the schema takes: this application has one database.

    /**
     * The solved table, most points first.
     *
     * <p>Weighted by difficulty rather than a flat count, so that grinding a
     * hundred easy problems does not outrank working through the hard ones —
     * which is the same judgement the contest scoring makes with its 3/4/5/6.
     */
    @Query(
            nativeQuery = true,
            value =
                    """
                    select u.id as userId, u.username as username, u.avatar as avatar,
                           coalesce(t.solved, 0) as solved, coalesce(t.points, 0) as points
                    from users u
                    left join (
                        select d.user_id as user_id, count(*) as solved, sum(p.difficulty_rank) as points
                        from (select distinct user_id, problem_id from submissions where status = 'ACCEPTED') d
                        join problems p on p.id = d.problem_id
                        where p.archived = false and p.published = true
                        group by d.user_id
                    ) t on t.user_id = u.id
                    where u.enabled = true and coalesce(t.solved, 0) > 0
                    order by points desc, solved desc, u.id asc
                    limit :limit offset :offset
                    """)
    List<SolvedRanking> findSolvedLeaderboard(@Param("limit") int limit, @Param("offset") int offset);

    @Query(
            nativeQuery = true,
            value =
                    """
                    select count(*) from (
                        select d.user_id
                        from (select distinct user_id, problem_id from submissions where status = 'ACCEPTED') d
                        join problems p on p.id = d.problem_id
                        join users u on u.id = d.user_id
                        where p.archived = false and p.published = true and u.enabled = true
                        group by d.user_id
                    ) ranked
                    """)
    long countSolvedRanked();

    /**
     * How many people are strictly ahead of this weighted score.
     *
     * <p>Plus one is the global rank shown on a profile.
     */
    @Query(
            nativeQuery = true,
            value =
                    """
                    select count(*) from (
                        select sum(p.difficulty_rank) as points
                        from (select distinct user_id, problem_id from submissions where status = 'ACCEPTED') d
                        join problems p on p.id = d.problem_id
                        join users u on u.id = d.user_id
                        where p.archived = false and p.published = true and u.enabled = true
                        group by d.user_id
                        having sum(p.difficulty_rank) > :points
                    ) ahead
                    """)
    long countSolvedRankedAbove(@Param("points") long points);

    /** Projection for the solved leaderboard. */
    interface SolvedRanking {
        Long getUserId();

        String getUsername();

        String getAvatar();

        long getSolved();

        long getPoints();
    }
}
