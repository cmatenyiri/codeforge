package com.codeforge.web.dto.user;

/**
 * @param avatar an {@link com.codeforge.domain.Avatar} name. Taken as a String
 *     rather than the enum so an unknown value becomes a field error instead of
 *     an unreadable deserialization failure.
 */
public record ChangeAvatarRequest(String avatar) {}
