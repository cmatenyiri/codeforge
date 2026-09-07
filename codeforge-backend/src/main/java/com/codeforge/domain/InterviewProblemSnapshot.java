package com.codeforge.domain;

import java.util.List;

/**
 * A problem exactly as it stood when a round began, frozen onto the slot.
 *
 * <p>The reason this exists is that an author may edit a problem while somebody
 * is halfway through solving it. The dangerous edit is the signature: the
 * candidate's editor holds starter code generated when they opened the slot, and
 * the harness is built from the problem's current signature — so renaming
 * {@code isValid} to {@code isValidTest} mid-round would compile the candidate's
 * own correct answer against a harness calling a function they were never shown,
 * fail every case, count the attempt against them and record the slot as failed.
 * Nothing on the screen would explain why. Test-case edits are the same fault in
 * a quieter register: code accepted at 10:05 fails at 10:07.
 *
 * <p>So the round stops reading the live row. This is the same move {@link
 * Interview#getDurationMinutes()} already makes for the clock — copied at the
 * start rather than referenced, so that a later change cannot rewrite a round
 * somebody has already sat — applied to the thing the verdict actually depends
 * on.
 *
 * <p>Everything a round needs is here and nothing else is. Notably absent: the
 * editorial, which is the answer key and is unreachable during a round anyway;
 * the tags, which name the technique and would give the question away; and the
 * submission counters, which are live catalogue state that no snapshot should
 * pin. The problem's own id is kept because a submission is still recorded
 * against the catalogue — an interview is a different way to be handed a
 * problem, not a different kind of solving.
 *
 * <p>Stored as JSON in one column by {@link InterviewProblemSnapshotConverter}.
 * A separate versions table would dedupe the test-case blobs across rounds that
 * drew the same problem, and is where this goes if those blobs ever get large
 * enough to matter; the read sites would not change, only where the record is
 * loaded from.
 *
 * @param problemId the catalogue row this was taken from, for recording
 *     submissions and linking to the problem page afterwards
 * @param testCases every case, hidden ones included — the judge needs them all
 *     and this record never leaves the server unfiltered
 */
public record InterviewProblemSnapshot(
        Long problemId,
        String slug,
        String title,
        Difficulty difficulty,
        String description,
        String constraintsMarkdown,
        List<Example> examples,
        List<String> hints,
        String functionName,
        DataType returnType,
        List<Parameter> parameters,
        List<Case> testCases) {

    public record Example(String input, String output, String explanation) {}

    public record Parameter(String name, DataType type) {}

    /**
     * @param id the catalogue's id for this case, so a result can still be keyed
     *     the way the solving page keys one. It may name a row that has since
     *     been edited or deleted; nothing reads it back, it only has to be stable
     *     within the round
     */
    public record Case(Long id, String input, String expectedOutput, boolean hidden) {}

    /** Defensive copies: a snapshot that could be edited in place would not be one. */
    public InterviewProblemSnapshot {
        examples = List.copyOf(examples);
        hints = List.copyOf(hints);
        parameters = List.copyOf(parameters);
        testCases = List.copyOf(testCases);
    }

    /**
     * Takes the snapshot. The problem's collections must already be initialised —
     * this runs inside the transaction that starts the round.
     */
    public static InterviewProblemSnapshot of(Problem problem) {
        return new InterviewProblemSnapshot(
                problem.getId(),
                problem.getSlug(),
                problem.getTitle(),
                problem.getDifficulty(),
                problem.getDescription(),
                problem.getConstraintsMarkdown(),
                problem.getExamples().stream()
                        .map(example -> new Example(example.getInput(), example.getOutput(), example.getExplanation()))
                        .toList(),
                problem.getHints().stream().map(ProblemHint::getContent).toList(),
                problem.getFunctionName(),
                problem.getReturnType(),
                problem.getParameters().stream()
                        .map(parameter -> new Parameter(parameter.getName(), parameter.getType()))
                        .toList(),
                problem.getTestCases().stream()
                        .map(testCase -> new Case(
                                testCase.getId(),
                                testCase.getInput(),
                                testCase.getExpectedOutput(),
                                testCase.isHidden()))
                        .toList());
    }

    /** The sample cases, which are the only ones a client may see. */
    public List<Case> sampleTestCases() {
        return testCases.stream().filter(testCase -> !testCase.hidden()).toList();
    }

    public long hiddenTestCaseCount() {
        return testCases.stream().filter(Case::hidden).count();
    }
}
