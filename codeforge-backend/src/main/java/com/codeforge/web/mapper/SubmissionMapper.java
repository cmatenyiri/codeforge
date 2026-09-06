package com.codeforge.web.mapper;

import com.codeforge.domain.Submission;
import com.codeforge.web.dto.submission.SubmissionDetailResponse;
import com.codeforge.web.dto.submission.SubmissionSummaryResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

/**
 * Submission → DTO.
 *
 * <p>Both shapes flatten the problem into the row, so a history renders without
 * a second lookup per entry; the repository fetch-joins it for the same reason.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SubmissionMapper {

    @Mapping(target = "problemSlug", source = "problem.slug")
    @Mapping(target = "problemTitle", source = "problem.title")
    @Mapping(target = "difficulty", source = "problem.difficulty")
    SubmissionSummaryResponse toSummary(Submission submission);

    @Mapping(target = "problemSlug", source = "problem.slug")
    @Mapping(target = "problemTitle", source = "problem.title")
    @Mapping(target = "difficulty", source = "problem.difficulty")
    SubmissionDetailResponse toDetail(Submission submission);
}
