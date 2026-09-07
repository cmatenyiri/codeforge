package com.codeforge.web.controller;

import com.codeforge.domain.Interview;
import com.codeforge.domain.InterviewFormat;
import com.codeforge.execution.codegen.CodeTemplateService;
import com.codeforge.execution.codegen.ProblemSignature;
import com.codeforge.service.InterviewExecutionService;
import com.codeforge.service.InterviewService;
import com.codeforge.service.InterviewService.OpenSlot;
import com.codeforge.service.InterviewService.RevealedHints;
import com.codeforge.web.dto.common.PageResponse;
import com.codeforge.web.dto.execution.RunRequest;
import com.codeforge.web.dto.execution.RunResponse;
import com.codeforge.web.dto.execution.SubmitRequest;
import com.codeforge.web.dto.interview.InterviewFormatResponse;
import com.codeforge.web.dto.interview.InterviewHintResponse;
import com.codeforge.web.dto.interview.InterviewProblemResponse;
import com.codeforge.web.dto.interview.InterviewReportResponse;
import com.codeforge.web.dto.interview.InterviewSessionResponse;
import com.codeforge.web.dto.interview.InterviewSummaryResponse;
import com.codeforge.web.dto.interview.SelfReportRequest;
import com.codeforge.web.dto.interview.StartInterviewRequest;
import com.codeforge.web.dto.submission.SubmissionResultResponse;
import com.codeforge.web.mapper.InterviewMapper;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The mock interview: starting one, working through it against the clock, and
 * reading the debrief afterwards.
 *
 * <p>Everything is scoped to the caller inside the queries, as elsewhere — no
 * endpoint here takes a user id.
 *
 * <p>Note what is missing. There is no route to an editorial, no way to ask for
 * a specific problem, and no way to see the whole set's descriptions at once.
 * Those absences are the feature: a round you chose the questions for, with the
 * written solution one tab away, is practice with a countdown on it.
 */
@RestController
@RequestMapping("/api/interviews")
@RequiredArgsConstructor
public class InterviewController {

    private static final int MAX_PAGE_SIZE = 50;

    private final InterviewService interviewService;
    private final InterviewExecutionService interviewExecutionService;
    private final CodeTemplateService codeTemplateService;
    private final InterviewMapper interviewMapper;

    /** The formats on offer, with the shape of each so the lobby need not hard-code it. */
    @GetMapping("/formats")
    public ResponseEntity<List<InterviewFormatResponse>> formats() {
        return ResponseEntity.ok(
                Arrays.stream(InterviewFormat.values()).map(InterviewFormatResponse::of).toList());
    }

    /**
     * Starts a round.
     *
     * <p>409 when one is already running: resuming it is the only sane reading of
     * a second start, and silently abandoning the first would throw away work.
     */
    @PostMapping
    public ResponseEntity<InterviewSessionResponse> start(@RequestBody StartInterviewRequest request) {
        Interview interview = interviewService.start(request.format());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(interviewMapper.toSession(interview, Instant.now()));
    }

    /** The caller's running interview, or 404 — what the lobby asks on load. */
    @GetMapping("/current")
    public ResponseEntity<InterviewSessionResponse> current() {
        return ResponseEntity.ok(interviewMapper.toSession(interviewService.current(), Instant.now()));
    }

