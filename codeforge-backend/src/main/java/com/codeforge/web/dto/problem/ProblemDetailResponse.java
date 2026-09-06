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
 * @param sampleTestCases the visible cases, which "Run" judges against
 * @param hiddenTestCaseCount how many further cases a submission is judged
 *     against. Shown, not hidden: a solver should know that passing the samples
 *     is not the bar, and the count is not a hint about their content
 * @param hasEditorial whether a written solution exists, so the tab can be
 *     disabled rather than opening onto an apology
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
        int hiddenTestCaseCount,
        Map<Language, String> starterCode,
        boolean hasEditorial,
        boolean solved,
        boolean attempted,
        Double acceptanceRate,
        long totalSubmissions,
        long acceptedSubmissions) {}
