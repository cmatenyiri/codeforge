package com.codeforge.repository;

import com.codeforge.domain.Submission;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    Page<Submission> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<Submission> findByUserIdAndProblemIdOrderByCreatedAtDesc(Long userId, Long problemId, Pageable pageable);

    /** Problem ids this user has solved, for the solved ticks on the catalogue. */
    @Query(
            """
            select distinct s.problem.id from Submission s
            where s.user.id = :userId and s.status = com.codeforge.domain.SubmissionStatus.ACCEPTED
            """)
    Set<Long> findSolvedProblemIds(@Param("userId") Long userId);
}
