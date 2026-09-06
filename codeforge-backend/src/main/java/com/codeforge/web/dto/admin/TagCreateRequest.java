package com.codeforge.web.dto.admin;

/** A new topic. The slug is derived from the name, as it is for a problem. */
public record TagCreateRequest(String name) {}
