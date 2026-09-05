package com.codeforge.web.dto.problem;

import com.codeforge.domain.Difficulty;
import com.codeforge.domain.Language;
import java.util.List;
import java.util.Map;

/**
 * Everything the solving page needs to render a problem.
 *
 * @param starterCode the editable stub per language, generated from the
 *     problem's signature. Empty when the problem has no signature authored yet,
 *     which the editor reads as "not solvable here" — the keys double as the
 *     list of languages the language picker should offer
 */
public record ProblemDetailResponse(
        Long id,
        String slug,
        String title,
        Difficulty difficulty,
        String description,
        String constraintsMarkdown,
        List<TagResponse> tags,
        List<ProblemExampleResponse> examples,
        List<String> hints,
        List<TestCaseResponse> sampleTestCases,
        Map<Language, String> starterCode,
        boolean solved) {}
