package com.codeforge.web.dto.contest;

import com.codeforge.domain.Difficulty;
import com.codeforge.domain.ProblemState;

/**
 * One question of a contest, as the authoring form reads it back.
 *
 * @param state the catalogue state of the problem behind it. Shown because a
 *     contest question is normally a draft — a published one is sitting in the
 *     catalogue with its editorial next to it, and anybody could read the answer
 *     before the contest starts — so the form flags that rather than refusing it
 * @param solvable whether the problem has a signature and cases at all; without
 *     them the arena renders an editor that cannot run
 */
public record AdminContestProblemResponse(
        int position,
        String label,
        Long problemId,
        String slug,
        String title,
        Difficulty difficulty,
        ProblemState state,
        int points,
        boolean solvable,
        int testCaseCount) {}
