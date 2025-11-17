package com.cloudrangers.cloudpilot.controller;

import com.cloudrangers.cloudpilot.common.ApiResponse;
import com.cloudrangers.cloudpilot.dto.request.LoginRequest;
import com.cloudrangers.cloudpilot.dto.response.LoginResponse;
import com.cloudrangers.cloudpilot.service.user.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody LoginRequest request) {
        LoginResponse response = userService.login(request);

        ResponseCookie accessCookie = ResponseCookie.from("access_token", response.getAccessToken())
                .httpOnly(true).secure(true).sameSite("Strict")
                .path("/").maxAge(3600).build();

        ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", response.getRefreshToken())
                .httpOnly(true).secure(true).sameSite("Strict")
                .path("/").maxAge(60 * 60 * 24 * 14).build();

        LoginResponse sanitized = LoginResponse.builder()
                .username(response.getUsername())
                .roleCode(response.getRoleCode())
                .roleName(response.getRoleName())
                .teamName(response.getTeamName())
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(ApiResponse.success(sanitized));
    }

    // NEW — refresh()
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<Void>> refreshToken(
            @CookieValue(value = "refresh_token", required = false) String refreshToken) {

        String newAccessToken = userService.refresh(refreshToken);

        ResponseCookie newAccessCookie = ResponseCookie.from("access_token", newAccessToken)
                .httpOnly(true).secure(true).sameSite("Strict")
                .path("/").maxAge(60 * 30).build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, newAccessCookie.toString())
                .body(ApiResponse.success(null));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        userService.logout(request);

        ResponseCookie clearAccess = ResponseCookie.from("access_token", "")
                .path("/").maxAge(0).httpOnly(true).secure(true).sameSite("Strict")
                .build();

        ResponseCookie clearRefresh = ResponseCookie.from("refresh_token", "")
                .path("/").maxAge(0).httpOnly(true).secure(true).sameSite("Strict")
                .build();

        response.addHeader("Set-Cookie", clearAccess.toString());
        response.addHeader("Set-Cookie", clearRefresh.toString());

        return ApiResponse.success(null);
    }

    @PostMapping("/password-reset")
    public ApiResponse<Void> sendPasswordReset(@RequestParam String email) {
        userService.sendPasswordResetEmail(email);
        return ApiResponse.success(null);
    }

    @PostMapping("/password-reset/confirm")
    public ApiResponse<Void> confirmPasswordReset(
            @RequestParam String token,
            @RequestParam String newPassword) {
        userService.confirmPasswordReset(token, newPassword);
        return ApiResponse.success(null);
    }

    @PostMapping("/password")
    public ApiResponse<Void> changePassword(
            @RequestParam String currentPassword,
            @RequestParam String newPassword) {
        userService.changePassword(currentPassword, newPassword);
        return ApiResponse.success(null);
    }
}

