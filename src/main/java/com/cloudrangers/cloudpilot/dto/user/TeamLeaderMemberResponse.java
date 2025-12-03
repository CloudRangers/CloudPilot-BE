package com.cloudrangers.cloudpilot.dto.user;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class TeamLeaderMemberResponse {

    private String teamMember;                 // "홍길동 (EMP-2024-001)"
    private List<MyPageVmResponse> servers;    // 팀원 VM 리스트
}
