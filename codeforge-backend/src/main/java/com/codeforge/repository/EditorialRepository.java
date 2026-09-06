package com.codeforge.repository;

import com.codeforge.domain.Editorial;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EditorialRepository extends JpaRepository<Editorial, Long> {

    Optional<Editorial> findByProblemSlug(String slug);

    boolean existsByProblemId(Long problemId);

    Optional<Editorial> findByProblemId(Long problemId);

    void deleteByProblemId(Long problemId);

    /** Which problems already have one, so the seeder can fill in only the gaps. */
    @Query("select e.problem.slug from Editorial e")
    Set<String> findProblemSlugs();

    /** Which of these problems have an editorial — one query per page, not per row. */
    @Query("select e.problem.id from Editorial e where e.problem.id in :problemIds")
    Set<Long> findProblemIdsIn(@Param("problemIds") Collection<Long> problemIds);
}
