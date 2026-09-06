package com.codeforge.service;

import com.codeforge.domain.Difficulty;
import com.codeforge.domain.Editorial;
import com.codeforge.domain.Problem;
import com.codeforge.domain.ProblemStatusFilter;
import com.codeforge.exception.NotFoundException;
import com.codeforge.domain.TestCase;
import com.codeforge.repository.EditorialRepository;
import com.codeforge.repository.ProblemRepository;
import com.codeforge.repository.SubmissionRepository;
import com.codeforge.repository.TestCaseRepository;
import com.codeforge.security.SecurityUtils;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProblemService {

    private final ProblemRepository problemRepository;
    private final SubmissionRepository submissionRepository;
    private final TestCaseRepository testCaseRepository;
    private final EditorialRepository editorialRepository;

    /**
     * Searches the catalogue. All filters are optional.
     *
     * @param search matched against the title, case-insensitively; null for no text filter
     * @param difficulty null for any
     * @param tagSlug null for any
     * @param status null for any; otherwise narrowed by what the caller has done
     *     with each problem, which is why this is per-user and not cacheable
     */
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public Page<Problem> search(
            String search, Difficulty difficulty, String tagSlug, ProblemStatusFilter status, Pageable pageable) {

        Page<Problem> problems = problemRepository.search(
                search,
                difficulty,
                tagSlug,
                status == null ? null : status.name(),
                SecurityUtils.requireCurrentUserId(),
                pageable);

        // Mapping to DTOs happens in the controller, outside this transaction, and
        // `open-in-view` is off — so anything the response needs has to be loaded
        // before the session closes. Tags are @BatchSize'd, making this one extra
        // query for the whole page rather than one per row.
        problems.forEach(problem -> problem.getTags().size());

        return problems;
    }

    /**
     * One problem at random from those matching the filters — the "surprise me"
     * button, which is the only sane way into a catalogue of a hundred problems
     * when nothing in particular is wanted.
     *
     * <p>Picking an offset costs a count and a single-row page rather than
     * loading every candidate: it stays one round trip per query no matter how
     * large the catalogue grows.
     */
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public Problem random(String search, Difficulty difficulty, String tagSlug, ProblemStatusFilter status) {
        String statusName = status == null ? null : status.name();
        Long userId = SecurityUtils.requireCurrentUserId();
        Sort byId = Sort.by(Sort.Direction.ASC, "id");

        long total = problemRepository
                .search(search, difficulty, tagSlug, statusName, userId, PageRequest.of(0, 1, byId))
                .getTotalElements();
        if (total == 0) {
            throw NotFoundException.of("problem", "random");
        }

        int offset = ThreadLocalRandom.current().nextInt((int) Math.min(total, Integer.MAX_VALUE));
        List<Problem> picked = problemRepository
                .search(search, difficulty, tagSlug, statusName, userId, PageRequest.of(offset, 1, byId))
                .getContent();

        if (picked.isEmpty()) {
            // The catalogue shrank between the two queries; the first match will do.
            picked = problemRepository
                    .search(search, difficulty, tagSlug, statusName, userId, PageRequest.of(0, 1, byId))
                    .getContent();
        }

        Problem problem = picked.stream().findFirst().orElseThrow(() -> NotFoundException.of("problem", "random"));

        // Same as in search: the response carries the tags, and this transaction
        // closes before the mapper runs.
        problem.getTags().size();

        return problem;
    }

    /**
     * A problem with everything the solving page renders — but deliberately
     * <em>without</em> its test cases.
     *
     * <p>Hidden cases can be hundreds of kilobytes of generated input, and the
     * page shows only the visible ones and a count of the rest. Loading the
     * collection here would pull every one of those blobs through the connection
     * on each page view, to throw all but two of them away.
     */
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public Problem getBySlug(String slug) {
        Problem problem = problemRepository.findBySlug(slug).orElseThrow(() -> NotFoundException.of("problem", slug));

        // A draft is not in the catalogue and must not be reachable by guessing
        // its URL either — but its author has to be able to open it, which is how
        // an unpublished problem is previewed and test-run before release. An
        // archived problem stays readable: people have solved it, and their
        // submission history links straight here.
        if (!problem.isPublished() && !SecurityUtils.isAdmin()) {
            throw NotFoundException.of("problem", slug);
        }

        // Same reason as above. These are separate queries rather than one fetch
        // join on purpose: three List associations in a single join would trip
        // Hibernate's MultipleBagFetchException.
        problem.getTags().size();
        problem.getExamples().size();
        problem.getHints().size();
        problem.getParameters().size();

        return problem;
    }

    /**
     * The same problem with every test case loaded — the one caller that needs
     * the hidden inputs is the judge.
     */
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public Problem getForJudging(String slug) {
        Problem problem = getBySlug(slug);
        problem.getTestCases().size();

        return problem;
    }

    /** The sample cases, which are the only ones a client may see. */
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public List<TestCase> visibleTestCases(Long problemId) {
        return testCaseRepository.findByProblemIdAndHiddenFalseOrderByDisplayOrderAsc(problemId);
    }

    /** How many graded cases a submission will face. A number, never their contents. */
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public int hiddenTestCaseCount(Long problemId) {
        return (int) testCaseRepository.countByProblemIdAndHiddenTrue(problemId);
    }

    /**
     * A problem's written solution.
     *
     * <p>Its own call, made only when the tab is opened: the walkthrough and four
     * reference implementations are larger than the problem itself, and most
     * visits never ask for them.
     */
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public Editorial editorial(String slug) {
        Editorial editorial = editorialRepository
                .findByProblemSlug(slug)
                .orElseThrow(() -> NotFoundException.of("editorial", slug));

        editorial.getSolutions().size();

        return editorial;
    }

    /** Whether there is an editorial to open, so the tab is not a dead end. */
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public boolean hasEditorial(Long problemId) {
        return editorialRepository.existsByProblemId(problemId);
    }

    /** The caller's solved problem ids, for the ticks on the catalogue. */
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public Set<Long> solvedProblemIds() {
        return submissionRepository.findSolvedProblemIds(SecurityUtils.requireCurrentUserId());
    }

    /**
     * Every problem the caller has submitted to. A superset of the solved ids:
     * the catalogue draws the difference between them as "attempted".
     */
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public Set<Long> attemptedProblemIds() {
        return submissionRepository.findAttemptedProblemIds(SecurityUtils.requireCurrentUserId());
    }
}
