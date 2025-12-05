// src/main/java/com/cloudrangers/cloudpilot/dto/user/AssignedMemberResponse.java
package com.cloudrangers.cloudpilot.dto.user;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AssignedMemberResponse {
    private Long userId;
    private String username;
    private String employeeId;  // "EMP-9003"
    private String roleCode;    // LEADER / MEMBER
    private String roleName;    // 팀장 / 팀원
}
