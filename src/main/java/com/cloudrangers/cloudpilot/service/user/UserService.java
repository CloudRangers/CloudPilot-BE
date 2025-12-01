package com.cloudrangers.cloudpilot.service.user;

import com.cloudrangers.cloudpilot.dto.request.LoginRequest;
import com.cloudrangers.cloudpilot.dto.response.LoginResponse;
import com.cloudrangers.cloudpilot.dto.response.TokenRefreshResponse;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

public interface UserService {
    LoginResponse login(LoginRequest request);

    void logout(HttpServletRequest request);

    void sendPasswordResetEmail(String email);

    void confirmPasswordReset(String token, String newPassword);

    void changePassword(String currentPassword, String newPassword);

    void updateEmail(Long userId, String newEmail);

    Map<String, Object> buildClaims(String empno);

    TokenRefreshResponse refresh(HttpServletRequest request);

    LoginResponse getMyInfo(HttpServletRequest request);
}