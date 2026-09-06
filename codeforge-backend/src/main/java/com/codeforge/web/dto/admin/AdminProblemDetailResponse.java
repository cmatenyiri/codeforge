package com.codeforge.web.dto.admin;

import com.codeforge.domain.DataType;
import com.codeforge.domain.Difficulty;
import com.codeforge.domain.Language;
import com.codeforge.domain.ProblemState;
import com.codeforge.web.dto.problem.TagResponse;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * A problem in full, for the editing form.
 *
 * <p>The one response that carries the hidden test cases: an author has to see
 * what a submission is judged against, and this endpoint is behind
 * {@code hasRole('ADMIN')} precisely so that nothing else has to.
 *
 * @param starterCode generated from the signature as it is stored, so the author
 *     sees the stub a solver would get without having to save and open the
 *     solving page
 * @param totalSubmissions how much this problem has been used, which is what
 *     makes an edit to its test cases consequential rather than free
 */
public record AdminProblemDetailResponse(
        Long id,
        String slug,
        String title,
        Difficulty difficulty,
        String description,
        String constraintsMarkdown,
        ProblemState state,
        boolean published,
        boolean archived,
        List<TagResponse> tags,
        String functionName,
        DataType returnType,
        List<ProblemParameterPayload> parameters,
        List<ProblemExamplePayload> examples,
        List<String> hints,
        List<TestCasePayload> testCases,
        EditorialPayload editorial,
        Map<Language, String> starterCode,
        long totalSubmissions,
        long acceptedSubmissions,
        Instant createdAt,
        Instant updatedAt) {}
