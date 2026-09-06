package com.codeforge.web.mapper;

import com.codeforge.domain.Editorial;
import com.codeforge.domain.Language;
import com.codeforge.domain.Problem;
import com.codeforge.domain.ProblemExample;
import com.codeforge.domain.ProblemHint;
import com.codeforge.domain.Tag;
import com.codeforge.domain.TestCase;
import com.codeforge.web.dto.problem.EditorialResponse;
import com.codeforge.web.dto.problem.ProblemDetailResponse;
import com.codeforge.web.dto.problem.ProblemExampleResponse;
import com.codeforge.web.dto.problem.ProblemSummaryResponse;
import com.codeforge.web.dto.problem.TagResponse;
import com.codeforge.web.dto.problem.TestCaseResponse;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

/**
 * Entity → DTO for the catalogue and the solving page.
 *
 * <p>{@code solved}, {@code attempted} and {@code starterCode} are not derivable
 * from a Problem alone, so they are passed in by the controller rather than
 * mapped.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ProblemMapper {

    TagResponse toTagResponse(Tag tag);

    List<TagResponse> toTagResponses(List<Tag> tags);

    ProblemExampleResponse toExampleResponse(ProblemExample example);

    TestCaseResponse toTestCaseResponse(TestCase testCase);

    @Mapping(target = "solved", source = "solved")
    @Mapping(target = "attempted", source = "attempted")
    ProblemSummaryResponse toSummary(Problem problem, boolean solved, boolean attempted);

    /**
     * The sample cases and the hidden count are passed in rather than read off
     * the entity: {@code ProblemService} loads them separately so that a page
     * view never pulls the hidden inputs, which can be very large.
     */
    @Mapping(target = "solved", source = "solved")
    @Mapping(target = "attempted", source = "attempted")
    @Mapping(target = "starterCode", source = "starterCode")
    @Mapping(target = "hints", source = "problem.hints", qualifiedByName = "hintContents")
    @Mapping(target = "sampleTestCases", source = "sampleTestCases")
    @Mapping(target = "hiddenTestCaseCount", source = "hiddenTestCaseCount")
    @Mapping(target = "hasEditorial", source = "hasEditorial")
    ProblemDetailResponse toDetail(
            Problem problem,
            boolean solved,
            boolean attempted,
            Map<Language, String> starterCode,
            List<TestCaseResponse> sampleTestCases,
            int hiddenTestCaseCount,
            boolean hasEditorial);

    @Mapping(target = "solutions", source = "solutions", qualifiedByName = "byLanguageOrder")
    EditorialResponse toEditorial(Editorial editorial);

    /**
     * Hibernate hands back a plain map, whose iteration order is its own business.
     * The client renders the language picker in key order, so it is settled here
     * rather than left to look arbitrary.
     */
    @Named("byLanguageOrder")
    default Map<Language, String> byLanguageOrder(Map<Language, String> solutions) {
        // Built and filled rather than copy-constructed: EnumMap's copy
        // constructor cannot infer the key type from an empty non-EnumMap and
        // throws, which an editorial authored in no languages would hit.
        Map<Language, String> ordered = new EnumMap<>(Language.class);
        ordered.putAll(solutions);
        return ordered;
    }

    @Named("hintContents")
    default List<String> hintContents(List<ProblemHint> hints) {
        return hints.stream().map(ProblemHint::getContent).toList();
    }

    /** Hidden cases are the graded ones; exposing them would give away the answers. */
    default List<TestCaseResponse> toTestCaseResponses(List<TestCase> testCases) {
        return testCases.stream().map(this::toTestCaseResponse).toList();
    }
}
