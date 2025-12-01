package com.cloudrangers.cloudpilot.dto.user;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class HeadTeamMemberResponse {

    private String teamMember;             // "홍길동 (EMP-2024-001)"
    private List<MyPageVmResponse> servers;
}
