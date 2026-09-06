package com.codeforge.web.mapper;

import com.codeforge.domain.Editorial;
import com.codeforge.domain.Problem;
import com.codeforge.domain.ProblemHint;
import com.codeforge.domain.ProblemState;
import com.codeforge.service.ProblemAuthoringService.Authored;
import com.codeforge.service.ProblemAuthoringService.TestCaseCounts;
import com.codeforge.web.dto.admin.AdminProblemDetailResponse;
import com.codeforge.web.dto.admin.AdminProblemSummaryResponse;
import com.codeforge.web.dto.admin.EditorialPayload;
import com.codeforge.web.dto.admin.ProblemExamplePayload;
import com.codeforge.web.dto.admin.ProblemParameterPayload;
import com.codeforge.web.dto.admin.TestCasePayload;
import com.codeforge.web.dto.problem.TagResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Entity → DTO for the authoring screens.
 *
 * <p>Hand-written rather than generated, unlike {@link ProblemMapper}: the
 * responses here are round-trip shapes — what the form renders is what it sends
 * back — so every child carries the id that lets a save match it to a stored row,
 * and a declarative mapper would need an annotation per field to say so anyway.
 */
@Component
@RequiredArgsConstructor
public class AdminProblemMapper {

    private final ProblemMapper problemMapper;

    public AdminProblemSummaryResponse toSummary(Problem problem, TestCaseCounts counts, boolean hasEditorial) {
        return new AdminProblemSummaryResponse(
                problem.getId(),
                problem.getSlug(),
                problem.getTitle(),
                problem.getDifficulty(),
                ProblemState.of(problem),
                tags(problem),
                counts.total(),
                counts.samples(),
                hasEditorial,
                problem.getFunctionName() != null && problem.getReturnType() != null,
                problem.getTotalSubmissions(),
                problem.getAcceptanceRate(),
                problem.getUpdatedAt());
    }

    public AdminProblemDetailResponse toDetail(Authored authored) {
        Problem problem = authored.problem();

        return new AdminProblemDetailResponse(
                problem.getId(),
                problem.getSlug(),
                problem.getTitle(),
                problem.getDifficulty(),
                problem.getDescription(),
                problem.getConstraintsMarkdown(),
                ProblemState.of(problem),
                problem.isPublished(),
                problem.isArchived(),
                tags(problem),
                problem.getFunctionName(),
                problem.getReturnType(),
                problem.getParameters().stream()
                        .map(parameter -> new ProblemParameterPayload(parameter.getName(), parameter.getType()))
                        .toList(),
                problem.getExamples().stream()
                        .map(example -> new ProblemExamplePayload(
                                example.getId(), example.getInput(), example.getOutput(), example.getExplanation()))
                        .toList(),
                problem.getHints().stream().map(ProblemHint::getContent).toList(),
                problem.getTestCases().stream()
                        .map(testCase -> new TestCasePayload(
                                testCase.getId(),
                                testCase.getInput(),
                                testCase.getExpectedOutput(),
                                testCase.isHidden()))
                        .toList(),
                toEditorialPayload(authored.editorial()),
                authored.starterCode(),
                problem.getTotalSubmissions(),
                problem.getAcceptedSubmissions(),
                problem.getCreatedAt(),
                problem.getUpdatedAt());
    }

    private EditorialPayload toEditorialPayload(Editorial editorial) {
        return editorial == null
                ? null
                : new EditorialPayload(
                        editorial.getContentMarkdown(),
                        editorial.getTimeComplexity(),
                        editorial.getSpaceComplexity(),
                        problemMapper.byLanguageOrder(editorial.getSolutions()));
    }

    private List<TagResponse> tags(Problem problem) {
        return problem.getTags().stream().map(problemMapper::toTagResponse).toList();
    }
}
