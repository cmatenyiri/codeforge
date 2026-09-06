package com.codeforge.repository;

import com.codeforge.domain.TestCase;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestCaseRepository extends JpaRepository<TestCase, Long> {

    List<TestCase> findByProblemIdAndHiddenFalseOrderByDisplayOrderAsc(Long problemId);

    List<TestCase> findByProblemIdOrderByDisplayOrderAsc(Long problemId);

    /**
     * How many graded cases a problem has.
     *
     * <p>A count rather than a filtered load: some hidden cases are hundreds of
     * kilobytes of generated input, and the solving page only ever shows the
     * number of them.
     */
    long countByProblemIdAndHiddenTrue(Long problemId);
}
