package com.codeforge.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Populates the SecurityContext straight from the signed token.
 *
 * <p>No {@code UserDetailsService}, no repository call: identity and role come
 * out of the token, which is the whole reason the role is a claim.
 *
 * <p>The browser session travels in an httpOnly cookie; the {@code Authorization}
 * header is still accepted so that non-browser clients (curl, integration
 * scripts) can authenticate without juggling a cookie jar. The cookie wins when
 * both are present.
 *
 * <p>A missing or invalid token is left unauthenticated rather than rejected
 * here — the entry point decides whether the target endpoint required
 * authentication at all.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final AuthCookieService authCookieService;
    private final WebAuthenticationDetailsSource detailsSource = new WebAuthenticationDetailsSource();

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            readToken(request).flatMap(jwtService::parseAccessToken).ifPresent(user -> authenticate(user, request));
        }

        filterChain.doFilter(request, response);
    }

    private Optional<String> readToken(HttpServletRequest request) {
        Optional<String> fromCookie = authCookieService.readToken(request);
        return fromCookie.isPresent() ? fromCookie : readBearerHeader(request);
    }

    private Optional<String> readBearerHeader(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return Optional.empty();
        }
        String token = header.substring(BEARER_PREFIX.length()).trim();
        return token.isEmpty() ? Optional.empty() : Optional.of(token);
    }

    private void authenticate(AuthenticatedUser user, HttpServletRequest request) {
        var authorities = List.of(new SimpleGrantedAuthority(user.role().authority()));
        var authentication = UsernamePasswordAuthenticationToken.authenticated(user, null, authorities);
        authentication.setDetails(detailsSource.buildDetails(request));

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
    }
}
