package com.codeforge.repository;

import com.codeforge.domain.Difficulty;
import com.codeforge.domain.Problem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProblemRepository extends JpaRepository<Problem, Long> {

    Optional<Problem> findBySlug(String slug);

    boolean existsByTitleIgnoreCase(String title);

    boolean existsBySlug(String slug);

    /**
     * Catalogue search. Every filter is optional — a null argument means "do not
     * narrow by this", which keeps one query serving the unfiltered list and
     * every combination of filters.
     *
     * <p>The tag join is only used for filtering; tags themselves load via the
     * batch-fetched association, so paging stays in SQL rather than in memory.
     *
     * <p>{@code status} is compared as a string rather than typed as the enum so
     * that the three branches can be written as one expression; each one is an
     * {@code exists} against the caller's own submissions, which is why the user
     * id travels with it.
     */
    @Query(
            """
            select distinct p from Problem p
            left join p.tags t
            where p.archived = false
              and (:search is null or lower(p.title) like lower(concat('%', :search, '%')))
              and (:difficulty is null or p.difficulty = :difficulty)
              and (:tagSlug is null or t.slug = :tagSlug)
              and (:status is null
                   or (:status = 'SOLVED' and exists (
                         select 1 from Submission accepted
                         where accepted.problem = p and accepted.user.id = :userId
                           and accepted.status = com.codeforge.domain.SubmissionStatus.ACCEPTED))
                   or (:status = 'ATTEMPTED'
                       and exists (
                         select 1 from Submission attempt where attempt.problem = p and attempt.user.id = :userId)
                       and not exists (
                         select 1 from Submission accepted
                         where accepted.problem = p and accepted.user.id = :userId
                           and accepted.status = com.codeforge.domain.SubmissionStatus.ACCEPTED))
                   or (:status = 'TODO' and not exists (
                         select 1 from Submission attempt where attempt.problem = p and attempt.user.id = :userId)))
            """)
    Page<Problem> search(
            @Param("search") String search,
            @Param("difficulty") Difficulty difficulty,
            @Param("tagSlug") String tagSlug,
            @Param("status") String status,
            @Param("userId") Long userId,
            Pageable pageable);

    /** How many live problems there are at each difficulty, for the progress rings. */
    @Query("""
            select p.difficulty as difficulty, count(p) as total from Problem p
            where p.archived = false
            group by p.difficulty
            """)
    List<DifficultyTotal> countByDifficulty();

    /**
     * Records one submission's outcome against the problem's counters.
     *
     * <p>An UPDATE rather than a read-modify-write on a loaded entity: two
     * submissions landing at the same moment would otherwise each read the same
     * total and write the same increment, losing one of them.
     *
     * @param accepted 1 for an accepted submission, 0 otherwise
     */
    @Modifying
    @Query("""
            update Problem p
            set p.totalSubmissions = p.totalSubmissions + 1,
                p.acceptedSubmissions = p.acceptedSubmissions + :accepted
            where p.id = :id
            """)
    void recordSubmissionOutcome(@Param("id") Long id, @Param("accepted") int accepted);

    /** Projection for {@link #countByDifficulty()}. */
    interface DifficultyTotal {
        Difficulty getDifficulty();

        long getTotal();
    }
}
