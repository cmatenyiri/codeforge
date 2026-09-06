package com.codeforge.repository;

import com.codeforge.domain.Editorial;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface EditorialRepository extends JpaRepository<Editorial, Long> {

    Optional<Editorial> findByProblemSlug(String slug);

    boolean existsByProblemId(Long problemId);

    /** Which problems already have one, so the seeder can fill in only the gaps. */
    @Query("select e.problem.slug from Editorial e")
    Set<String> findProblemSlugs();
}
