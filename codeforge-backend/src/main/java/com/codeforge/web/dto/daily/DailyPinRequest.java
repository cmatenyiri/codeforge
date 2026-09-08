package com.codeforge.web.dto.daily;

/** Pins a chosen problem to a future date, taking it out of the rotation. */
public record DailyPinRequest(Long problemId) {}
