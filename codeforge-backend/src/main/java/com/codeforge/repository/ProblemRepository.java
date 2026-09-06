package com.codeforge.repository;

import com.codeforge.domain.Difficulty;
import com.codeforge.domain.Problem;
import java.util.Collection;
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

    /**
     * Problems an interview may draw at one difficulty, each tagged with how
     * familiar it already is to this caller.
     *
     * <p>Only problems that can actually be judged are candidates: a problem with
     * no solution signature or no test cases renders an editor that cannot run,
     * which is a wasted slot in a timed round rather than a hard question.
     *
     * <p>{@code familiarity} is what makes a mock feel unseen without keeping a
     * second, hidden catalogue: 0 for never attempted, 1 for attempted but not
     * solved, 2 for already solved. The sampler takes the lowest tier that has
     * anything in it, so a solved problem only comes back when the catalogue has
     * genuinely run out of fresher ones.
     *
     * @param excludedIds never empty — callers pass a sentinel, because SQL has
     *     no syntax for an empty {@code in} list
     */
    @Query(
            """
            select p.id as id,
                   case
                     when exists (select 1 from Submission accepted
                                  where accepted.problem = p and accepted.user.id = :userId
                                    and accepted.status = com.codeforge.domain.SubmissionStatus.ACCEPTED) then 2
                     when exists (select 1 from Submission attempt
                                  where attempt.problem = p and attempt.user.id = :userId) then 1
                     else 0
                   end as familiarity
            from Problem p
            where p.archived = false
              and p.difficulty = :difficulty
              and p.functionName is not null
              and p.returnType is not null
              and exists (select 1 from TestCase tc where tc.problem = p)
              and p.id not in :excludedIds
            """)
    List<InterviewCandidate> findInterviewCandidates(
            @Param("difficulty") Difficulty difficulty,
            @Param("userId") Long userId,
            @Param("excludedIds") Collection<Long> excludedIds);

    /** How many hints a problem has, so a reveal can stop at the last one. */
    @Query("select count(h) from Problem p join p.hints h where p.id = :id")
    long countHints(@Param("id") Long id);

    /** Projection for {@link #findInterviewCandidates}. */
    interface InterviewCandidate {
        Long getId();

        int getFamiliarity();
    }

    /** Projection for {@link #countByDifficulty()}. */
    interface DifficultyTotal {
        Difficulty getDifficulty();

        long getTotal();
    }
}
