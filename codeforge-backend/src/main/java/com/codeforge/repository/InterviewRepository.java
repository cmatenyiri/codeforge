package com.codeforge.repository;

import com.codeforge.domain.Interview;
import com.codeforge.domain.InterviewStatus;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InterviewRepository extends JpaRepository<Interview, Long> {

    Page<Interview> findByUserIdOrderByStartedAtDesc(Long userId, Pageable pageable);

    Optional<Interview> findByUserIdAndStatus(Long userId, InterviewStatus status);
}
