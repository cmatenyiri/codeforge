package com.codeforge.web.dto.interview;

import com.codeforge.domain.Difficulty;
import com.codeforge.domain.Language;
import com.codeforge.web.dto.problem.ProblemExampleResponse;
import com.codeforge.web.dto.problem.TestCaseResponse;
import java.util.List;
import java.util.Map;

/**
 * A problem as it appears inside an interview.
 *
 * <p>Its own type rather than {@code ProblemDetailResponse}, because three of
 * that record's fields have no business being on screen during a round:
 *
 * <ul>
 *   <li><b>The editorial.</b> There is no {@code hasEditorial} here and no
 *       endpoint to reach one through, so the tab cannot be opened until the
 *       report.
 *   <li><b>Hints.</b> Sent only as far as they have been revealed, one call at a
 *       time, because each one is counted against the round. A client that
 *       received all of them and promised to hide them would be counting
 *       nothing.
 *   <li><b>{@code solved} / {@code attempted}.</b> Whether this problem has come
 *       up before is exactly the thing that should not be on screen while it is
 *       being answered.
 *   <li><b>Topic tags.</b> "Sliding window" above the statement is the answer to
 *       most problems that have it. No interviewer tells you which technique
 *       they are testing, and the report links to the catalogue page where the
 *       tags are — afterwards, where they are worth reading.
 * </ul>
 *
 * @param hintCount how many exist, so the button can say how many are left
 * @param hints the revealed ones, in order — empty until one is asked for
 * @param editable false once the round has moved past this problem. The editor
 *     goes read-only and the judge refuses it either way; the statement stays
 *     readable, because wanting to re-read the question you just answered is
 *     reasonable and costs nothing
 * @param submittedSourceCode what a closed problem shows in the editor: the code
 *     the judge actually saw. The client's own draft is not good enough — it
 *     keeps taking keystrokes while a submission is being judged, so an
 *     acceptance can arrive and lock the problem over text that was never
 *     submitted. Absent while the problem is still open, and for one skipped
 *     without a single attempt
 * @param submittedLanguage the language that submission was written in, so the
 *     locked view highlights it correctly rather than in whatever the picker
 *     happened to be showing
 */
public record InterviewProblemResponse(
        Long id,
        String slug,
        String title,
        Difficulty difficulty,
        String description,
        String constraintsMarkdown,
        List<ProblemExampleResponse> examples,
        List<TestCaseResponse> sampleTestCases,
        int hiddenTestCaseCount,
        Map<Language, String> starterCode,
        int position,
        boolean warmUp,
        boolean solved,
        boolean skipped,
        boolean editable,
        String submittedSourceCode,
        Language submittedLanguage,
        int attempts,
        int hintCount,
        List<String> hints) {}
