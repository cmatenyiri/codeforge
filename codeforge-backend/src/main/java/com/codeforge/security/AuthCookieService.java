package com.codeforge.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

/**
 * Carries the access token in an httpOnly cookie.
 *
 * <p>httpOnly means page scripts cannot read the token, so an XSS bug cannot
 * exfiltrate the session. The trade-off is that the browser now attaches
 * credentials automatically, which is what CSRF exploits — {@code SameSite}
 * plus an explicit CORS origin allowlist is what closes that back off. Any
 * state-changing endpoint added later should be re-checked against that.
 */
@Service
public class AuthCookieService {

    private final JwtProperties.CookieProperties cookieProperties;
    private final Duration ttl;

    public AuthCookieService(JwtProperties properties) {
        this.cookieProperties = properties.cookie();
        this.ttl = properties.accessTokenTtl();
    }

    /** The {@code Set-Cookie} value that establishes a session. */
    public ResponseCookie create(String token) {
        return baseCookie(token).maxAge(ttl).build();
    }

    /** The {@code Set-Cookie} value that ends one; an empty, already-expired cookie. */
    public ResponseCookie clear() {
        return baseCookie("").maxAge(0).build();
    }

    /** Reads the token the browser sent back, if any. */
    public Optional<String> readToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }

        return Arrays.stream(cookies)
                .filter(cookie -> cookieProperties.name().equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(value -> value != null && !value.isBlank())
                .findFirst();
    }

    private ResponseCookie.ResponseCookieBuilder baseCookie(String value) {
        return ResponseCookie.from(cookieProperties.name(), value)
                .httpOnly(true)
                .secure(cookieProperties.secure())
                .sameSite(cookieProperties.sameSite())
                .path(cookieProperties.path());
    }
}
