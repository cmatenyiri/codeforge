package com.codeforge.web.controller;

import com.codeforge.domain.Difficulty;
import com.codeforge.domain.Problem;
import com.codeforge.execution.codegen.CodeTemplateService;
import com.codeforge.service.ProblemService;
import com.codeforge.web.dto.common.PageResponse;
import com.codeforge.web.dto.problem.ProblemDetailResponse;
import com.codeforge.web.dto.problem.ProblemSummaryResponse;
import com.codeforge.web.mapper.ProblemMapper;
import java.util.List;
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
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(Math.max(page, 0), clampSize(size), Sort.by(Sort.Direction.ASC, "id"));
        Page<Problem> problems = problemService.search(blankToNull(search), difficulty, blankToNull(tag), pageable);

        Set<Long> solved = problemService.solvedProblemIds();
        List<ProblemSummaryResponse> content = problems.getContent().stream()
                .map(problem -> problemMapper.toSummary(problem, solved.contains(problem.getId())))
                .toList();

        return ResponseEntity.ok(PageResponse.of(problems, content));
    }

    @GetMapping("/{slug}")
    public ResponseEntity<ProblemDetailResponse> getBySlug(@PathVariable String slug) {
        Problem problem = problemService.getBySlug(slug);
        boolean solved = problemService.solvedProblemIds().contains(problem.getId());

        // Generated per request rather than stored: the stub is a pure function of
        // the signature, so there is nothing to invalidate when a generator changes.
        return ResponseEntity.ok(
                problemMapper.toDetail(problem, solved, codeTemplateService.starterCode(problem)));
    }

    private static int clampSize(int size) {
        return Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
