package com.codeforge.web.dto.daily;

import com.codeforge.domain.Difficulty;
import java.time.LocalDate;

/**
 * One square of the daily-challenge calendar.
 *
 * @param slug null for a day that has not arrived: its problem has not been
 *     decided yet, and deciding it early would fix a future day's question
 *     against today's catalogue
 */
public record DailyCalendarDayResponse(
        LocalDate date, String slug, String title, Difficulty difficulty, boolean solved, boolean today) {}
