package com.codeforge.web.dto.admin;

import com.codeforge.domain.DataType;
import com.codeforge.domain.Difficulty;
import java.util.List;

/**
 * The whole problem, as one write.
 *
 * <p>A create and an edit send the same shape, and the shape is the complete
 * problem rather than a patch: the author is editing one document across half a
 * dozen sections, and a partial payload would make "I deleted the last example"
 * indistinguishable from "I did not touch the examples".
 *
 * @param slug optional — derived from the title when blank, which is what an
 *     author wants until the day they need a URL that outlives a retitling
 * @param tagIds the topics, by id; a tag that does not exist yet is created
 *     through the tag endpoint first, so an unknown id here is a validation error
 * @param functionName null for a problem with no signature yet. A problem
 *     cannot be published without one — there would be no editor to solve it in
 * @param editorial null to leave the problem without a written solution, or to
 *     delete the one it has
 */
public record ProblemUpsertRequest(
        String title,
        String slug,
        Difficulty difficulty,
        String description,
        String constraintsMarkdown,
        boolean published,
        boolean archived,
        List<Long> tagIds,
        String functionName,
        DataType returnType,
        List<ProblemParameterPayload> parameters,
        List<ProblemExamplePayload> examples,
        List<String> hints,
        List<TestCasePayload> testCases,
        EditorialPayload editorial) {}
