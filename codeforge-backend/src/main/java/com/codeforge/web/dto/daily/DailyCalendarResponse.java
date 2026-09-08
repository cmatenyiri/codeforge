package com.codeforge.web.dto.daily;

import java.util.List;

/** A month of daily challenges, for the picker above the problem list. */
public record DailyCalendarResponse(int year, int month, List<DailyCalendarDayResponse> days) {}
