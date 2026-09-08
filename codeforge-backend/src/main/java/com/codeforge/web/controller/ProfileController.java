package com.codeforge.web.controller;

import com.codeforge.service.ProfileService;
import com.codeforge.service.ProfileService.Board;
import com.codeforge.web.dto.common.PageResponse;
import com.codeforge.web.dto.profile.ActivityCalendarResponse;
import com.codeforge.web.dto.profile.LeaderboardRowResponse;
import com.codeforge.web.dto.profile.PublicProfileResponse;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public profiles and the tables that put everybody in order.
 *
 * <p>Keyed by username rather than id, because a profile URL is a thing people
 * type and share and {@code /u/ada} is the only form of it worth having.
 *
 * <p>Signed-in callers only, like the rest of the application — this is not an
 * open directory of everybody's activity, it is a feature of a product you are
 * already using.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ProfileController {

    private static final int MAX_PAGE_SIZE = 100;

    private final ProfileService profileService;

    /**
     * One person's public profile: solves, contests, rating and a year of
     * activity.
     *
     * @param year which calendar year the heatmap should show; omitted means the
     *     rolling twelve months, which is what a profile opens on
     * @param locale the reader's, so a badge reads "Sep 2026" in their language
     */
    @GetMapping("/profiles/{username}")
    public ResponseEntity<PublicProfileResponse> profile(
            @PathVariable String username,
            @RequestParam(required = false) Integer year,
            Locale locale) {

        return ResponseEntity.ok(profileService.profile(username, year, locale));
    }

    /**
     * One year of somebody's activity calendar.
     *
     * <p>Separate from the profile so switching years re-fetches a grid rather
     * than the whole page — the rating graph and the contest history do not
     * change when you look at 2024.
     */
    @GetMapping("/profiles/{username}/calendar")
    public ResponseEntity<ActivityCalendarResponse> calendar(
            @PathVariable String username, @RequestParam(required = false) Integer year) {

        return ResponseEntity.ok(profileService.calendar(username, year));
    }

    /** The rating table — only people who have actually competed. */
    @GetMapping("/leaderboard/rating")
    public ResponseEntity<PageResponse<LeaderboardRowResponse>> ratingLeaderboard(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "25") int size) {

        return ResponseEntity.ok(toPage(profileService.ratingLeaderboard(clampPage(page), clampSize(size)), page, size));
    }

    /** The solved table, weighted by difficulty so that hard problems count for more. */
    @GetMapping("/leaderboard/solved")
    public ResponseEntity<PageResponse<LeaderboardRowResponse>> solvedLeaderboard(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "25") int size) {

        return ResponseEntity.ok(toPage(profileService.solvedLeaderboard(clampPage(page), clampSize(size)), page, size));
    }

    /**
     * Wraps a board in the same envelope every other list uses.
     *
     * <p>Built by hand rather than from a Spring {@code Page} because neither
     * board is one: the rating table is a limit/offset over an indexed column and
     * the solved table is an aggregate, and forcing either into a Page would mean
     * running its count query a second time.
     */
    private static PageResponse<LeaderboardRowResponse> toPage(Board board, int page, int size) {
        int clampedSize = clampSize(size);
        int totalPages = clampedSize == 0 ? 0 : (int) Math.ceil((double) board.total() / clampedSize);

        return new PageResponse<>(board.rows(), clampPage(page), clampedSize, board.total(), totalPages);
    }

    private static int clampPage(int page) {
        return Math.max(page, 0);
    }

    private static int clampSize(int size) {
        return Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
    }
}
