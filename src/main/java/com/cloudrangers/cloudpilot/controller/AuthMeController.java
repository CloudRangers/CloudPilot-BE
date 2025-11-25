package com.cloudrangers.cloudpilot.controller;

import com.cloudrangers.cloudpilot.common.ApiResponse;
import com.cloudrangers.cloudpilot.domain.user.User;
import com.cloudrangers.cloudpilot.domain.user.UserRole;
import com.cloudrangers.cloudpilot.dto.response.LoginResponse;
import com.cloudrangers.cloudpilot.exception.notfound.UserNotFoundException;
import com.cloudrangers.cloudpilot.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;

@RestController
@RequiredArgsConstructor
public class AuthMeController {

    private final UserRepository userRepository;

    @GetMapping("/auth/me")
    public ApiResponse<LoginResponse> me() {

        // 1) SecurityContext 에서 현재 사용자(empno) 꺼내기
        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();

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
