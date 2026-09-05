package com.codeforge.service;

import com.codeforge.domain.Difficulty;
import com.codeforge.domain.Problem;
import com.codeforge.exception.NotFoundException;
import com.codeforge.repository.ProblemRepository;
import com.codeforge.repository.SubmissionRepository;
import com.codeforge.security.SecurityUtils;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProblemService {

    private final ProblemRepository problemRepository;
    private final SubmissionRepository submissionRepository;

    /**
     * Searches the catalogue. All filters are optional.
     *
     * @param search matched against the title, case-insensitively; null for no text filter
     * @param difficulty null for any
     * @param tagSlug null for any
     */
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public Page<Problem> search(String search, Difficulty difficulty, String tagSlug, Pageable pageable) {
        Page<Problem> problems = problemRepository.search(search, difficulty, tagSlug, pageable);

        // Mapping to DTOs happens in the controller, outside this transaction, and
        // `open-in-view` is off — so anything the response needs has to be loaded
        // before the session closes. Tags are @BatchSize'd, making this one extra
        // query for the whole page rather than one per row.
        problems.forEach(problem -> problem.getTags().size());

        return problems;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public Problem getBySlug(String slug) {
        Problem problem = problemRepository.findBySlug(slug).orElseThrow(() -> NotFoundException.of("problem", slug));

        // Same reason as above. These are separate queries rather than one fetch
        // join on purpose: three List associations in a single join would trip
        // Hibernate's MultipleBagFetchException.
        problem.getTags().size();
        problem.getExamples().size();
        problem.getHints().size();
        problem.getTestCases().size();
        problem.getParameters().size();

        return problem;
    }

    /** The caller's solved problem ids, for the ticks on the catalogue. */
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public Set<Long> solvedProblemIds() {
        return submissionRepository.findSolvedProblemIds(SecurityUtils.requireCurrentUserId());
    }
}
