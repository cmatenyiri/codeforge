package com.codeforge.web.dto.auth;

/**
 * What a successful register/login returns.
 *
 * <p>The token itself is deliberately absent: it goes out in an httpOnly cookie
 * so that page scripts cannot read it. Putting it in the body as well would
 * hand it straight back to any XSS and undo the point of the cookie.
 *
 * @param user the caller, so the UI can render immediately without a second call
 * @param expiresInSeconds session lifetime, so the UI can warn before expiry
 */
public record AuthResponse(UserResponse user, long expiresInSeconds) {}
