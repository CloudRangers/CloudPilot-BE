package com.cloudrangers.cloudpilot.ops.security;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CurrentUser {
    private Long userId;
    private Long teamId;    // 기본 팀 하나 기준
    private String roleCode; // "ADMIN", "HEAD", "LEADER", "MEMBER"
}
