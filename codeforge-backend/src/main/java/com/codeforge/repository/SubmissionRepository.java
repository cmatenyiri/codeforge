package com.codeforge.repository;

import com.codeforge.domain.Difficulty;
import com.codeforge.domain.Submission;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
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

    @Query("""
            select count(s) from Submission s
            where s.user.id = :userId and s.status = com.codeforge.domain.SubmissionStatus.ACCEPTED
            """)
    long countAcceptedByUserId(@Param("userId") Long userId);

    /**
     * Distinct problems solved at each difficulty.
     *
     * <p>Counts distinct problems, not submissions: solving the same problem
     * three times is one solve, and the progress bars would otherwise pass 100%.
     */
    @Query("""
            select s.problem.difficulty as difficulty, count(distinct s.problem.id) as total
            from Submission s
            where s.user.id = :userId and s.status = com.codeforge.domain.SubmissionStatus.ACCEPTED
            group by s.problem.difficulty
            """)
    List<ProblemRepository.DifficultyTotal> countSolvedByDifficulty(@Param("userId") Long userId);
}
