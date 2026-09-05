package com.codeforge.web.controller;

import com.codeforge.domain.User;
import com.codeforge.security.AuthCookieService;
import com.codeforge.service.AuthService;
import com.codeforge.service.UserService;
import com.codeforge.validation.ChangeAvatarRequestValidator;
import com.codeforge.validation.ChangePasswordRequestValidator;
import com.codeforge.validation.ChangeUsernameRequestValidator;
import com.codeforge.web.dto.auth.UserResponse;
import com.codeforge.web.dto.user.ChangeAvatarRequest;
import com.codeforge.web.dto.user.ChangePasswordRequest;
import com.codeforge.web.dto.user.ChangeUsernameRequest;
import com.codeforge.web.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final AuthService authService;
    private final AuthCookieService authCookieService;
    private final UserMapper userMapper;
    private final ChangeAvatarRequestValidator changeAvatarRequestValidator;
    private final ChangeUsernameRequestValidator changeUsernameRequestValidator;
    private final ChangePasswordRequestValidator changePasswordRequestValidator;

    /** Lets the frontend re-hydrate the session from the cookie on boot. */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> currentUser() {
        return ResponseEntity.ok(userMapper.toResponse(userService.getCurrentUser()));
    }

    @PatchMapping("/me/avatar")
    public ResponseEntity<UserResponse> changeAvatar(@RequestBody ChangeAvatarRequest request) {
        User user = userService.changeAvatar(changeAvatarRequestValidator.validate(request));

        return ResponseEntity.ok(userMapper.toResponse(user));
    }

    /**
     * Renames the caller and re-issues the session cookie.
     *
     * <p>The old token would still authenticate — the subject is the user id —
     * but its {@code username} claim would be stale, so anything reading the
     * claim would show the previous name until the token expired.
     */
    @PatchMapping("/me/username")
    public ResponseEntity<UserResponse> changeUsername(@RequestBody ChangeUsernameRequest request) {
        changeUsernameRequestValidator.validate(request);

        User user = userService.changeUsername(request.username());

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        authCookieService
                                .create(authService.issueAccessToken(user))
                                .toString())
                .body(userMapper.toResponse(user));
    }

    @PatchMapping("/me/password")
    public ResponseEntity<UserResponse> changePassword(@RequestBody ChangePasswordRequest request) {
        changePasswordRequestValidator.validate(request);

        User user = userService.changePassword(request.newPassword());

        return ResponseEntity.ok(userMapper.toResponse(user));
    }
}
