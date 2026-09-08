package com.codeforge.web.dto.contest;

import com.codeforge.domain.Difficulty;
import com.codeforge.domain.Language;
import com.codeforge.web.dto.problem.ProblemExampleResponse;
import com.codeforge.web.dto.problem.TestCaseResponse;
import java.util.List;
import java.util.Map;

/**
 * A question as it appears inside the arena.
 *
 * <p>Every field comes off the frozen snapshot rather than the catalogue, which
 * is what makes an author's edit unable to change the question underneath a
 * field of competitors mid-contest.
 *
 * <p>Note what is missing next to {@code ProblemDetail}: no editorial, no topic
 * tags — "sliding window" above the statement is the answer to a good many
 * problems — no hints, and nothing about whether the caller has solved this
 * problem before. None of it is withheld by the client; the server does not send
 * it.
 *
 * @param counted false once the clock has run out, so the arena can say plainly
 *     that this attempt is practice and will not move the standings
 */
public record ContestProblemResponse(
        int position,
        String label,
        String slug,
        String title,
        Difficulty difficulty,
        int points,
        String description,
        String constraintsMarkdown,
        List<ProblemExampleResponse> examples,
        List<TestCaseResponse> sampleTestCases,
        int hiddenTestCaseCount,
        Map<Language, String> starterCode,
        boolean solved,
        int attempts,
        int wrongAttempts,
        boolean counted,
        long remainingSeconds,
        String submittedSourceCode,
        Language submittedLanguage) {}
