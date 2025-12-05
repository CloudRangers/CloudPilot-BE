package com.cloudrangers.cloudpilot.dto.response;

import com.cloudrangers.cloudpilot.domain.user.Team;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class TeamResponse {

    private Long id;
    private String name;
    private String description;
    private LocalDateTime createdAt;

    public static TeamResponse from(Team team) {
        return TeamResponse.builder()
                .id(team.getId())
                .name(team.getName())
                .description(team.getDescription())
                .createdAt(team.getCreatedAt())
                .build();
    }
}