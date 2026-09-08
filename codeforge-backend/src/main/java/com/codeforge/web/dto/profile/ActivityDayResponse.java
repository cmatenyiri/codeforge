package com.codeforge.web.dto.profile;

/**
 * One square of the activity calendar.
 *
 * @param date ISO {@code yyyy-MM-dd}, bucketed in UTC — the calendar does not
 *     re-bucket per viewer, because a streak that broke when somebody flew to
 *     Tokyo would be worse than one an hour out of step
 * @param accepted of those submissions, how many were accepted; the calendar
 *     colours by total activity but the tooltip is more honest with both
 */
public record ActivityDayResponse(String date, long submissions, long accepted) {}
