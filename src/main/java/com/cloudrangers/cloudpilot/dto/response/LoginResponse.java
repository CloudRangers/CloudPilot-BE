package com.cloudrangers.cloudpilot.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class LoginResponse {

    // 🔹 토큰 필드 추가
    private String accessToken;
    private String refreshToken;

    private String username;
    private String roleCode;
    private String roleName;
    private Long teamId;
    private String teamName;

    @Builder
    public LoginResponse(
            String accessToken, //토큰 추가
            String refreshToken, //추가
            String username,
            String roleCode,
            String roleName,
            Long teamId,
            String teamName
    ) {
        this.accessToken = accessToken; //추가
        this.refreshToken = refreshToken; //추가
        this.username = username;
        this.roleCode = roleCode;
        this.roleName = roleName;
        this.teamId = teamId;
        this.teamName = teamName;
    }
}