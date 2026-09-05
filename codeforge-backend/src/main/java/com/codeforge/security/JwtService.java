package com.codeforge.security;

import com.codeforge.domain.Role;
import com.codeforge.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

/**
 * Issues and verifies access tokens.
 *
 * <p>The subject is the user id rather than the username. Both identify the user,
 * but an id is stable across a username change, so a rename cannot silently
 * invalidate (or worse, misroute) tokens already in the wild. The username still
 * travels as a claim so the UI and audit logs can show it without a lookup.
 */
@Service
public class JwtService {

    private static final String CLAIM_USERNAME = "username";
    private static final String CLAIM_ROLE = "role";

    private final SecretKey key;
    private final String issuer;
    private final Duration accessTokenTtl;

    public JwtService(JwtProperties properties) {
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
        this.issuer = properties.issuer();
        this.accessTokenTtl = properties.accessTokenTtl();
    }

    /** Mints an access token carrying everything the SecurityContext needs. */
    public String issueAccessToken(User user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(accessTokenTtl);

        return Jwts.builder()
                .issuer(issuer)
                .subject(String.valueOf(user.getId()))
                .claim(CLAIM_USERNAME, user.getUsername())
                .claim(CLAIM_ROLE, user.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(key)
                .compact();
    }

    public Duration accessTokenTtl() {
        return accessTokenTtl;
    }

    /**
     * Verifies the signature, issuer and expiry, and rebuilds the principal.
     *
     * @return the principal, or empty if the token is absent, malformed, tampered
     *     with, expired, or carries a role this build no longer knows about
     */
    public Optional<AuthenticatedUser> parseAccessToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(issuer)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return Optional.of(new AuthenticatedUser(
                    Long.valueOf(claims.getSubject()),
                    claims.get(CLAIM_USERNAME, String.class),
                    Role.valueOf(claims.get(CLAIM_ROLE, String.class))));
        } catch (JwtException | IllegalArgumentException ex) {
            // Any invalid token is simply an unauthenticated request; the entry
            // point turns that into a 401 further down the chain.
            return Optional.empty();
        }
    }
}
