package com.cloudrangers.cloudpilot.controller;

import com.cloudrangers.cloudpilot.common.ApiResponse;
import com.cloudrangers.cloudpilot.dto.request.LoginRequest;
import com.cloudrangers.cloudpilot.dto.response.LoginResponse;
import com.cloudrangers.cloudpilot.security.JwtProvider;
import com.cloudrangers.cloudpilot.service.user.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
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
    private final JwtProvider jwtProvider;
    private final RedisTemplate<String, Object> redisTemplate;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody LoginRequest request) {
        LoginResponse response = userService.login(request);


        // 🍪 ① Access Token 쿠키 생성
        ResponseCookie accessCookie = ResponseCookie.from("access_token", response.getAccessToken())
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(60 * 60) // 1h
                .build();

        // 🍪 ② Refresh Token 쿠키 생성
        ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", response.getRefreshToken())
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(60 * 60 * 24 * 14) // 2 weeks
                .build();

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

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<Void>> refreshToken(
            @CookieValue(value = "refresh_token", required = false) String refreshToken
    ) {
        if (refreshToken == null) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.of(false, null, "리프레시 토큰이 없습니다."));
        }

        // 블랙리스트 체크
        if (redisTemplate.hasKey("BLACKLIST:" + refreshToken)) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.of(false, null, "만료되었거나 로그아웃된 토큰입니다."));
        }

        // refresh token 검증
        if (!jwtProvider.validateToken(refreshToken)) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.of(false, null, "리프레시 토큰이 유효하지 않습니다."));
        }

        String empno = jwtProvider.getEmpno(refreshToken);

        // Claims 없이 AccessToken 새로 발급 (UserService의 로직 재활용)
        Map<String, Object> claims = userService.buildClaims(empno);
        String newAccessToken = jwtProvider.generateAccessToken(empno, claims);

        ResponseCookie newAccessCookie = ResponseCookie.from("access_token", newAccessToken)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(60 * 30)  // 30분
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, newAccessCookie.toString())
                .body(ApiResponse.success(null));
    }

    /** ✅ 로그아웃 (Redis 블랙리스트 등록) */
    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request, HttpServletResponse response) {

        userService.logout(request);

        // 쿠키 삭제
        ResponseCookie clearAccess = ResponseCookie.from("access_token", "")
                .path("/")
                .maxAge(0)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .build();

        ResponseCookie clearRefresh = ResponseCookie.from("refresh_token", "")
                .path("/")
                .maxAge(0)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .build();

        response.addHeader("Set-Cookie", clearAccess.toString());
        response.addHeader("Set-Cookie", clearRefresh.toString());

        return ApiResponse.of(true, null, "로그아웃이 완료되었습니다.");
    }

    /** ✅ 비밀번호 초기화 (이메일 발송) */
    @PostMapping("/password-reset")
    public ApiResponse<Void> sendPasswordReset(@RequestParam String email) {
        userService.sendPasswordResetEmail(email);
        return ApiResponse.of(true, null, "비밀번호 재설정 이메일이 발송되었습니다.");
    }

    /** ✅ 비밀번호 재설정 (토큰 검증 후 새 비밀번호 저장) */
    @PostMapping("/password-reset/confirm")
    public ApiResponse<Void> confirmPasswordReset(
            @RequestParam String token,
            @RequestParam String newPassword
    ) {
        userService.confirmPasswordReset(token, newPassword);
        return ApiResponse.of(true, null, "비밀번호가 성공적으로 재설정되었습니다.");
    }

    /** ✅ 로그인된 사용자의 비밀번호 변경 */
    @PostMapping("/password")
    public ApiResponse<Void> changePassword(
            @RequestParam String currentPassword,
            @RequestParam String newPassword
    ) {
        userService.changePassword(currentPassword, newPassword);
        return ApiResponse.of(true, null, "비밀번호가 성공적으로 변경되었습니다.");
    }
}
