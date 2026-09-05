package com.codeforge.web.controller;

import com.codeforge.service.ExecutionService;
import com.codeforge.web.dto.execution.RunRequest;
import com.codeforge.web.dto.execution.RunResponse;
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
}
