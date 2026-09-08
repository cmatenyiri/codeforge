package com.codeforge.web.controller;

import com.codeforge.domain.Contest;
import com.codeforge.domain.ContestParticipation;
import com.codeforge.domain.ContestRatingChange;
import com.codeforge.domain.Submission;
import com.codeforge.execution.codegen.CodeTemplateService;
import com.codeforge.execution.codegen.ProblemSignature;
import com.codeforge.service.ContestExecutionService;
import com.codeforge.service.ContestService;
import com.codeforge.service.ContestService.OpenProblem;
import com.codeforge.service.ContestService.SummaryContext;
import com.codeforge.security.SecurityUtils;
import com.codeforge.service.ContestStandingsService;
import com.codeforge.service.SubmissionService;
import com.codeforge.web.dto.common.PageResponse;
import com.codeforge.web.dto.contest.ContestDetailResponse;
import com.codeforge.web.dto.contest.ContestProblemResponse;
import com.codeforge.web.dto.contest.ContestRegistrationResponse;
import com.codeforge.web.dto.contest.ContestResultResponse;
import com.codeforge.web.dto.contest.ContestStandingsResponse;
import com.codeforge.web.dto.contest.ContestSummaryResponse;
import com.codeforge.web.dto.execution.RunRequest;
import com.codeforge.web.dto.execution.RunResponse;
import com.codeforge.web.dto.execution.SubmitRequest;
import com.codeforge.web.dto.submission.SubmissionResultResponse;
import com.codeforge.web.dto.submission.SubmissionSummaryResponse;
import com.codeforge.web.mapper.ContestMapper;
import com.codeforge.web.mapper.SubmissionMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Contests, as the people competing in them see one.
 *
 * <p>Note what is not here. There is no route that returns a problem before the
 * contest starts, no way to change a submission after it lands, and no way to
 * see anybody's code but your own. Those absences are the product: a contest is
 * a measurement, and every one of them is a way the measurement could be avoided.
 *
 * <p>The clock is entirely the server's. Every response recomputes the
 * countdowns from a single reading of it, and the browser ticking down between
 * polls decides nothing.
 */
@RestController
@RequestMapping("/api/contests")
@RequiredArgsConstructor
public class ContestController {

    private static final int MAX_PAGE_SIZE = 100;

    /** The standings are read far more than anything else here, so the page is generous. */
    private static final int DEFAULT_STANDINGS_SIZE = 25;

    private final ContestService contestService;
    private final ContestExecutionService contestExecutionService;
    private final ContestStandingsService standingsService;
    private final SubmissionService submissionService;
    private final CodeTemplateService codeTemplateService;
    private final ContestMapper contestMapper;
    private final SubmissionMapper submissionMapper;

