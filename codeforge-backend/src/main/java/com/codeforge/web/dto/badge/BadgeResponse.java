package com.codeforge.web.dto.badge;

import com.codeforge.domain.BadgeKind;

/**
 * One badge on a profile.
 *
 * @param name what it is called — "Sep 2026" for a monthly one, "100 Days 2026"
 *     for an annual one. Assembled on the server so a client cannot render a
 *     badge under a name that means something else
 * @param month ISO {@code yyyy-MM} for a monthly badge, null otherwise
 * @param progress how far towards it they are, 0–1. Complete badges are 1; the
 *     month in progress carries a real fraction, which is what makes the
 *     current month worth looking at on the third of the month
 * @param earned false for the in-progress entry, which is shown faded rather
 *     than hidden — a badge you cannot see the progress of is one nobody chases
 */
public record BadgeResponse(
        BadgeKind kind, String name, String month, Integer year, boolean earned, double progress) {}