    /** Past rounds, newest first. */
    @GetMapping
    public ResponseEntity<PageResponse<InterviewSummaryResponse>> history(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {

        Page<Interview> interviews = interviewService.history(pageable(page, size));
        List<InterviewSummaryResponse> rows =
                interviews.getContent().stream().map(interviewMapper::toSummary).toList();

        return ResponseEntity.ok(PageResponse.of(interviews, rows));
    }

    /**
     * The session state, polled by the session screen.
     *
     * <p>{@code remainingSeconds} is recomputed here on every call and is the
     * only clock that decides anything; the browser counts down from it so the
     * timer moves smoothly between polls. A round whose time ran out comes back
     * already finished, which is the client's cue to go to the report.
     */
    @GetMapping("/{id}")
    public ResponseEntity<InterviewSessionResponse> session(@PathVariable Long id) {
        return ResponseEntity.ok(interviewMapper.toSession(interviewService.load(id), Instant.now()));
    }

    /** The debrief. Available while a round is still running, and honest about it. */
    @GetMapping("/{id}/report")
    public ResponseEntity<InterviewReportResponse> report(@PathVariable Long id) {
        return ResponseEntity.ok(toReport(interviewService.load(id)));
    }

    /** Ends the round early and returns its debrief. */
    @PostMapping("/{id}/finish")
    public ResponseEntity<InterviewReportResponse> finish(@PathVariable Long id) {
        return ResponseEntity.ok(toReport(interviewService.finish(id)));
    }

    /**
     * Throws the round away.
     *
     * <p>Not the same as finishing: a round quit five minutes in should not sit
     * in the history as a failed one.
     */
    @PostMapping("/{id}/abandon")
    public ResponseEntity<InterviewSummaryResponse> abandon(@PathVariable Long id) {
        return ResponseEntity.ok(interviewMapper.toSummary(interviewService.abandon(id)));
    }

    /** The honesty toggle on the report. Changes no score and gates nothing. */
    @PatchMapping("/{id}/self-report")
    public ResponseEntity<InterviewReportResponse> selfReport(
            @PathVariable Long id, @RequestBody SelfReportRequest request) {

        return ResponseEntity.ok(toReport(interviewService.recordSelfReport(id, request.usedOutsideHelp())));
    }

    // ── Inside the round ──────────────────────────────────────────────────

    /**
     * One problem of the set.
     *
     * <p>A GET that writes: the first fetch is when this problem's clock starts,
     * and a separate "I have opened it" call would be one more round trip and one
     * more thing for a client to forget. The stamp is set once, so returning to a
     * problem does not reset what it has already cost.
     */
    @GetMapping("/{id}/problems/{position}")
    public ResponseEntity<InterviewProblemResponse> problem(
            @PathVariable Long id, @PathVariable int position) {

        OpenSlot slot = interviewService.openSlot(id, position);
        List<String> hints = slot.snapshot().hints();

        return ResponseEntity.ok(interviewMapper.toProblem(
                slot,
                // Generated from the signature frozen with the round, so the stub
                // the candidate is given and the harness their submission is
                // compiled into can never disagree.
                codeTemplateService.starterCode(ProblemSignature.from(slot.snapshot())),
                // Only the prefix the candidate has paid for. The rest never
                // leaves the server, so nothing on the client is trusted to hide
                // them.
                hints.subList(0, Math.min(slot.hintsRevealed(), hints.size()))));
    }

    /** Opens the next hint and counts it against the round. */
    @PostMapping("/{id}/problems/{position}/hints")
    public ResponseEntity<InterviewHintResponse> revealHint(
            @PathVariable Long id, @PathVariable int position) {

        RevealedHints revealed = interviewService.revealNextHint(id, position);

        return ResponseEntity.ok(
                new InterviewHintResponse(revealed.available(), revealed.revealed(), revealed.hints()));
    }

    /** Marks a problem passed over. Reversible by solving it. */
    @PostMapping("/{id}/problems/{position}/skip")
    public ResponseEntity<InterviewSessionResponse> skip(@PathVariable Long id, @PathVariable int position) {
        return ResponseEntity.ok(
                interviewMapper.toSession(interviewService.skip(id, position), Instant.now()));
    }

    /** Runs against the sample cases. Nothing recorded, but the clock still has to be running. */
    @PostMapping("/{id}/problems/{position}/run")
    public ResponseEntity<RunResponse> run(
            @PathVariable Long id, @PathVariable int position, @RequestBody RunRequest request) {

        return ResponseEntity.ok(interviewExecutionService.run(
                id, position, request.language(), request.sourceCode()));
    }

    /**
     * Judges every case and attributes the verdict to the slot.
     *
     * <p>The attempt lands in the ordinary submission history too: an interview
     * is a different way to be handed a problem, not a different kind of solving.
     */
    @PostMapping("/{id}/problems/{position}/submit")
    public ResponseEntity<SubmissionResultResponse> submit(
            @PathVariable Long id, @PathVariable int position, @RequestBody SubmitRequest request) {

        return ResponseEntity.ok(interviewExecutionService.submit(
                id, position, request.language(), request.sourceCode()));
    }

    private InterviewReportResponse toReport(Interview interview) {
        return interviewMapper.toReport(interview, interviewService.insights(interview), Instant.now());
    }

    private static Pageable pageable(int page, int size) {
        // Ordering is fixed by the query — newest first is the only order a
        // history is read in — so the Pageable carries no Sort of its own.
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE));
    }
}