    /** Announced contests, newest first. Past ones carry the caller's own result. */
    @GetMapping
    public ResponseEntity<PageResponse<ContestSummaryResponse>> list(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {

        Instant now = Instant.now();
        Page<Contest> contests = contestService.list(pageable(page, size));
        SummaryContext context = contestService.summaryContext(contests.getContent());

        return ResponseEntity.ok(PageResponse.of(contests, summaries(contests.getContent(), context, now)));
    }

    /**
     * What is about to happen, and what is happening now.
     *
     * <p>One call rather than two, because the lobby always wants both and the
     * answer to each is at most a handful of rows.
     */
    @GetMapping("/upcoming")
    public ResponseEntity<List<ContestSummaryResponse>> upcoming() {
        Instant now = Instant.now();
        List<Contest> contests = new ArrayList<>(contestService.running());
        contests.addAll(contestService.upcoming());

        return ResponseEntity.ok(summaries(contests, contestService.summaryContext(contests), now));
    }

    /**
     * One contest's page.
     *
     * <p>The call that seals a contest whose start time has passed — see
     * {@link ContestService#sealIfDue}. Everybody loading this at 10:00:00 races
     * to be the one that freezes the problems, and exactly one of them wins.
     */
    @GetMapping("/{slug}")
    public ResponseEntity<ContestDetailResponse> detail(@PathVariable String slug) {
        Instant now = Instant.now();
        Contest contest = contestService.getBySlug(slug);
        SummaryContext context = contestService.summaryContext(List.of(contest));

        return ResponseEntity.ok(contestMapper.toDetail(
                contest,
                now,
                context.isRegistered(contest.getId()),
                context.registrations(contest.getId()),
                context.participants(contest.getId()),
                standingsService.participationOf(contest.getId(), SecurityUtils.requireCurrentUserId()),
                contestService.myRatingChange(contest.getId()),
                // Only worth computing once the contest has started; before that
                // every question has been solved by nobody, which the client can
                // work out for itself.
                contest.hasStarted(now) ? standingsService.solveCounts(contest) : List.of()));
    }

    /** Signs the caller up. Idempotent — pressing it twice is not an error. */
    @PostMapping("/{slug}/register")
    public ResponseEntity<ContestRegistrationResponse> register(@PathVariable String slug) {
        contestService.register(slug);

        return ResponseEntity.ok(registrationState(slug));
    }

    /** Withdraws a registration. Refused once the contest is under way. */
    @DeleteMapping("/{slug}/register")
    public ResponseEntity<ContestRegistrationResponse> unregister(@PathVariable String slug) {
        contestService.unregister(slug);

        return ResponseEntity.ok(registrationState(slug));
    }

    // ── Inside the contest ────────────────────────────────────────────────

    /**
     * One question, from the frozen copy.
     *
     * <p>A 409 before the contest starts rather than a 404: the question exists
     * and the caller knows it exists — they can see it numbered on the contest
     * page — so pretending otherwise would only be confusing.
     */
    @GetMapping("/{slug}/problems/{position}")
    public ResponseEntity<ContestProblemResponse> problem(
            @PathVariable String slug, @PathVariable int position) {

        OpenProblem open = contestService.openProblem(slug, position);

        return ResponseEntity.ok(contestMapper.toProblem(
                open,
                // Generated from the signature frozen with the contest, so the
                // stub a competitor is given and the harness their submission is
                // compiled into can never disagree.
                codeTemplateService.starterCode(ProblemSignature.from(open.snapshot())),
                Instant.now()));
    }

    /** Runs against the sample cases. Nothing recorded, and it costs no penalty. */
    @PostMapping("/{slug}/problems/{position}/run")
    public ResponseEntity<RunResponse> run(
            @PathVariable String slug, @PathVariable int position, @RequestBody RunRequest request) {

        return ResponseEntity.ok(
                contestExecutionService.run(slug, position, request.language(), request.sourceCode()));
    }

    /**
     * Judges every case and scores it.
     *
     * <p>The attempt lands in the ordinary submission history too, and an
     * accepted one genuinely solves the problem in the catalogue: a contest is a
     * different way to be handed a problem, not a different kind of solving.
     */
    @PostMapping("/{slug}/problems/{position}/submit")
    public ResponseEntity<SubmissionResultResponse> submit(
            @PathVariable String slug, @PathVariable int position, @RequestBody SubmitRequest request) {

        return ResponseEntity.ok(
                contestExecutionService.submit(slug, position, request.language(), request.sourceCode()));
    }

    /** The caller's own attempts at one question, newest first. */
    @GetMapping("/{slug}/problems/{position}/submissions")
    public ResponseEntity<PageResponse<SubmissionSummaryResponse>> submissions(
            @PathVariable String slug,
            @PathVariable int position,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        OpenProblem open = contestService.openProblem(slug, position);
        Page<Submission> submissions = submissionService.listMineForContestProblem(
                open.contestProblem().getId(), pageable(page, size));

        return ResponseEntity.ok(PageResponse.of(
                submissions, submissions.getContent().stream().map(submissionMapper::toSummary).toList()));
    }

    // ── The scoreboard ────────────────────────────────────────────────────

    /**
     * The standings.
     *
     * <p>The caller's own row travels with every page. Somebody in 812th place
     * opens this to find themselves, and making them page through eight screens
     * for it would be a strange thing to ask.
     */
    @GetMapping("/{slug}/standings")
    public ResponseEntity<ContestStandingsResponse> standings(
            @PathVariable String slug,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + DEFAULT_STANDINGS_SIZE) int size) {

        Instant now = Instant.now();
        Contest contest = contestService.getBySlug(slug);

        Page<ContestParticipation> standings = standingsService.standings(contest, pageable(page, size));
        SummaryContext context = contestService.summaryContext(List.of(contest));
        Optional<ContestParticipation> mine =
                standingsService.participationOf(contest.getId(), SecurityUtils.requireCurrentUserId());

        // Batched rather than looked up per row: the standings are the most-read
        // page a contest has, and a query per competitor would be twenty-five of
        // them per screen.
        Map<Long, ContestRatingChange> ratingChanges = contestService.ratingChangesFor(
                contest.getId(),
                standings.getContent().stream()
                        .map(participation -> participation.getUser().getId())
                        .toList());

        List<ContestResultResponse> rows = standings.getContent().stream()
                .map(participation -> contestMapper.toResult(
                        participation,
                        Optional.ofNullable(ratingChanges.get(participation.getUser().getId()))))
                .toList();

        return ResponseEntity.ok(new ContestStandingsResponse(
                PageResponse.of(standings, rows),
                mine.map(participation ->
                                contestMapper.toResult(participation, contestService.myRatingChange(contest.getId())))
                        .orElse(null),
                standingsService.solveCounts(contest),
                contest.hasEnded(now),
                contest.isRated(),
                contest.getUnratedReason()));
    }

    // ── Internals ─────────────────────────────────────────────────────────

    private List<ContestSummaryResponse> summaries(
            List<Contest> contests, SummaryContext context, Instant now) {

        return contests.stream()
                .map(contest -> contestMapper.toSummary(
                        contest,
                        now,
                        context.isRegistered(contest.getId()),
                        context.registrations(contest.getId()),
                        context.participants(contest.getId()),
                        context.participation(contest.getId()),
                        context.rating(contest.getId()),
                        context.problems(contest.getId())))
                .toList();
    }

    private ContestRegistrationResponse registrationState(String slug) {
        Contest contest = contestService.getBySlug(slug);

        return new ContestRegistrationResponse(
                contestService.isRegistered(contest.getId()),
                contestService.registrationCount(contest.getId()));
    }

    private static Pageable pageable(int page, int size) {
        // Ordering is fixed by the queries — a contest list is newest-first and
        // standings are by score — so the Pageable carries no Sort of its own.
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE));
    }
}
