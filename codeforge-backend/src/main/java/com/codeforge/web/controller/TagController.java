package com.codeforge.web.controller;

import com.codeforge.service.TagService;
import com.codeforge.web.dto.problem.TagResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;
    private final com.codeforge.web.mapper.ProblemMapper problemMapper;

    @GetMapping
    public ResponseEntity<List<TagResponse>> list() {
        return ResponseEntity.ok(
                tagService.findAll().stream().map(problemMapper::toTagResponse).toList());
    }
}
