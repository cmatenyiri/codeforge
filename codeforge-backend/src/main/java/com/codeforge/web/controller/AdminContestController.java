package com.codeforge.web.controller;

import com.codeforge.domain.Contest;
import com.codeforge.service.ContestAuthoringService;
import com.codeforge.service.ContestRejudgeService;
import com.codeforge.web.dto.common.PageResponse;
import com.codeforge.web.dto.contest.AdminContestDetailResponse;
import com.codeforge.web.dto.contest.AdminContestSummaryResponse;
import com.codeforge.web.dto.contest.ContestFlagRequest;
import com.codeforge.web.dto.contest.ContestRatedRequest;
import com.codeforge.web.dto.contest.ContestUpsertRequest;
import com.codeforge.web.mapper.ContestMapper;
import java.net.URI;
import java.time.Instant;
import java.util.List;
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
 * Running the contests: writing them, announcing them, and repairing them.
 *
 * <p>Gated twice, like everything under {@code /api/admin}: by the filter chain
 * in {@code SecurityConfig}, and again by {@code hasRole('ADMIN')} on each
 * service method. The duplication is deliberate — the URL rule is easy to read
 * and easy to get wrong when a path is renamed, and the annotation travels with
 * the code that actually does the writing.
 *
 * <p>The three endpoints at the bottom are the interesting ones, and they exist
 * because a contest is the one thing in this application that cannot simply be
 * edited after the fact. Once it has started, its questions are frozen and its
 * result is public; once it has been rated, that result is inside everybody's
 * rating. So repairing a broken contest is not an edit but one of three
 * deliberate acts: settle it, re-run it, or withdraw it.
 */
@RestController
@RequestMapping("/api/admin/contests")
@RequiredArgsConstructor
public class AdminContestController {

    private static final int MAX_PAGE_SIZE = 100;

    private final ContestAuthoringService authoringService;
    private final ContestRejudgeService rejudgeService;
    private final ContestMapper contestMapper;

    /** Every contest, drafts included, soonest start last. */
    @GetMapping
    public ResponseEntity<PageResponse<AdminContestSummaryResponse>> list(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Instant now = Instant.now();
        Page<Contest> contests = authoringService.search(blankToNull(search), pageable(page, size));

        List<AdminContestSummaryResponse> rows = contests.getContent().stream()
                .map(contest -> contestMapper.toAdminSummary(
                        contest, now, authoringService.counts(contest.getId())))
                .toList();

        return ResponseEntity.ok(PageResponse.of(contests, rows));
    }

    /** One contest in full, keyed by id rather than slug — the slug is editable. */
    @GetMapping("/{id}")
    public ResponseEntity<AdminContestDetailResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(detail(id));
    }

    @PostMapping
    public ResponseEntity<AdminContestDetailResponse> create(@RequestBody ContestUpsertRequest request) {
        Long id = authoringService.create(request);

        return ResponseEntity.created(URI.create("/api/admin/contests/" + id)).body(detail(id));
    }

    /**
     * Replaces the contest with the document the form sent.
     *
     * <p>Returns the saved state rather than an empty 204, so the form adopts the
     * slug that was derived and the points that were defaulted instead of
     * re-deriving them and drifting.
     */
    @PutMapping("/{id}")
    public ResponseEntity<AdminContestDetailResponse> update(
            @PathVariable Long id, @RequestBody ContestUpsertRequest request) {

        authoringService.update(id, request);

        return ResponseEntity.ok(detail(id));
    }

    /** Announces a contest, or takes the announcement back before it starts. */
    @PatchMapping("/{id}/published")
    public ResponseEntity<AdminContestDetailResponse> setPublished(
            @PathVariable Long id, @RequestBody ContestFlagRequest request) {

        authoringService.setPublished(id, request.value());

        return ResponseEntity.ok(detail(id));
    }

    /** Refused with a 409 once anybody has competed in it. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        authoringService.delete(id);

        return ResponseEntity.noContent().build();
    }

    // ── Settling and repairing ────────────────────────────────────────────

    /**
     * Freezes the places and applies the ratings.
     *
     * <p>A decision rather than a timer, deliberately. The window between a
     * contest ending and its ratings landing is when a broken test case gets
     * found, and rejudging a contest that has not been rated is far cheaper than
     * one that has — so a human confirms the result is worth acting on.
     */
    @PostMapping("/{id}/finalize")
    public ResponseEntity<AdminContestDetailResponse> finalizeContest(@PathVariable Long id) {
        authoringService.applyRatings(id);

        return ResponseEntity.ok(detail(id));
    }

    /**
     * Withdraws the contest's effect on everybody's rating, or restores it.
     *
     * <p>The repair for a round that turned out not to measure anything. Both
     * directions replay the rating ledger from this contest forward, because a
     * rating is a running total and every contest since was computed on top of
     * this one.
     */
    @PatchMapping("/{id}/rated")
    public ResponseEntity<AdminContestDetailResponse> setRated(
            @PathVariable Long id, @RequestBody ContestRatedRequest request) {

        authoringService.setRated(id, request.rated(), request.reason());

        return ResponseEntity.ok(detail(id));
    }

    /**
     * Re-runs every submission against the corrected problems.
     *
     * <p>Returns immediately with the contest marked as rejudging; the work
     * happens on a background worker and can take minutes. The screen polls
     * {@code GET /{id}} for the progress, which is why this hands back the same
     * shape rather than a job handle nothing else understands.
     */
    @PostMapping("/{id}/rejudge")
    public ResponseEntity<AdminContestDetailResponse> rejudge(@PathVariable Long id) {
        // Marked before the job is queued, so a screen that polls immediately
        // sees RUNNING rather than a state indistinguishable from nothing having
        // happened. The worker re-marks it with the real total once it knows how
        // many submissions there are.
        authoringService.markRejudgeRunning(id, 0);
        rejudgeService.rejudge(id);

        return ResponseEntity.ok(detail(id));
    }

    private AdminContestDetailResponse detail(Long id) {
        Contest contest = authoringService.get(id);

        return contestMapper.toAdminDetail(contest, Instant.now(), authoringService.counts(id));
    }

    private static Pageable pageable(int page, int size) {
        return PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                // Newest first, which for a contest means the one starting
                // soonest — an author is nearly always looking at what is next.
                Sort.by(Sort.Direction.DESC, "startsAt").and(Sort.by(Sort.Direction.DESC, "id")));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
