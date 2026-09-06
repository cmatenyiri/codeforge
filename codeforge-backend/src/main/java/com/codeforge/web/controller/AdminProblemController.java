package com.codeforge.web.controller;

import com.codeforge.domain.Difficulty;
import com.codeforge.domain.Language;
import com.codeforge.domain.Problem;
import com.codeforge.domain.ProblemState;
import com.codeforge.service.ExecutionService;
import com.codeforge.service.ProblemAuthoringService;
import com.codeforge.service.ProblemAuthoringService.TestCaseCounts;
import com.codeforge.web.dto.admin.AdminProblemDetailResponse;
import com.codeforge.web.dto.admin.AdminProblemSummaryResponse;
import com.codeforge.web.dto.admin.ProblemStateRequest;
import com.codeforge.web.dto.admin.ProblemUpsertRequest;
import com.codeforge.web.dto.admin.SignaturePreviewRequest;
import com.codeforge.web.dto.common.PageResponse;
import com.codeforge.web.dto.execution.RunRequest;
import com.codeforge.web.dto.execution.RunResponse;
import com.codeforge.web.mapper.AdminProblemMapper;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authoring the catalogue.
 *
 * <p>Everything under {@code /api/admin} is gated twice: by the filter chain in
 * {@code SecurityConfig}, and again by {@code hasRole('ADMIN')} on each service
 * method. The duplication is deliberate — the URL rule is easy to read and easy
 * to get wrong when a path is renamed, and the annotation travels with the code
 * that actually does the writing.
 */
@RestController
@RequestMapping("/api/admin/problems")
@RequiredArgsConstructor
public class AdminProblemController {

    private static final int MAX_PAGE_SIZE = 100;

    /** The same allow-list reasoning as the public catalogue, plus "recently touched". */
    private static final Map<String, String> SORT_PROPERTIES = Map.of(
            "id", "id",
            "title", "title",
            "difficulty", "difficultyRank",
            "updated", "updatedAt");

    private final ProblemAuthoringService authoringService;
    private final ExecutionService executionService;
    private final AdminProblemMapper adminProblemMapper;

    /** Drafts, published problems and archived ones, with the state as a filter. */
    @GetMapping
    public ResponseEntity<PageResponse<AdminProblemSummaryResponse>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Difficulty difficulty,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) ProblemState state,
            @RequestParam(defaultValue = "updated") String sort,
            @RequestParam(defaultValue = "desc") String order,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(Math.max(page, 0), clampSize(size), sortOf(sort, order));
        Page<Problem> problems =
                authoringService.search(blankToNull(search), difficulty, blankToNull(tag), state, pageable);

        List<Long> ids = problems.getContent().stream().map(Problem::getId).toList();
        Map<Long, TestCaseCounts> counts = authoringService.testCaseCounts(ids);
        Set<Long> withEditorial = authoringService.problemIdsWithEditorial(ids);

        List<AdminProblemSummaryResponse> rows = problems.getContent().stream()
                .map(problem -> adminProblemMapper.toSummary(
                        problem,
                        counts.getOrDefault(problem.getId(), new TestCaseCounts(0, 0)),
                        withEditorial.contains(problem.getId())))
                .toList();

        return ResponseEntity.ok(PageResponse.of(problems, rows));
    }

    /** One problem in full, keyed by id rather than slug — the slug is editable. */
    @GetMapping("/{id}")
    public ResponseEntity<AdminProblemDetailResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(adminProblemMapper.toDetail(authoringService.get(id)));
    }

    @PostMapping
    public ResponseEntity<AdminProblemDetailResponse> create(@RequestBody ProblemUpsertRequest request) {
        Long id = authoringService.create(request);

        return ResponseEntity.created(URI.create("/api/admin/problems/" + id))
                .body(adminProblemMapper.toDetail(authoringService.get(id)));
    }

    /**
     * Replaces the problem with the document the form sent.
     *
     * <p>Returns the saved state rather than an empty 204: the form has to adopt
     * the ids of rows it has just created, or the next save would insert them all
     * over again.
     */
    @PutMapping("/{id}")
    public ResponseEntity<AdminProblemDetailResponse> update(
            @PathVariable Long id, @RequestBody ProblemUpsertRequest request) {

        authoringService.update(id, request);

        return ResponseEntity.ok(adminProblemMapper.toDetail(authoringService.get(id)));
    }

    @PostMapping("/{id}/duplicate")
    public ResponseEntity<AdminProblemDetailResponse> duplicate(@PathVariable Long id) {
        Long copyId = authoringService.duplicate(id);

        return ResponseEntity.created(URI.create("/api/admin/problems/" + copyId))
                .body(adminProblemMapper.toDetail(authoringService.get(copyId)));
    }

    @PatchMapping("/{id}/published")
    public ResponseEntity<AdminProblemDetailResponse> setPublished(
            @PathVariable Long id, @RequestBody ProblemStateRequest request) {

        authoringService.setPublished(id, request.value());

        return ResponseEntity.ok(adminProblemMapper.toDetail(authoringService.get(id)));
    }

    @PatchMapping("/{id}/archived")
    public ResponseEntity<AdminProblemDetailResponse> setArchived(
            @PathVariable Long id, @RequestBody ProblemStateRequest request) {

        authoringService.setArchived(id, request.value());

        return ResponseEntity.ok(adminProblemMapper.toDetail(authoringService.get(id)));
    }

    /** Refused with a 409 once the problem has any history worth keeping. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        authoringService.delete(id);

        return ResponseEntity.noContent().build();
    }

    /**
     * Runs a reference solution against every case, including the hidden ones.
     *
     * <p>The check that turns a page of hand-typed expected outputs into a problem
     * anybody can be judged against fairly: if the author's own correct solution
     * does not pass, the cases are wrong, not the solver.
     */
    @PostMapping("/{id}/validate")
    public ResponseEntity<RunResponse> validate(@PathVariable Long id, @RequestBody RunRequest request) {
        String slug = authoringService.get(id).problem().getSlug();

        return ResponseEntity.ok(executionService.dryRun(slug, request.language(), request.sourceCode()));
    }

    /** The stubs a signature would generate, before it is saved. */
    @PostMapping("/starter-code")
    public ResponseEntity<Map<Language, String>> starterCode(@RequestBody SignaturePreviewRequest request) {
        return ResponseEntity.ok(authoringService.previewStarterCode(request));
    }

    private static Sort sortOf(String sort, String order) {
        String property = SORT_PROPERTIES.getOrDefault(sort.toLowerCase(Locale.ROOT), "updatedAt");
        Sort.Direction direction = "asc".equalsIgnoreCase(order) ? Sort.Direction.ASC : Sort.Direction.DESC;

        return "id".equals(property)
                ? Sort.by(direction, "id")
                : Sort.by(direction, property).and(Sort.by(Sort.Direction.ASC, "id"));
    }

    private static int clampSize(int size) {
        return Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
