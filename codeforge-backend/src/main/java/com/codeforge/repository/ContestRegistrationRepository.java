package com.codeforge.repository;

import com.codeforge.domain.ContestRegistration;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContestRegistrationRepository extends JpaRepository<ContestRegistration, Long> {

    boolean existsByContestIdAndUserId(Long contestId, Long userId);

    long countByContestId(Long contestId);

    @Modifying
    void deleteByContestIdAndUserId(Long contestId, Long userId);

    /**
     * Drops every registration for a contest.
     *
     * <p>Only used when the contest itself is being deleted. A registration is
     * an intention to sit something, so it has no meaning at all once the thing
     * is gone — unlike a participation, which is a record of what somebody
     * actually did and is why a contest with those cannot be deleted.
     */
    @Modifying
    void deleteByContestId(Long contestId);

    /** Which of these contests the caller has signed up for, for the list page's buttons. */
    @Query("""
            select r.contest.id from ContestRegistration r
            where r.user.id = :userId and r.contest.id in :contestIds
            """)
    Set<Long> findRegisteredContestIds(
            @Param("userId") Long userId, @Param("contestIds") Iterable<Long> contestIds);

    /**
     * Registration totals for a page of contests, in one query.
     *
     * <p>A count per row would be twenty round trips to render a list of twenty
     * contests, which is the classic way a list page gets slow without anything
     * obviously wrong with it.
     */
    @Query("""
            select r.contest.id as contestId, count(r) as total from ContestRegistration r
            where r.contest.id in :contestIds
            group by r.contest.id
            """)
    List<ContestCount> countByContestIds(@Param("contestIds") Collection<Long> contestIds);

    /** Projection for the batched counts. */
    interface ContestCount {
        Long getContestId();

        long getTotal();
    }
}
