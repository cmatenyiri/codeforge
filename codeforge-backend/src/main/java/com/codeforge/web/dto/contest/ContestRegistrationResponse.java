package com.codeforge.web.dto.contest;

/** The state of the register button after it has been pressed. */
public record ContestRegistrationResponse(boolean registered, long registrationCount) {}
