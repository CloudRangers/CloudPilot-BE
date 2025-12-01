package com.cloudrangers.cloudpilot.controller;

import com.cloudrangers.cloudpilot.common.ApiResponse;
import com.cloudrangers.cloudpilot.domain.user.User;
import com.cloudrangers.cloudpilot.domain.user.UserRole;
import com.cloudrangers.cloudpilot.dto.request.LoginRequest;
import com.cloudrangers.cloudpilot.dto.response.LoginResponse;
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

import java.util.Comparator;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtProvider jwtProvider;

    // ✅ 네가 AuthMeController에서 사용하던 레포지토리 주입
    private final UserRepository userRepository;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody LoginRequest request) {

        LoginResponse info = userService.login(request);

        String empno = String.valueOf(request.getEmpno());
        var claims = userService.buildClaims(empno);

        String accessToken = jwtProvider.generateAccessToken(empno, claims);
        String refreshToken = jwtProvider.generateRefreshToken(empno);

        // ⭐ 로컬 개발환경: SameSite=Lax + secure=false
        ResponseCookie accessCookie = ResponseCookie.from("access_token", accessToken)
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(60 * 30)
                .build();

        ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", refreshToken)
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(60L * 60 * 24 * 14)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE,
                        accessCookie.toString(),
                        refreshCookie.toString())
                .body(ApiResponse.success(info));

    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<Void>> refreshToken(HttpServletRequest request) {

        String newAccessToken = userService.refresh(request);

        ResponseCookie newAccessCookie = ResponseCookie.from("access_token", newAccessToken)
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(60 * 30)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, newAccessCookie.toString())
                .body(ApiResponse.success(null));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request, HttpServletResponse response) {

        userService.logout(request);

        ResponseCookie clearAccess = ResponseCookie.from("access_token", "")
                .path("/")
                .maxAge(0)
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .build();

        ResponseCookie clearRefresh = ResponseCookie.from("refresh_token", "")
                .path("/")
                .maxAge(0)
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .build();

        response.addHeader("Set-Cookie", clearAccess.toString());
        response.addHeader("Set-Cookie", clearRefresh.toString());

        return ApiResponse.success(null);
    }

    /**
     * 로그인한 내 정보 조회
     * GET /auth/me
     *
     * - JwtAuthenticationFilter에서 넣어준 Authentication의 name(empno)을 사용
     * - User + UserRole + Team 정보 조회해서 LoginResponse 구성
     */
    @GetMapping("/me")
    public ApiResponse<LoginResponse> getMyInfo(HttpServletRequest request) {

        // 1) SecurityContext 에서 현재 사용자(empno) 꺼내기
        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (authentication == null || authentication.getName() == null) {
            throw new RuntimeException("인증 정보가 없습니다. (SecurityContext authentication null)");
        }

        String empnoStr = authentication.getName();   // JwtAuthenticationFilter 에서 세팅한 값 (사번)
        Long empno = Long.valueOf(empnoStr);

        // 2) DB에서 유저 + 역할 정보 로딩
        User user = userRepository.findWithRolesByEmpno(empno)
                .orElseThrow(() -> new UserNotFoundException(empno));

        UserRole userRole = user.getUserRoles().stream()
                .max(Comparator.comparingInt(a -> a.getRole().getPermissionLevel()))
                .orElseThrow(() -> new RuntimeException("역할 정보가 없습니다."));

        var role = userRole.getRole();
        var team = userRole.getTeam();

        // 3) LoginResponse 형태로 응답 DTO 구성
        LoginResponse dto = LoginResponse.builder()
                .username(user.getUsername())
                .roleCode(role.getCode())
                .roleName(role.getName())
                .teamId(team != null ? team.getId() : null)
                .teamName(team != null ? team.getName() : "GLOBAL")
                .build();

        // 4) { success: true, data: {...} } 형태로 반환
        return ApiResponse.success(dto);
    }
}
