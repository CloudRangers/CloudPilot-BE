package com.cloudrangers.cloudpilot.dto.user;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class TeamLeaderMyPageResponse {

    private String leaderName;
    private String leaderEmployeeId;
    private String department;
    private String teamName;
    private String roleName;

    private List<TeamLeaderMemberResponse> members;
}
