package com.codeforge.service;

import com.codeforge.domain.Tag;
import com.codeforge.repository.TagRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TagService {

    private final TagRepository tagRepository;

    /** The full tag list, for the catalogue's filter control. */
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public List<Tag> findAll() {
        return tagRepository.findAll(Sort.by("name"));
    }
}
