package com.cloudrangers.cloudpilot.controller;

import com.cloudrangers.cloudpilot.dto.response.TeamResponse;
import com.cloudrangers.cloudpilot.service.user.TeamService;
import com.cloudrangers.cloudpilot.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
@Tag(name = "Team", description = "팀 관리 API")
public class TeamController {

    private final TeamService teamService;

    /**
     * 전체 팀 목록 조회
     * GET /api/teams
     */
    @GetMapping
    @Operation(summary = "전체 팀 목록 조회", description = "등록된 모든 팀 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<List<TeamResponse>>> getAllTeams() {
        log.info("[TeamController] GET /api/teams - 전체 팀 목록 조회");

        List<TeamResponse> teams = teamService.getAllTeams();

        return ResponseEntity.ok(ApiResponse.success(teams));
    }

    /**
     * 팀 단건 조회 (ID)
     * GET /api/teams/{teamId}
     */
    @GetMapping("/{teamId}")
    @Operation(summary = "팀 단건 조회", description = "팀 ID로 특정 팀 정보를 조회합니다.")
    public ResponseEntity<ApiResponse<TeamResponse>> getTeamById(
            @Parameter(description = "팀 ID") @PathVariable Long teamId) {
        log.info("[TeamController] GET /api/teams/{} - 팀 단건 조회", teamId);

        TeamResponse team = teamService.getTeamById(teamId);

        return ResponseEntity.ok(ApiResponse.success(team));
    }
}