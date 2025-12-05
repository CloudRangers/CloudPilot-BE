package com.cloudrangers.cloudpilot.service.user;

import com.cloudrangers.cloudpilot.dto.response.TeamResponse;
import com.cloudrangers.cloudpilot.domain.user.Team;
import com.cloudrangers.cloudpilot.repository.vm.TeamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeamService {

    private final TeamRepository teamRepository;

    /**
     * 모든 팀 목록 조회 (이름순 정렬)
     */
    public List<TeamResponse> getAllTeams() {
        log.info("[TeamService] 전체 팀 목록 조회");

        List<Team> teams = teamRepository.findAllByOrderByNameAsc();

        return teams.stream()
                .map(TeamResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 팀 ID로 단건 조회
     */
    public TeamResponse getTeamById(Long teamId) {
        log.info("[TeamService] 팀 조회 - teamId: {}", teamId);

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 팀입니다. teamId: " + teamId));

        return TeamResponse.from(team);
    }

    /**
     * 팀 이름으로 단건 조회
     */
    public TeamResponse getTeamByName(String name) {
        log.info("[TeamService] 팀 조회 - name: {}", name);

        Team team = teamRepository.findByName(name)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 팀입니다. name: " + name));

        return TeamResponse.from(team);
    }

    /**
     * 팀 존재 여부 확인
     */
    public boolean existsById(Long teamId) {
        return teamRepository.existsById(teamId);
    }
}
