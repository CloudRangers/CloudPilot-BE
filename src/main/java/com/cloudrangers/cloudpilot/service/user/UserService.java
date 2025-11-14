package com.cloudrangers.cloudpilot.service.user;

import com.cloudrangers.cloudpilot.dto.request.LoginRequest;
import com.cloudrangers.cloudpilot.dto.response.LoginResponse;

public interface UserService {
    LoginResponse login(LoginRequest request);
    void logout(String token);
    void sendPasswordResetEmail(String email);
    void confirmPasswordReset(String token, String newPassword);
    void changePassword(String currentPassword, String newPassword);
    void updateEmail(Long userId, String newEmail);
}
