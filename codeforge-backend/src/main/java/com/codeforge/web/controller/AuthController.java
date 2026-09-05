package com.codeforge.web.controller;

import com.codeforge.domain.User;
import com.codeforge.security.AuthCookieService;
import com.codeforge.service.AuthService;
import com.codeforge.validation.LoginRequestValidator;
import com.codeforge.validation.RegisterRequestValidator;
import com.codeforge.web.dto.auth.AuthResponse;
import com.codeforge.web.dto.auth.LoginRequest;
import com.codeforge.web.dto.auth.RegisterRequest;
import com.codeforge.web.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Registration, login and logout.
 *
 * <p>The controller is the DTO boundary: it validates the payload, maps it to an
 * entity, and hands the entity to the service. Nothing below this layer sees a
 * DTO. The access token leaves only as an httpOnly cookie.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AuthCookieService authCookieService;
    private final UserMapper userMapper;
    private final RegisterRequestValidator registerRequestValidator;
    private final LoginRequestValidator loginRequestValidator;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        registerRequestValidator.validate(request);

        User user = authService.register(userMapper.toEntity(request), request.password());

        return respondWithSession(user, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        loginRequestValidator.validate(request);

        User user = authService.authenticate(request.username(), request.password());

        return respondWithSession(user, HttpStatus.OK);
    }

    /**
     * Ends the session by overwriting the cookie with an expired one.
     *
     * <p>The JWT itself stays cryptographically valid until it expires; killing
     * it server-side would need a revocation list, which is only worth adding
     * once there is a reason to revoke.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, authCookieService.clear().toString())
                .build();
    }

    private ResponseEntity<AuthResponse> respondWithSession(User user, HttpStatus status) {
        ResponseCookie cookie = authCookieService.create(authService.issueAccessToken(user));

        return ResponseEntity.status(status)
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new AuthResponse(userMapper.toResponse(user), authService.accessTokenTtl().toSeconds()));
    }
}
