package com.codeforge.security;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT settings, bound from {@code codeforge.jwt.*}.
 *
 * @param secret HMAC key material; must be at least 32 bytes for HS256
 * @param issuer value placed in, and required from, the {@code iss} claim
 * @param accessTokenTtl how long an issued access token stays valid
 * @param cookie how the token is delivered to the browser
 */
@ConfigurationProperties(prefix = "codeforge.jwt")
public record JwtProperties(String secret, String issuer, Duration accessTokenTtl, CookieProperties cookie) {

    /**
     * @param name cookie name
     * @param secure HTTPS-only; must be true anywhere but localhost
     * @param sameSite {@code Lax} is enough while the SPA and API share a site,
     *     and is what keeps cross-site requests from riding the cookie
     * @param path cookie path scope
     */
    public record CookieProperties(String name, boolean secure, String sameSite, String path) {}
}
