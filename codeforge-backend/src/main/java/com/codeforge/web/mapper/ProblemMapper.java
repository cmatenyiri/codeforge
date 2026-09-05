package com.codeforge.web.mapper;

import com.codeforge.domain.Language;
import com.codeforge.domain.Problem;
import com.codeforge.domain.ProblemExample;
import com.codeforge.domain.ProblemHint;
import com.codeforge.domain.Tag;
import com.codeforge.domain.TestCase;
import com.codeforge.web.dto.problem.ProblemDetailResponse;
import com.codeforge.web.dto.problem.ProblemExampleResponse;
import com.codeforge.web.dto.problem.ProblemSummaryResponse;
import com.codeforge.web.dto.problem.TagResponse;
import com.codeforge.web.dto.problem.TestCaseResponse;
import java.util.List;
import java.util.Map;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

/**
 * Entity → DTO for the catalogue and the solving page.
 *
 * <p>{@code solved} and {@code starterCode} are not derivable from a Problem
 * alone, so they are passed in by the controller rather than mapped.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ProblemMapper {

    TagResponse toTagResponse(Tag tag);

    List<TagResponse> toTagResponses(List<Tag> tags);

    ProblemExampleResponse toExampleResponse(ProblemExample example);

    TestCaseResponse toTestCaseResponse(TestCase testCase);

    @Mapping(target = "solved", source = "solved")
    ProblemSummaryResponse toSummary(Problem problem, boolean solved);

    @Mapping(target = "solved", source = "solved")
    @Mapping(target = "starterCode", source = "starterCode")
    @Mapping(target = "hints", source = "problem.hints", qualifiedByName = "hintContents")
    @Mapping(target = "sampleTestCases", source = "problem.testCases", qualifiedByName = "visibleTestCases")
    ProblemDetailResponse toDetail(Problem problem, boolean solved, Map<Language, String> starterCode);

    @Named("hintContents")
    default List<String> hintContents(List<ProblemHint> hints) {
        return hints.stream().map(ProblemHint::getContent).toList();
    }

    /** Hidden cases are the graded ones; exposing them would give away the answers. */
    @Named("visibleTestCases")
    default List<TestCaseResponse> visibleTestCases(List<TestCase> testCases) {
        return testCases.stream()
                .filter(testCase -> !testCase.isHidden())
                .map(this::toTestCaseResponse)
                .toList();
    }
}
