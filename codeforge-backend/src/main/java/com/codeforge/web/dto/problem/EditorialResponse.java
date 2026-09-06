package com.codeforge.web.dto.problem;

import com.codeforge.domain.Language;
import java.util.Map;

/**
 * A problem's written solution.
 *
 * @param contentMarkdown the walkthrough — approach, why the obvious attempt
 *     falls short, and what the edge cases are
 * @param solutions reference code per language; the keys are the languages the
 *     editorial was authored in, which is what the client's picker offers
 */
public record EditorialResponse(
        String contentMarkdown,
        String timeComplexity,
        String spaceComplexity,
        Map<Language, String> solutions) {}
