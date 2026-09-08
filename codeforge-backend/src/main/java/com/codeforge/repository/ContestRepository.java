package com.codeforge.repository;

import com.codeforge.domain.Contest;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContestRepository extends JpaRepository<Contest, Long> {

    Optional<Contest> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    boolean existsByTitleIgnoreCase(String title);

    boolean existsByTitleIgnoreCaseAndIdNot(String title, Long id);

    /**
     * One contest with its questions and the problems behind them.
     *
     * <p>Everything that touches a contest needs the slots — reading a problem,
     * scoring a submission, sealing the snapshots — so this is the default way
     * to load one rather than a special case.
     */
    @Query("""
            select c from Contest c
            left join fetch c.problems slot
            left join fetch slot.problem
            where c.id = :id
            """)
    Optional<Contest> findWithProblems(@Param("id") Long id);

    @Query("""
            select c from Contest c
            left join fetch c.problems slot
            left join fetch slot.problem
            where c.slug = :slug
            """)
    Optional<Contest> findWithProblemsBySlug(@Param("slug") String slug);

    /**
     * The announced contests, newest start first.
     *
     * <p>Drafts are excluded here rather than filtered by a flag the caller
     * passes, for the same reason the problem catalogue has a separate authoring
     * query: the two have opposite defaults, and one query with a "show hidden"
     * switch is one bug away from announcing every unfinished contest.
     */
    @Query("select c from Contest c where c.published = true order by c.startsAt desc")
    Page<Contest> findPublished(Pageable pageable);

    /** Announced and not started yet, soonest first — the "upcoming" rail. */
    @Query("""
            select c from Contest c
            where c.published = true and c.startsAt > :now
            order by c.startsAt asc
            """)
    List<Contest> findUpcoming(@Param("now") Instant now, Pageable pageable);

    /** Announced and under way right now. Normally none, occasionally one. */
    @Query("""
            select c from Contest c
            where c.published = true and c.startsAt <= :now and c.endsAt > :now
            order by c.startsAt asc
            """)
    List<Contest> findRunning(@Param("now") Instant now);

    /**
     * Every rated contest that has already ended, oldest first.
     *
     * <p>The replay order for the rating ledger. When a contest is rejudged or
     * withdrawn, every rated contest from that point on has to be re-scored in
     * the order it originally happened — a rating is a running total, so redoing
     * one link means redoing the chain after it.
     */
    @Query("""
            select c from Contest c
            where c.published = true and c.rated = true and c.startsAt >= :from and c.endsAt <= :now
            order by c.startsAt asc, c.id asc
            """)
    List<Contest> findRatedEndedFrom(@Param("from") Instant from, @Param("now") Instant now);

    /** The authoring list: every contest, drafts included. */
    @Query("""
            select c from Contest c
            where (:search is null
                   or lower(c.title) like lower(concat('%', :search, '%'))
                   or lower(c.slug) like lower(concat('%', :search, '%')))
            """)
    Page<Contest> searchForAuthor(@Param("search") String search, Pageable pageable);

    /**
     * Whether any contest has ever asked this problem.
     *
     * <p>Consulted before a delete, exactly like the interview check next to it:
     * standings name the problems they asked, and removing one would leave a
     * finished contest pointing at nothing.
     */
    @Query("select count(cp) > 0 from ContestProblem cp where cp.problem.id = :id")
    boolean isUsedByContests(@Param("id") Long id);

    /**
     * One contest, locked for the row that seals it.
     *
     * <p>Sealing is a read that writes, triggered by whoever happens to load the
     * contest first after it starts — which, at the start of a popular contest,
     * is several hundred people in the same second. The lock makes exactly one of
     * them take the snapshots; everybody else waits a moment and finds them
     * already taken. Without it, two requests could each snapshot the problems,
     * and an author's edit landing between the two would be the one thing the
     * whole mechanism exists to prevent.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Contest c where c.id = :id")
    Optional<Contest> findForSealing(@Param("id") Long id);
}
