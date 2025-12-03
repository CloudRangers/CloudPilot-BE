package com.cloudrangers.cloudpilot.controller;

import com.cloudrangers.cloudpilot.common.ApiResponse;
import com.cloudrangers.cloudpilot.domain.user.User;
import com.cloudrangers.cloudpilot.domain.user.UserRole;
import com.cloudrangers.cloudpilot.dto.request.LoginRequest;
import com.cloudrangers.cloudpilot.dto.response.LoginResponse;
import com.cloudrangers.cloudpilot.dto.response.TokenRefreshResponse;
import com.cloudrangers.cloudpilot.exception.notfound.UserNotFoundException;
import com.cloudrangers.cloudpilot.repository.user.UserRepository;
import com.cloudrangers.cloudpilot.security.JwtProvider;
import com.cloudrangers.cloudpilot.service.user.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;

    /**
     * 공통 쿠키 생성 함수
     */
    private ResponseCookie createCookie(String name, String value, long maxAgeSeconds) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(false)         // 🔥 로컬 개발 기준
                .sameSite("Lax")
                .path("/")
                .maxAge(maxAgeSeconds)
                .build();
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody LoginRequest request) {

        // 기본 로그인 처리 (유저 정보)
        LoginResponse info = userService.login(request);

        String empno = String.valueOf(request.getEmpno());
        var claims = userService.buildClaims(empno);

        String accessToken = jwtProvider.generateAccessToken(empno, claims);
        String refreshToken = jwtProvider.generateRefreshToken(empno);

        // access 30분, refresh 14일
        ResponseCookie accessCookie = createCookie("access_token", accessToken, 60L * 30);
        ResponseCookie refreshCookie = createCookie("refresh_token", refreshToken, 60L * 60 * 24 * 14);

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        accessCookie.toString(),
                        refreshCookie.toString()
                )
                .body(ApiResponse.success(info));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<Void>> refreshToken(HttpServletRequest request) {

        // refresh_token 기반으로 새 토큰들 발급
        TokenRefreshResponse tokens = userService.refresh(request);

        long newAccessTokenMaxAge = jwtProvider.getRemainingExpiration(tokens.getAccessToken()) / 1000;
        ResponseCookie newAccessCookie =
                createCookie("access_token", tokens.getAccessToken(), newAccessTokenMaxAge);
        ResponseCookie newRefreshCookie =
                createCookie("refresh_token", tokens.getRefreshToken(), 60L * 60 * 24 * 14);

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        newAccessCookie.toString(),
                        newRefreshCookie.toString()
                )
                .body(ApiResponse.success(null));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request, HttpServletResponse response) {

        userService.logout(request);

        // 쿠키 삭제
        ResponseCookie clearAccess = createCookie("access_token", "", 0);
        ResponseCookie clearRefresh = createCookie("refresh_token", "", 0);

        response.addHeader(HttpHeaders.SET_COOKIE, clearAccess.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, clearRefresh.toString());

        return ApiResponse.success(null);
    }

    /**
     * 로그인한 내 정보 조회
     * GET /auth/me
     */
    @GetMapping("/me")
    @Transactional(readOnly = true)  // ⭐ 추가
    public ApiResponse<LoginResponse> getMyInfo() {

        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (authentication == null || authentication.getName() == null) {
            throw new RuntimeException("인증 정보가 없습니다. (SecurityContext authentication null)");
        }

        // JwtAuthenticationFilter + CustomUserDetails → getName() = username
        String username = authentication.getName();
        log.info("[AuthController] /auth/me 요청, username={}", username);

        User user = userRepository.findWithRolesByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(username));

        UserRole userRole = user.getUserRoles().stream()
                .max(Comparator.comparingInt(a -> a.getRole().getPermissionLevel()))
                .orElseThrow(() -> new RuntimeException("역할 정보가 없습니다."));

        var role = userRole.getRole();
        var team = userRole.getTeam();

        LoginResponse dto = LoginResponse.builder()
                .username(user.getUsername())
                .roleCode(role.getCode())
                .roleName(role.getName())
                .teamId(team != null ? team.getId() : null)
                .teamName(team != null ? team.getName() : "GLOBAL")
                .build();

        return ApiResponse.success(dto);
    }
}
