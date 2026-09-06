package com.codeforge.web.controller;

import com.codeforge.domain.Submission;
import com.codeforge.service.SubmissionService;
import com.codeforge.web.dto.common.PageResponse;
import com.codeforge.web.dto.submission.SubmissionDetailResponse;
import com.codeforge.web.dto.submission.SubmissionSummaryResponse;
import com.codeforge.web.mapper.SubmissionMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * A user's own submission history.
 *
 * <p>Everything here is scoped to the caller in the query itself — there is no
 * endpoint that takes a user id, so there is nothing to get wrong about whose
 * code is being returned.
 */
@RestController
@RequiredArgsConstructor
public class SubmissionController {

    private static final int MAX_PAGE_SIZE = 50;

    private final SubmissionService submissionService;
    private final SubmissionMapper submissionMapper;

    /** The caller's whole history, newest first — the profile's activity list. */
    @GetMapping("/api/submissions")
    public ResponseEntity<PageResponse<SubmissionSummaryResponse>> listMine(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {

        Page<Submission> submissions = submissionService.listMine(pageable(page, size));

        return ResponseEntity.ok(PageResponse.of(submissions, toSummaries(submissions)));
    }

    /** The caller's history for one problem — the editor's "Submissions" tab. */
    @GetMapping("/api/problems/{slug}/submissions")
    public ResponseEntity<PageResponse<SubmissionSummaryResponse>> listMineForProblem(
            @PathVariable String slug,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<Submission> submissions = submissionService.listMineForProblem(slug, pageable(page, size));

        return ResponseEntity.ok(PageResponse.of(submissions, toSummaries(submissions)));
    }

    /** One submission with the source that produced it. */
    @GetMapping("/api/submissions/{id}")
    public ResponseEntity<SubmissionDetailResponse> getMine(@PathVariable Long id) {
        return ResponseEntity.ok(submissionMapper.toDetail(submissionService.getMine(id)));
    }

    private List<SubmissionSummaryResponse> toSummaries(Page<Submission> submissions) {
        return submissions.getContent().stream().map(submissionMapper::toSummary).toList();
    }

    private static Pageable pageable(int page, int size) {
        // Ordering is fixed by the query — newest first is the only order a
        // history is ever read in — so the Pageable carries no Sort of its own.
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE));
    }
}
