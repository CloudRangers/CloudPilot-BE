package com.cloudrangers.cloudpilot.dto.user;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class HeadMyPageResponse {

    private String managerName;
    private String managerEmployeeId;
    private String department;
    private String roleName;

    private List<HeadTeamResponse> teams;
}
