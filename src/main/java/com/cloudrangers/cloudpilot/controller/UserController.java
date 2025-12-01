// src/main/java/com/cloudrangers/cloudpilot/controller/UserController.java
package com.cloudrangers.cloudpilot.controller;

import com.cloudrangers.cloudpilot.common.ApiResponse;
import com.cloudrangers.cloudpilot.dto.user.MyPageResponse;
import com.cloudrangers.cloudpilot.security.CustomUserDetails;
import com.cloudrangers.cloudpilot.service.user.MyPageService;
import com.cloudrangers.cloudpilot.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final MyPageService myPageService;  // 🔹 마이페이지용 서비스

    /** 📧 이메일 변경 */
    @PatchMapping("/{userId}/email")
    public ApiResponse<Void> updateEmail(
            @PathVariable Long userId,
            @RequestParam String newEmail
    ) {
        userService.updateEmail(userId, newEmail);
        return ApiResponse.success(null);
    }

    /** 👤 로그인 사용자 기준 마이페이지 정보 조회 */
    @GetMapping("/me")
    public ApiResponse<MyPageResponse> getMyPage(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        // ⚠️ /users/me 를 permitAll 해두면 userDetails가 null일 수 있음 (로컬 디버깅 시 주의)
        Long userId = userDetails.getUserId();   // JWT에서 넣어둔 userId 사용
        MyPageResponse response = myPageService.getMyPage(userId);
        return ApiResponse.success(response);
    }
}
