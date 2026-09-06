package com.codeforge.web.controller;

import com.codeforge.service.ExecutionService;
import com.codeforge.web.dto.execution.RunRequest;
import com.codeforge.web.dto.execution.RunResponse;
import com.codeforge.web.dto.execution.SubmitRequest;
import com.codeforge.web.dto.submission.SubmissionResultResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/problems/{slug}")
@RequiredArgsConstructor
public class ExecutionController {

    private final ExecutionService executionService;

    /**
     * Runs the editor's code against the problem's sample cases.
     *
     * <p>Nothing is recorded: this is the fast feedback loop, not a submission.
     * A judge failure surfaces as a 409 through {@code BusinessRuleException}
     * rather than a 500, because the request was fine — the sandbox was not.
     */
    @PostMapping("/run")
    public ResponseEntity<RunResponse> run(@PathVariable String slug, @RequestBody RunRequest request) {
        return ResponseEntity.ok(executionService.run(slug, request.language(), request.sourceCode()));
    }

    /**
     * Judges the code against every case, hidden ones included, and records the
     * verdict against the caller.
     *
     * <p>Only a submission can mark a problem solved. Hidden cases come back with
     * a status and no contents — the count of them is public, what is in them is
     * not.
     */
    @PostMapping("/submit")
    public ResponseEntity<SubmissionResultResponse> submit(
            @PathVariable String slug, @RequestBody SubmitRequest request) {
        return ResponseEntity.ok(executionService.submit(slug, request.language(), request.sourceCode()));
    }
}
