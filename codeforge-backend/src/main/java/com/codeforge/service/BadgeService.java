package com.codeforge.service;

import com.codeforge.domain.BadgeKind;
import com.codeforge.repository.DailyChallengeRepository;
import com.codeforge.web.dto.badge.BadgeResponse;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The badges the daily challenge awards.
 *
 * <p>Entirely derived. There is no badges table and nothing is ever granted:
 * the days somebody solved the daily are already recorded, and a badge is just
 * a sentence about them. That buys three things for free — a badge can never be
 * awarded twice, one can never go missing because a grant failed, and a rejudge
 * that turns an acceptance into a rejection takes the badge with it, which a
 * stored grant would not.
 *
 * <p>The cost is two aggregate queries per profile view. At the size a badge
 * list is worth showing that is cheaper than the bookkeeping the alternative
 * needs.
 */
@Service
@RequiredArgsConstructor
public class BadgeService {

    /** Days within one year that earn the annual badges. */
    private static final int ANNUAL_50 = 50;

    private static final int ANNUAL_100 = 100;

    /** Marker for "every day of the year"; the real threshold depends on the year. */
    private static final int ANNUAL_ALL = -1;

    private static final DateTimeFormatter MONTH_KEY = DateTimeFormatter.ofPattern("yyyy-MM");

    private final DailyChallengeRepository dailyChallengeRepository;

    /**
     * Every badge this user has, newest first, with the month in progress at the
     * front.
     *
     * <p>The current month is included even when unfinished, carrying its real
     * fraction. A badge nobody can see themselves approaching is a badge nobody
     * chases, and the whole purpose of the monthly one is to be visible on the
     * third of the month.
     */
    @Transactional(readOnly = true)
    public List<BadgeResponse> badgesOf(Long userId, Locale locale) {
        List<BadgeResponse> badges = new ArrayList<>();
        YearMonth thisMonth = YearMonth.from(DailyChallengeService.today());

        for (DailyChallengeRepository.MonthProgress month : dailyChallengeRepository.findMonthlyProgress(userId)) {
            if (month.getTotal() == 0) {
                continue;
            }
            YearMonth when = YearMonth.parse(month.getMonth(), MONTH_KEY);
            boolean complete = month.getSolved() == month.getTotal();
            boolean current = when.equals(thisMonth);

            // A finished month that was not cleared is not a badge and not a
            // near-miss worth displaying — it is simply a month that went by.
            if (!complete && !current) {
                continue;
            }

            badges.add(new BadgeResponse(
                    BadgeKind.MONTHLY,
                    monthName(when, locale),
                    month.getMonth(),
                    when.getYear(),
                    complete,
                    (double) month.getSolved() / month.getTotal()));
        }

        for (DailyChallengeRepository.YearProgress year : dailyChallengeRepository.findYearlyProgress(userId)) {
            for (int marker : new int[] {ANNUAL_50, ANNUAL_100, ANNUAL_ALL}) {
                // A leap year needs 366, so the "every day" threshold is the
                // length of the year rather than a constant that would make the
                // badge quietly unreachable every fourth year.
                int threshold = marker == ANNUAL_ALL ? daysIn(year.getYear()) : marker;
                boolean earned = year.getDays() >= threshold;
                boolean current = year.getYear() == DailyChallengeService.today().getYear();

                // Same rule as the months: a past year that fell short is not
                // shown, but the year in progress shows what is still reachable.
                if (!earned && !current) {
                    continue;
                }
                badges.add(new BadgeResponse(
                        kindFor(marker),
                        threshold + " Days " + year.getYear(),
                        null,
                        year.getYear(),
                        earned,
                        Math.min(1, (double) year.getDays() / threshold)));
            }
        }

        // Newest first, and within a date the earned ones before the ones still
        // being worked on.
        badges.sort((left, right) -> {
            int byYear = Integer.compare(
                    right.year() == null ? 0 : right.year(), left.year() == null ? 0 : left.year());
            if (byYear != 0) {
                return byYear;
            }
            int byMonth = String.valueOf(right.month()).compareTo(String.valueOf(left.month()));
            return byMonth != 0 ? byMonth : Boolean.compare(right.earned(), left.earned());
        });

        return badges;
    }

    private static BadgeKind kindFor(int marker) {
        return switch (marker) {
            case ANNUAL_50 -> BadgeKind.ANNUAL_50;
            case ANNUAL_100 -> BadgeKind.ANNUAL_100;
            default -> BadgeKind.ANNUAL_365;
        };
    }

    /** "Sep 2026", in the reader's language. */
    private static String monthName(YearMonth month, Locale locale) {
        return month.getMonth().getDisplayName(TextStyle.SHORT, locale) + " " + month.getYear();
    }

    /** Days in the year, so the "every day" badge is right in a leap year. */
    private static int daysIn(int year) {
        return LocalDate.of(year, 1, 1).lengthOfYear();
    }
}
