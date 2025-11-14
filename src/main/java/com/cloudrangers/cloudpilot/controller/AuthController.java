package com.cloudrangers.cloudpilot.controller;

import com.cloudrangers.cloudpilot.common.ApiResponse;
import com.cloudrangers.cloudpilot.dto.request.LoginRequest;
import com.cloudrangers.cloudpilot.dto.response.LoginResponse;
import com.cloudrangers.cloudpilot.service.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    /** ✅ 로그인 */
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@RequestBody LoginRequest request) {
        LoginResponse response = userService.login(request);

        log.info("✅ 로그인 성공 - empno={}, role={}, team={}",
                request.getEmpno(), response.getRole(), response.getTeam());

        return ApiResponse.of(true, response, "로그인 성공");
    }

    /** ✅ 로그아웃 (Redis 블랙리스트 등록) */
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestHeader(value = "Authorization", required = false) String token) {
        userService.logout(token);
        log.info("🔒 로그아웃 완료 - token={}", token);
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
