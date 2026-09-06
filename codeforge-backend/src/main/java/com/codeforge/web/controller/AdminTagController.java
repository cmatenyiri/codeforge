package com.codeforge.web.controller;

import com.codeforge.service.ProblemAuthoringService;
import com.codeforge.web.dto.admin.TagCreateRequest;
import com.codeforge.web.dto.problem.TagResponse;
import com.codeforge.web.mapper.ProblemMapper;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Adding a topic.
 *
 * <p>Only a create: reading the list is {@code /api/tags}, which every signed-in
 * caller already needs for the catalogue's filter, and there is nothing an admin
 * would see in it that a solver would not.
 */
@RestController
@RequestMapping("/api/admin/tags")
@RequiredArgsConstructor
public class AdminTagController {

    private final ProblemAuthoringService authoringService;
    private final ProblemMapper problemMapper;

    @PostMapping
    public ResponseEntity<TagResponse> create(@RequestBody TagCreateRequest request) {
        TagResponse tag = problemMapper.toTagResponse(authoringService.createTag(request));

        return ResponseEntity.created(URI.create("/api/tags/" + tag.slug())).body(tag);
    }
}
