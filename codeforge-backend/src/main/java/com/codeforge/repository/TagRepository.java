package com.codeforge.repository;

import com.codeforge.domain.Tag;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TagRepository extends JpaRepository<Tag, Long> {

    Optional<Tag> findBySlug(String slug);

    boolean existsByNameIgnoreCase(String name);
}
