package com.codeforge.repository;

import com.codeforge.domain.TestCase;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestCaseRepository extends JpaRepository<TestCase, Long> {

    List<TestCase> findByProblemIdAndHiddenFalseOrderByDisplayOrderAsc(Long problemId);

    List<TestCase> findByProblemIdOrderByDisplayOrderAsc(Long problemId);
}
