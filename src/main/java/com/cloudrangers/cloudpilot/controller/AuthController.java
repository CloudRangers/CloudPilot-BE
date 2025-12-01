package com.cloudrangers.cloudpilot.controller;

import com.cloudrangers.cloudpilot.common.ApiResponse;
import com.cloudrangers.cloudpilot.dto.request.LoginRequest;
import com.cloudrangers.cloudpilot.dto.response.LoginResponse;
import com.cloudrangers.cloudpilot.dto.response.TokenRefreshResponse;
import com.cloudrangers.cloudpilot.security.JwtProvider;
import com.cloudrangers.cloudpilot.service.user.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtProvider jwtProvider;

    // 공통 쿠키 생성 함수
    // Environment-aware cookie creation could be added here (e.g., based on active profile)
    private ResponseCookie createCookie(String name, String value, long maxAge) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(true) // Enforce HTTPS. For local testing over HTTP, this might need to be false.
                .sameSite("Lax") // More secure default than "None"
                .path("/")
                .maxAge(maxAge)
                .build();
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody LoginRequest request) {

        LoginResponse info = userService.login(request);

        String empno = String.valueOf(request.getEmpno());
        var claims = userService.buildClaims(empno);

        String accessToken = jwtProvider.generateAccessToken(empno, claims);
        String refreshToken = jwtProvider.generateRefreshToken(empno);

        long accessTokenMaxAge = jwtProvider.getRemainingExpiration(accessToken) / 1000;
        ResponseCookie accessCookie = createCookie("access_token", accessToken, accessTokenMaxAge);
        ResponseCookie refreshCookie = createCookie("refresh_token", refreshToken, 60L * 60 * 24 * 14);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(ApiResponse.success(info));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<Void>> refreshToken(HttpServletRequest request) {

        TokenRefreshResponse tokens = userService.refresh(request);

        long newAccessTokenMaxAge = jwtProvider.getRemainingExpiration(tokens.getAccessToken()) / 1000;
        ResponseCookie newAccessCookie = createCookie("access_token", tokens.getAccessToken(), newAccessTokenMaxAge);
        ResponseCookie newRefreshCookie = createCookie("refresh_token", tokens.getRefreshToken(), 60L * 60 * 24 * 14);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, newAccessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, newRefreshCookie.toString())
                .body(ApiResponse.success(null));
    }


    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request, HttpServletResponse response) {

        userService.logout(request);

        ResponseCookie clearAccess = createCookie("access_token", "", 0);
        ResponseCookie clearRefresh = createCookie("refresh_token", "", 0);

        response.addHeader("Set-Cookie", clearAccess.toString());
        response.addHeader("Set-Cookie", clearRefresh.toString());

        return ApiResponse.success(null);
    }

    @GetMapping("/me")
    public ApiResponse<LoginResponse> getMyInfo(HttpServletRequest request) {
        return ApiResponse.success(userService.getMyInfo(request));
    }
}
