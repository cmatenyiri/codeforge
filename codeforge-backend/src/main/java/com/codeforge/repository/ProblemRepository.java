package com.codeforge.repository;

import com.codeforge.domain.Difficulty;
import com.codeforge.domain.Problem;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
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
     */
    @Query(
            """
            select distinct p from Problem p
            left join p.tags t
            where p.archived = false
              and (:search is null or lower(p.title) like lower(concat('%', :search, '%')))
              and (:difficulty is null or p.difficulty = :difficulty)
              and (:tagSlug is null or t.slug = :tagSlug)
            """)
    Page<Problem> search(
            @Param("search") String search,
            @Param("difficulty") Difficulty difficulty,
            @Param("tagSlug") String tagSlug,
            Pageable pageable);
}
