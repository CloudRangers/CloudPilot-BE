// src/main/java/com/cloudrangers/cloudpilot/dto/user/HeadTeamResponse.java
package com.cloudrangers.cloudpilot.dto.user;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class HeadTeamResponse {

    private String teamName;       // "A팀"
    private String teamLeader;     // "이팀장 (TL-2024-001)" 형식

    private List<HeadTeamMemberResponse> members;
}
