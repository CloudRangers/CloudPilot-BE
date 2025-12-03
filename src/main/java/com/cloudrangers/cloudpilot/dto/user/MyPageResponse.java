// src/main/java/com/cloudrangers/cloudpilot/dto/user/MyPageResponse.java
package com.cloudrangers.cloudpilot.dto.user;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class MyPageResponse {

    // --- 사용자 기본 정보 ---
    private Long userId;
    private Long empno;
    private String username;
    private String email;

    private String roleCode;   // "ADMIN", "HEAD", "LEADER", "MEMBER"
    private String roleName;   // 한글 이름 (예: "팀장", "부장")
    private Long teamId;
    private String teamName;

    // --- 사용자 VM 목록 ---
    private List<MyPageVmResponse> vms;
}
