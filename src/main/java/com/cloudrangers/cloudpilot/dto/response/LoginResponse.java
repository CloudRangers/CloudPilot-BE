package com.cloudrangers.cloudpilot.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class LoginResponse {

    private String username;
    private String roleCode;
    private String roleName;
    private Long teamId;
    private String teamName;


    @Builder
    public LoginResponse(
            String username,
            String roleCode,
            String roleName,
            Long teamId,
            String teamName
    ) {
        this.username = username;
        this.roleCode = roleCode;
        this.roleName = roleName;
        this.teamId = teamId;
        this.teamName = teamName;
    }
}
