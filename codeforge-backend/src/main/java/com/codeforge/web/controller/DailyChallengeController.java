package com.codeforge.web.controller;

import com.codeforge.domain.DailyChallenge;
import com.codeforge.domain.Problem;
import com.codeforge.domain.Streaks;
import com.codeforge.domain.Tag;
import com.codeforge.service.DailyChallengeService;
import com.codeforge.service.DailyChallengeService.Month;
import com.codeforge.web.dto.daily.DailyCalendarDayResponse;
import com.codeforge.web.dto.daily.DailyCalendarResponse;
import com.codeforge.web.dto.daily.DailyChallengeResponse;
import com.codeforge.web.dto.daily.DailyPinRequest;
import java.time.Duration;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The problem of the day.
 *
 * <p>One problem, the same one for everybody, turning over at midnight UTC. It
 * exists to give people a reason to come back on a day they had not planned to,
 * and the streak is the whole of that mechanism — which is why solving it late
 * counts for the catalogue but not for the run.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DailyChallengeController {

    private final DailyChallengeService dailyChallengeService;

    /** Today's problem, with the caller's streak. */
    @GetMapping("/daily")
    public ResponseEntity<DailyChallengeResponse> today() {
        DailyChallenge challenge = dailyChallengeService.todaysChallenge();
        Streaks streak = dailyChallengeService.streak();
        Problem problem = challenge.getProblem();

        return ResponseEntity.ok(new DailyChallengeResponse(
                challenge.getDate(),
                problem.getSlug(),
                problem.getTitle(),
                problem.getDifficulty(),
                problem.getTags().stream().map(Tag::getName).toList(),
                dailyChallengeService.hasSolvedToday(),
                streak.current(),
                streak.longest(),
                secondsUntilRollover()));
    }

    /**
     * A month of daily challenges.
     *
     * <p>Past days carry their problem and whether the caller solved it on the
     * day; future days are deliberately blank — see
     * {@link DailyCalendarDayResponse#slug()}.
     */
    @GetMapping("/daily/calendar")
    public ResponseEntity<DailyCalendarResponse> calendar(
            @RequestParam int year, @RequestParam int month) {

        YearMonth requested = YearMonth.of(year, month);
        Month loaded = dailyChallengeService.month(requested);
        LocalDate today = DailyChallengeService.today();

        List<DailyCalendarDayResponse> days = new ArrayList<>();
        for (LocalDate date = requested.atDay(1);
                !date.isAfter(requested.atEndOfMonth());
                date = date.plusDays(1)) {

            LocalDate day = date;
            DailyChallenge challenge = loaded.challenges().stream()
                    .filter(entry -> entry.getDate().equals(day))
                    .findFirst()
                    .orElse(null);

            days.add(new DailyCalendarDayResponse(
                    day,
                    challenge == null ? null : challenge.getProblem().getSlug(),
                    challenge == null ? null : challenge.getProblem().getTitle(),
                    challenge == null ? null : challenge.getProblem().getDifficulty(),
                    loaded.solvedDates().contains(day.toString()),
                    day.equals(today)));
        }

        return ResponseEntity.ok(new DailyCalendarResponse(year, month, days));
    }

    /** Pins a problem to a future date. Admin only; the service enforces it. */
    @PutMapping("/admin/daily/{date}")
    public ResponseEntity<Void> pin(@PathVariable String date, @RequestBody DailyPinRequest request) {
        dailyChallengeService.pin(LocalDate.parse(date), request.problemId());

        return ResponseEntity.noContent().build();
    }

    /** Hands a future date back to the rotation. */
    @DeleteMapping("/admin/daily/{date}")
    public ResponseEntity<Void> unpin(@PathVariable String date) {
        dailyChallengeService.unpin(LocalDate.parse(date));

        return ResponseEntity.noContent().build();
    }

    /**
     * Seconds until the challenge turns over.
     *
     * <p>Midnight UTC, which is the same instant for everybody — a countdown
     * derived from the viewer's own midnight would tell two people in different
     * places different amounts of time to save the same streak.
     */
    private static long secondsUntilRollover() {
        return Duration.between(
                        java.time.Instant.now(),
                        DailyChallengeService.today().plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant())
                .toSeconds();
    }
}
