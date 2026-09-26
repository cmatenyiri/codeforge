package com.codeforge.web.dto.admin;

/**
 * The announced contest keeping a problem out of the catalogue until it ends,
 * so the authoring screens can say which one and link to it.
 */
public record ContestHoldResponse(Long contestId, String slug, String title) {}
