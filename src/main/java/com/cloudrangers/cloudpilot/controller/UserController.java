package com.cloudrangers.cloudpilot.controller;

import com.cloudrangers.cloudpilot.common.ApiResponse;
import com.cloudrangers.cloudpilot.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /** 이메일 변경 */
    @PatchMapping("/{userId}/email")
    public ApiResponse<Void> updateEmail(
            @PathVariable Long userId,
            @RequestParam String newEmail
    ) {
        userService.updateEmail(userId, newEmail);
        return ApiResponse.success(null);
    }
}