package com.codeforge.web.mapper;

import com.codeforge.domain.Interview;
import com.codeforge.domain.InterviewInsight;
import com.codeforge.domain.InterviewProblem;
import com.codeforge.domain.InterviewProblemSnapshot;
import com.codeforge.domain.Language;
import com.codeforge.domain.Problem;
import com.codeforge.service.InterviewService.OpenSlot;
import com.codeforge.web.dto.interview.InterviewProblemResponse;
import com.codeforge.web.dto.interview.InterviewProblemResultResponse;
import com.codeforge.web.dto.interview.InterviewReportResponse;
import com.codeforge.web.dto.interview.InterviewSessionResponse;
import com.codeforge.web.dto.interview.InterviewSlotResponse;
import com.codeforge.web.dto.interview.InterviewSummaryResponse;
import com.codeforge.web.dto.problem.ProblemExampleResponse;
import com.codeforge.web.dto.problem.TestCaseResponse;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Entity → DTO for the interview screens.
 *
 * <p>Hand-written rather than a MapStruct interface like its neighbours,
 * because almost nothing here is a field copy: the remaining time is derived
 * from a clock, the warm-up flag from a slot's position within its set, and the
 * whole point of {@link InterviewProblemResponse} is the fields it leaves out.
 * A generated mapper plus an equal weight of {@code @Named} helpers would say
 * the same thing less directly.
 */
@Component
public class InterviewMapper {

    /**
     * A running interview.
     *
     * @param now taken once by the caller so every slot and the countdown agree
     *     on the same instant
     */
    public InterviewSessionResponse toSession(Interview interview, Instant now) {
        return new InterviewSessionResponse(
                interview.getId(),
                interview.getFormat(),
                interview.getStatus(),
                interview.getStartedAt(),
                interview.getDurationMinutes(),
                interview.isActive() ? interview.remainingSeconds(now) : 0,
                interview.activePosition().isPresent() ? interview.activePosition().getAsInt() : null,
                interview.getProblems().stream()
                        .map(slot -> toSlot(slot, interview))
                        .toList());
    }

    /**
     * One slot in the tab strip.
     *
     * <p>Title and difficulty only. The description is not here on purpose: it
     * arrives one problem at a time, which is also what starts that problem's
     * clock — and a problem the round has not reached yet cannot be fetched at
     * all, which is why the tab strip needs to know it is locked.
     */
    public InterviewSlotResponse toSlot(InterviewProblem slot, Interview interview) {
        // The frozen copy, like everything else inside a running round: an author
        // renaming the problem must not rename it on a candidate mid-question.
        InterviewProblemSnapshot snapshot = slot.getSnapshot();
        int active = interview.activePosition().orElse(interview.getProblems().size());

        return new InterviewSlotResponse(
                slot.getPosition(),
                snapshot.problemId(),
                snapshot.slug(),
                snapshot.title(),
                snapshot.difficulty(),
                slot.isWarmUp(),
                slot.getPosition() > active,
                slot.isResolved(),
                slot.isSolved(),
                slot.isSkipped(),
                slot.getAttempts(),
                slot.getHintsRevealed());
    }

    /**
     * A problem as it appears mid-round.
     *
     * <p>Everything the solving page would show about the candidate's history
     * with this problem is dropped — whether they have solved it before is
     * exactly what should not be on screen while they answer it — along with any
     * route to the editorial and the topic tags, which for a good many problems
     * are the answer.
     *
     * @param hints only those revealed so far, each one already counted against
     *     the round by the service
     */
    public InterviewProblemResponse toProblem(
            OpenSlot slot, Map<Language, String> starterCode, List<String> hints) {

        // Every field below comes off the snapshot rather than the catalogue.
        // Nothing on this screen is read from the live problem, which is what
        // makes an edit landing mid-round unable to change the question under
        // the candidate — statement, examples and sample cases included.
        InterviewProblemSnapshot snapshot = slot.snapshot();

        return new InterviewProblemResponse(
                snapshot.problemId(),
                snapshot.slug(),
                snapshot.title(),
                snapshot.difficulty(),
                snapshot.description(),
                snapshot.constraintsMarkdown(),
                snapshot.examples().stream()
                        .map(example -> new ProblemExampleResponse(
                                example.input(), example.output(), example.explanation()))
                        .toList(),
                snapshot.sampleTestCases().stream()
                        .map(testCase ->
                                new TestCaseResponse(testCase.id(), testCase.input(), testCase.expectedOutput()))
                        .toList(),
                (int) snapshot.hiddenTestCaseCount(),
                starterCode,
                slot.position(),
                slot.warmUp(),
                slot.solved(),
                slot.skipped(),
                slot.editable(),
                slot.submittedSourceCode(),
                slot.submittedLanguage(),
                slot.attempts(),
                snapshot.hints().size(),
                hints);
    }

    /** The debrief. */
    public InterviewReportResponse toReport(
            Interview interview, List<InterviewInsight> insights, Instant now) {

        List<InterviewProblem> slots = interview.getProblems();

        return new InterviewReportResponse(
                interview.getId(),
                interview.getFormat(),
                interview.getStatus(),
                interview.getOutcome(),
                interview.getStartedAt(),
                interview.getEndedAt(),
                interview.getDurationMinutes(),
                interview.elapsedSeconds(now),
                (int) interview.solvedCount(),
                slots.size(),
                slots.stream().mapToInt(InterviewProblem::getAttempts).sum(),
                slots.stream().mapToInt(InterviewProblem::getHintsRevealed).sum(),
                interview.getUsedOutsideHelp(),
                slots.stream().map(this::toResult).toList(),
                insights);
    }

    /**
     * One problem's line in the debrief.
     *
     * <p>Named as it was asked — the title and difficulty come off the snapshot,
     * because a report that renamed the question after the fact would describe a
     * round nobody sat. The link, though, uses the live slug: it points into the
     * catalogue as it is now, and a frozen slug would 404 if the problem has been
     * renamed since.
     */
    public InterviewProblemResultResponse toResult(InterviewProblem slot) {
        Problem problem = slot.getProblem();
        InterviewProblemSnapshot snapshot = slot.getSnapshot();

        return new InterviewProblemResultResponse(
                slot.getPosition(),
                problem.getId(),
                problem.getSlug(),
                snapshot.title(),
                snapshot.difficulty(),
                slot.isWarmUp(),
                slot.isSolved(),
                slot.isSkipped(),
                slot.timeToSolveSeconds(),
                slot.getAttempts(),
                slot.getHintsRevealed(),
                slot.getSolvedBySubmission() == null ? null : slot.getSolvedBySubmission().getId());
    }

    /**
     * One row in the history.
     *
     * <p>The problem count comes off the format rather than the slots, which is
     * what lets the list be paged without a collection fetch per row.
     */
    public InterviewSummaryResponse toSummary(Interview interview) {
        return new InterviewSummaryResponse(
                interview.getId(),
                interview.getFormat(),
                interview.getStatus(),
                interview.getOutcome(),
                interview.getStartedAt(),
                interview.getEndedAt(),
                interview.getDurationMinutes(),
                interview.getScore(),
                interview.getFormat().problemCount());
    }
}
