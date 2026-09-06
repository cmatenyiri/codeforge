package com.codeforge.repository;

import com.codeforge.domain.TestCase;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    /**
     * How many cases each of these problems has, and how many are hidden.
     *
     * <p>One query for a whole page of the authoring catalogue. The alternative —
     * two counts per row — is twenty round trips to draw twenty "3 samples, 12
     * hidden" labels.
     */
    @Query(
            """
            select tc.problem.id as problemId,
                   count(tc) as total,
                   sum(case when tc.hidden = true then 1 else 0 end) as hidden
            from TestCase tc
            where tc.problem.id in :problemIds
            group by tc.problem.id
            """)
    List<TestCaseCounts> countByProblemIds(@Param("problemIds") Collection<Long> problemIds);

    /** Projection for {@link #countByProblemIds}. */
    interface TestCaseCounts {
        Long getProblemId();

        long getTotal();

        long getHidden();
    }
}
