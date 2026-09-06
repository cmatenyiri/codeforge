package com.codeforge.repository;

import com.codeforge.domain.Interview;
import com.codeforge.domain.InterviewStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InterviewRepository extends JpaRepository<Interview, Long> {

    /**
     * A page of the caller's interviews, newest first.
     *
     * <p>Carries no slots on purpose: the history table shows a date, a format
     * and a score, and fetch-joining a collection alongside a {@code Pageable}
     * would make Hibernate page the whole result set in memory.
     */
    Page<Interview> findByUserIdOrderByStartedAtDesc(Long userId, Pageable pageable);

    /**
     * One of the caller's interviews with its slots and their problems.
     *
     * <p>Scoped to the owner in the query rather than checked afterwards, so
     * another user's id is a 404 and not a 403 — which would confirm it exists.
     */
    @Query("""
            select i from Interview i
            left join fetch i.problems slot
            left join fetch slot.problem
            left join fetch slot.solvedBySubmission
            where i.id = :id and i.user.id = :userId
            """)
    Optional<Interview> findOwned(@Param("id") Long id, @Param("userId") Long userId);

    /** The caller's unfinished interview, if they have one. Only ever one at a time. */
    @Query("""
            select i from Interview i
            left join fetch i.problems slot
            left join fetch slot.problem
            where i.user.id = :userId and i.status = com.codeforge.domain.InterviewStatus.IN_PROGRESS
            """)
    Optional<Interview> findActive(@Param("userId") Long userId);

    boolean existsByUserIdAndStatus(Long userId, InterviewStatus status);

    /**
     * Problems this user has already been given in a recent interview.
     *
     * <p>The cooldown behind the "unseen problem" feeling: a set is sampled from
     * everything the catalogue has, so without this a short catalogue would hand
     * back last week's round.
     */
    @Query("""
            select distinct slot.problem.id from InterviewProblem slot
            where slot.interview.user.id = :userId and slot.interview.startedAt >= :since
            """)
    Set<Long> findProblemIdsUsedSince(@Param("userId") Long userId, @Param("since") Instant since);

    /** Every interview of this user's still marked in progress — swept when they come back. */
    List<Interview> findByUserIdAndStatus(Long userId, InterviewStatus status);
}
