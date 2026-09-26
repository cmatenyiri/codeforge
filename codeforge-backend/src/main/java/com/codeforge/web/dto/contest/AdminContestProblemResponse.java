package com.codeforge.web.dto.contest;

import com.codeforge.domain.Difficulty;
import com.codeforge.domain.ProblemState;

/**
 * One question of a contest, as the authoring form reads it back.
 *
 * @param state the catalogue state of the problem behind it: a draft or, once
 *     the contest is announced, held by it. Shown because a published one is
 *     sitting in the catalogue with its editorial next to it, where anybody
 *     could read the answer before the contest starts — the form flags that
 *     while the contest is a draft, and announcing refuses it
 * @param solvable whether the problem has a signature and cases at all; without
 *     them the arena renders an editor that cannot run
 * @param everPublished whether the problem has ever been public, which rules it
 *     out of a contest even when it is a draft again
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
        int testCaseCount,
        boolean everPublished) {}
