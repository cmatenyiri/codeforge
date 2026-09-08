package com.codeforge.repository;

import com.codeforge.domain.ContestProblem;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContestProblemRepository extends JpaRepository<ContestProblem, Long> {

    Optional<ContestProblem> findByContestIdAndPosition(Long contestId, int position);

    List<ContestProblem> findByContestIdOrderByPositionAsc(Long contestId);

    long countByContestId(Long contestId);

    /**
     * Whether this problem was asked in a contest this user actually sat.
     *
     * <p>The contest half of "your own history still resolves". A contest
     * problem is normally an unpublished draft until the round is over, so
     * without this a participant's own submission history would link to a 404
     * for the problem they solved an hour ago.
     */
    @Query("""
            select count(s) > 0 from Submission s
            where s.contestProblem.problem.id = :problemId and s.user.id = :userId
            """)
    boolean isInContestOf(@Param("problemId") Long problemId, @Param("userId") Long userId);

    /**
     * How many questions each of these contests has, in one query.
     *
     * <p>A list page needs the number and nothing else, and the rows themselves
     * are the last thing it should load: every one carries a frozen snapshot with
     * every test case in it, so touching the collection to call {@code size()}
     * would pull megabytes of judge input through the connection to render a "4
     * problems" label.
     */
    @Query("""
            select cp.contest.id as contestId, count(cp) as total from ContestProblem cp
            where cp.contest.id in :contestIds
            group by cp.contest.id
            """)
    List<ContestRegistrationRepository.ContestCount> countByContestIds(
            @Param("contestIds") Collection<Long> contestIds);
}
