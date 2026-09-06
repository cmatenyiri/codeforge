package com.codeforge.web.controller;

import com.codeforge.domain.Difficulty;
import com.codeforge.domain.Problem;
import com.codeforge.domain.ProblemStatusFilter;
import com.codeforge.execution.codegen.CodeTemplateService;
import com.codeforge.service.ProblemService;
import com.codeforge.web.dto.common.PageResponse;
import com.codeforge.web.dto.problem.EditorialResponse;
import com.codeforge.web.dto.problem.ProblemDetailResponse;
import com.codeforge.web.dto.problem.ProblemSummaryResponse;
import com.codeforge.web.mapper.ProblemMapper;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/problems")
@RequiredArgsConstructor
public class ProblemController {

    private static final int MAX_PAGE_SIZE = 100;

    /**
     * Sort keys the API accepts, mapped to the entity properties behind them.
     *
     * <p>An allow-list rather than a pass-through: {@code sort} reaches SQL as an
     * ORDER BY, so accepting an arbitrary string would let a caller order by —
     * and so probe — any column on the entity. It is also where the two keys that
     * are not plain columns are resolved: difficulty sorts by its rank, because
     * ordering the stored strings alphabetically would put HARD before MEDIUM,
     * and acceptance sorts by a ratio the database computes.
     */
    private static final Map<String, String> SORT_PROPERTIES = Map.of(
            "id", "id",
            "title", "title",
            "difficulty", "difficultyRank",
            "acceptance", "acceptanceRate");

    private final ProblemService problemService;
    private final ProblemMapper problemMapper;
    private final CodeTemplateService codeTemplateService;

    /**
     * The catalogue. Blank filters are normalised to null so an empty search box
     * behaves the same as no search box.
     */
    @GetMapping
    public ResponseEntity<PageResponse<ProblemSummaryResponse>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Difficulty difficulty,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) ProblemStatusFilter status,
            @RequestParam(defaultValue = "id") String sort,
            @RequestParam(defaultValue = "asc") String order,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(Math.max(page, 0), clampSize(size), sortOf(sort, order));
        Page<Problem> problems =
                problemService.search(blankToNull(search), difficulty, blankToNull(tag), status, pageable);

        return ResponseEntity.ok(PageResponse.of(problems, toSummaries(problems.getContent())));
    }

    /**
     * One problem at random from those matching the filters.
     *
     * <p>Declared before {@code /{slug}} matters not at all — Spring prefers the
     * literal path over the template — but a problem may never be slugged
     * "random", which {@code ProblemSeeder.slugify} cannot produce anyway.
     */
    @GetMapping("/random")
    public ResponseEntity<ProblemSummaryResponse> random(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Difficulty difficulty,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) ProblemStatusFilter status) {

        Problem problem = problemService.random(blankToNull(search), difficulty, blankToNull(tag), status);

        return ResponseEntity.ok(toSummaries(List.of(problem)).getFirst());
    }

    @GetMapping("/{slug}")
    public ResponseEntity<ProblemDetailResponse> getBySlug(@PathVariable String slug) {
        Problem problem = problemService.getBySlug(slug);
        boolean solved = problemService.solvedProblemIds().contains(problem.getId());
        boolean attempted = solved || problemService.attemptedProblemIds().contains(problem.getId());

        // Generated per request rather than stored: the stub is a pure function of
        // the signature, so there is nothing to invalidate when a generator changes.
        return ResponseEntity.ok(problemMapper.toDetail(
                problem,
                solved,
                attempted,
                codeTemplateService.starterCode(problem),
                problemMapper.toTestCaseResponses(problemService.visibleTestCases(problem.getId())),
                problemService.hiddenTestCaseCount(problem.getId()),
                problemService.hasEditorial(problem.getId())));
    }

    /**
     * The problem's written solution.
     *
     * <p>A separate call rather than part of the detail response: it is the
     * largest thing a problem owns and most visits never open the tab. A problem
     * with none is a 404, which the client renders as "not written yet".
     */
    @GetMapping("/{slug}/editorial")
    public ResponseEntity<EditorialResponse> editorial(@PathVariable String slug) {
        return ResponseEntity.ok(problemMapper.toEditorial(problemService.editorial(slug)));
    }

    /**
     * Two set lookups for a whole page, rather than a solved/attempted query per
     * row.
     */
    private List<ProblemSummaryResponse> toSummaries(List<Problem> problems) {
        Set<Long> solved = problemService.solvedProblemIds();
        Set<Long> attempted = problemService.attemptedProblemIds();

        return problems.stream()
                .map(problem -> problemMapper.toSummary(
                        problem, solved.contains(problem.getId()), attempted.contains(problem.getId())))
                .toList();
    }

    /**
     * An unknown sort key falls back to the catalogue's own order rather than
     * failing: a stale bookmark should still load the list.
     *
     * <p>Every sort is broken by id, so two problems with the same title — or the
     * same acceptance, which is common early on — keep a stable order across
     * pages instead of drifting between requests.
     */
    private static Sort sortOf(String sort, String order) {
        String property = SORT_PROPERTIES.getOrDefault(sort.toLowerCase(Locale.ROOT), "id");
        Sort.Direction direction =
                "desc".equalsIgnoreCase(order) ? Sort.Direction.DESC : Sort.Direction.ASC;

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
