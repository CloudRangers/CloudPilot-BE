// src/main/java/com/cloudrangers/cloudpilot/service/user/MyPageService.java
package com.cloudrangers.cloudpilot.service.user;

import com.cloudrangers.cloudpilot.domain.user.Role;
import com.cloudrangers.cloudpilot.domain.user.User;
import com.cloudrangers.cloudpilot.domain.user.UserRole;
import com.cloudrangers.cloudpilot.domain.vm.VmInstance;
import com.cloudrangers.cloudpilot.dto.user.AssignedMemberResponse;
import com.cloudrangers.cloudpilot.dto.user.HeadMyPageResponse;
import com.cloudrangers.cloudpilot.dto.user.HeadTeamMemberResponse;
import com.cloudrangers.cloudpilot.dto.user.HeadTeamResponse;
import com.cloudrangers.cloudpilot.dto.user.MyPageResponse;
import com.cloudrangers.cloudpilot.dto.user.MyPageVmResponse;
import com.cloudrangers.cloudpilot.dto.user.TeamLeaderMemberResponse;
import com.cloudrangers.cloudpilot.dto.user.TeamLeaderMyPageResponse;
import com.cloudrangers.cloudpilot.repository.user.UserRepository;
import com.cloudrangers.cloudpilot.repository.user.UserRoleRepository;
import com.cloudrangers.cloudpilot.repository.vm.VmInstanceRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyPageService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final VmInstanceRepository vmInstanceRepository;
    private final ObjectMapper objectMapper;

    // ===== 공통 유틸 =====

    /**
     * tags JSON 문자열에서 fieldName 값 안전하게 꺼내는 헬퍼
     * 예: osType, ipAddress 등
     */
    private String extractFieldFromTags(String tagsJson, String fieldName) {
        if (tagsJson == null || tagsJson.isBlank()) return null;
        try {
            JsonNode node = objectMapper.readTree(tagsJson);
            if (node.has(fieldName)) {
                return node.get(fieldName).asText();
            }
        } catch (Exception e) {
            // 파싱 실패해도 마이페이지 전체가 깨지지 않도록 조용히 무시
            // TODO: 필요하면 log.warn(...) 추가
        }
        return null;
    }

    /**
     * 이 VM의 담당 팀원(팀장/팀원) 정보 계산
     * - VM.createdBy(=ownerId) + VM.teamId 기준
     * - 해당 팀에서 LEADER / MEMBER 역할만 인정
     * - 없으면 "미할당" → 빈 리스트 반환
     */
    private List<AssignedMemberResponse> resolveAssignedMembers(VmInstance vm) {

        Long ownerId = vm.getCreatedBy();
        Long teamId = vm.getTeamId();

        if (ownerId == null || teamId == null) {
            return List.of();
        }

        // owner + roles 로딩
        User owner = userRepository.findWithRolesById(ownerId).orElse(null);
        if (owner == null || owner.getUserRoles() == null) {
            return List.of();
        }

        // 이 팀(teamId)에서의 역할들 중 LEADER / MEMBER 만 필터
        Optional<Role> maybeRole = owner.getUserRoles().stream()
                .filter(ur -> ur.getTeam() != null &&
                        Objects.equals(ur.getTeam().getId(), teamId))
                .map(UserRole::getRole)
                .filter(Objects::nonNull)
                .filter(r ->
                        "LEADER".equalsIgnoreCase(r.getCode()) ||
                                "MEMBER".equalsIgnoreCase(r.getCode())
                )
                // 둘 다 있을 가능성 대비: permissionLevel 제일 높은 것 선택
                .max(Comparator.comparingInt(Role::getPermissionLevel));

        if (maybeRole.isEmpty()) {
            // 🔸 여기서 걸리면 → 이 VM의 owner는 ADMIN/HEAD 이거나,
            //   해당 팀의 LEADER/MEMBER가 아니라는 뜻 → "미할당" 취급
            return List.of();
        }

        Role role = maybeRole.get();

        AssignedMemberResponse dto = AssignedMemberResponse.builder()
                .userId(owner.getId())
                .username(owner.getUsername())
                .employeeId("EMP-" + owner.getEmpno())
                .roleCode(role.getCode())
                .roleName(role.getName())
                .build();

        // 지금은 담당자 1명 구조로 설계
        return List.of(dto);
    }

    /**
     * VmInstance → 마이페이지용 VM DTO 매핑
     *
     * VmInstance 필드 구조:
     *  - vcpu        : vCPU 개수
     *  - memoryMb    : 메모리 MB
     *  - rootDiskGb  : 디스크 GB
     *  - powerState  : "POWERED_ON" / "POWERED_OFF" ...
     *  - tags        : JSON 문자열 (osType, ipAddress 등 포함 가능)
     */
    private MyPageVmResponse toVmResponse(VmInstance vm) {

        LocalDateTime createdAt = null;
        if (vm.getCreatedAt() != null) {
            createdAt = LocalDateTime.ofInstant(vm.getCreatedAt(), ZoneId.systemDefault());
        }

        LocalDateTime updatedAt = null;
        if (vm.getUpdatedAt() != null) {
            updatedAt = LocalDateTime.ofInstant(vm.getUpdatedAt(), ZoneId.systemDefault());
        }

        // ✅ CPU / 메모리 / 디스크 실제 값 사용
        Integer vcpu = vm.getVcpu();
        Integer memoryMb = vm.getMemoryMb();
        Integer diskGb = vm.getRootDiskGb();

        int cpu = vcpu != null ? vcpu : 0;
        int memoryGb = memoryMb != null ? memoryMb / 1024 : 0;
        int storageGb = diskGb != null ? diskGb : 0;

        // ✅ tags JSON에서 osType / ipAddress 등 추출 (없으면 null)
        String osName = extractFieldFromTags(vm.getTags(), "osType");

        String ip = vm.getIp();

        // ip 컬럼이 비어있을 경우에만 tags 의 ipAddress 보조 사용
        if (ip == null || ip.isBlank()) {
            ip = extractFieldFromTags(vm.getTags(), "ipAddress");
        }

        // powerState → FE status 매핑
        String powerState = vm.getPowerState();
        String feStatus;
        if ("ON".equalsIgnoreCase(powerState)) {
            feStatus = "running";
        } else if ("OFF".equalsIgnoreCase(powerState)) {
            feStatus = "stopped";
        } else {
            feStatus = "pending";
        }

        // 🔹 여기서 담당 팀원 정보 계산
        List<AssignedMemberResponse> assignedMembers = resolveAssignedMembers(vm);

        return MyPageVmResponse.builder()
                .id(vm.getId())
                .name(vm.getName())
                .type(vm.getProviderType())
                .status(feStatus)
                .cpu(cpu)
                .memory(memoryGb)
                .storage(storageGb)
                .os(osName)
                .ipAddress(ip)
                .createdAt(createdAt)
                .lastUpdated(updatedAt)
                .ownerId(vm.getCreatedBy())
                .ownerName(null)
                .teamId(vm.getTeamId())
                .teamName(null)                      // 상위에서 이미 teamName을 알고 있으므로 여기선 생략
                .packages(List.of())                 // 패키지는 아직 없음
                .assignedMembers(assignedMembers)    // 🔹 새 필드
                .build();
    }


    /** 유저가 특정 roleCode(예: "LEADER")를 갖고 있는지 확인하는 헬퍼 */
    private boolean hasRole(User user, String roleCode) {
        if (user.getUserRoles() == null) return false;
        return user.getUserRoles().stream()
                .map(UserRole::getRole)
                .filter(Objects::nonNull)
                .anyMatch(r -> roleCode.equalsIgnoreCase(r.getCode()));
    }

    /**
     * ✅ 공통 헬퍼
     * - 특정 teamId에 속한 VM 중에서
     * - vm_instance.lifecycle = 'RUNNING' 인 VM만 조회
     * - 그 결과를 MyPageVmResponse 로 매핑해서 반환
     */
    private List<MyPageVmResponse> findRunningLifecycleVmsForTeam(Long teamId) {
        if (teamId == null) {
            return List.of();
        }

        // lifecycle 이 String 컬럼인 경우:

        List<VmInstance> vms = vmInstanceRepository
                .findByTeamIdAndLifecycleIgnoreCase(teamId, "RUNNING");
        // lifecycle 이 enum 인 경우라면 위 대신:
        // List<VmInstance> vms = vmInstanceRepository
        //        .findByTeamIdAndLifecycle(teamId, VmLifecycle.RUNNING);

        return vms.stream()
                .map(this::toVmResponse)
                .toList();
    }

    // ===== 1) 사원(MEMBER) 마이페이지 =====

    /**
     * 요구사항: 사원은 자기 팀의 VM 정보를 모니터링
     * → 로그인한 사용자의 teamId 기준으로 VmInstance 리스트 조회
     */
    public MyPageResponse getMyPage(Long userId) {

        User user = userRepository.findWithRolesById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다. id=" + userId));

        String roleCode = null;
        String roleName = null;
        Long teamId = null;
        String teamName = null;

        if (user.getUserRoles() != null && !user.getUserRoles().isEmpty()) {
            UserRole userRole = user.getUserRoles().get(0);
            if (userRole.getRole() != null) {
                roleCode = userRole.getRole().getCode();   // 예: "MEMBER"
                roleName = userRole.getRole().getName();   // 예: "팀원"
            }
            if (userRole.getTeam() != null) {
                teamId = userRole.getTeam().getId();
                teamName = userRole.getTeam().getName();
            }
        }

        List<MyPageVmResponse> vmList = List.of();
        if (teamId != null) {
            vmList = vmInstanceRepository.findByTeamId(teamId).stream()
                    .map(this::toVmResponse)
                    .toList();
        }

        return MyPageResponse.builder()
                .userId(user.getId())
                .empno(user.getEmpno())
                .username(user.getUsername())
                .email(user.getEmail())
                .roleCode(roleCode)
                .roleName(roleName)
                .teamId(teamId)
                .teamName(teamName)
                .vms(vmList)
                .build();
    }

    // ===== 2) 팀장(LEADER) 마이페이지 =====

    /**
     * 팀장: 내 팀의 팀원 + 각 팀원별로 lifecycle = RUNNING 인 VM 목록
     */
    public TeamLeaderMyPageResponse getTeamLeaderMyPage(Long userId) {

        User leader = userRepository.findWithRolesById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다. id=" + userId));

        String roleName = null;
        String teamName = null;
        Long teamId = null;

        if (leader.getUserRoles() != null && !leader.getUserRoles().isEmpty()) {
            UserRole userRole = leader.getUserRoles().get(0);
            if (userRole.getRole() != null) {
                roleName = userRole.getRole().getName(); // 예: "팀장"
            }
            if (userRole.getTeam() != null) {
                teamId = userRole.getTeam().getId();
                teamName = userRole.getTeam().getName(); // 예: "A팀"
            }
        }

        if (teamId == null) {
            // 팀이 없으면 빈 구조 반환 (FE 안터지게)
            return TeamLeaderMyPageResponse.builder()
                    .leaderName(leader.getUsername())
                    .leaderEmployeeId("TL-" + leader.getEmpno())
                    .department("개발본부")
                    .teamName(teamName != null ? teamName : "-")
                    .roleName(roleName != null ? roleName : "팀장")
                    .members(List.of())
                    .build();
        }

        // 1) 해당 팀 팀원들 (ADMIN/HEAD 제외, LEADER/MEMBER만)
        List<User> teamMembers = userRepository.findByUserRoles_Team_Id(teamId).stream()
                .filter(u -> hasRole(u, "LEADER") || hasRole(u, "MEMBER"))
                .toList();

        // 2) 팀의 전체 VM
        List<VmInstance> teamVms = vmInstanceRepository.findByTeamId(teamId);

        // 3) createdBy(=userId) 별로 VM 그룹핑
        Map<Long, List<MyPageVmResponse>> vmsByUserId = teamVms.stream()
                .filter(vm -> vm.getCreatedBy() != null)
                .map(this::toVmResponse)
                .collect(Collectors.groupingBy(
                        MyPageVmResponse::getOwnerId,
                        Collectors.toList()
                ));

        // 4) DTO로 매핑
        List<TeamLeaderMemberResponse> members = teamMembers.stream()
                .map(member -> TeamLeaderMemberResponse.builder()
                        .teamMember(member.getUsername() + " (EMP-" + member.getEmpno() + ")")
                        .servers(vmsByUserId.getOrDefault(member.getId(), List.of()))
                        .build())
                .toList();

        return TeamLeaderMyPageResponse.builder()
                .leaderName(leader.getUsername())
                .leaderEmployeeId("TL-" + leader.getEmpno())
                .department("개발본부")
                .teamName(teamName != null ? teamName : "A팀")
                .roleName(roleName != null ? roleName : "팀장")
                .members(members)
                .build();
    }

    // ===== 3) 부장(HEAD) 마이페이지 =====

    /**
     * 요구사항: 부장은 "여러 팀의 VM 정보"를 모니터링
     */
    public HeadMyPageResponse getHeadMyPage(Long userId) {

        User head = userRepository.findWithRolesById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다. id=" + userId));

        // 1) 전체 UserRole 조회 후, 팀 단위로 그룹핑
        List<UserRole> allRoles = userRoleRepository.findAll();

        Map<Long, List<UserRole>> rolesByTeamId = allRoles.stream()
                .filter(ur -> ur.getTeam() != null)
                .collect(Collectors.groupingBy(ur -> ur.getTeam().getId()));

        // 2) 팀별 DTO 구성
        List<HeadTeamResponse> headTeams = rolesByTeamId.entrySet().stream()
                .map(entry -> {
                    Long teamId = entry.getKey();
                    List<UserRole> roles = entry.getValue();

                    String teamName = roles.stream()
                            .map(r -> r.getTeam().getName())
                            .filter(Objects::nonNull)
                            .findFirst()
                            .orElse("팀");

                    // 팀원 유저 목록 (중복 제거)
                    List<User> teamUsers = roles.stream()
                            .map(UserRole::getUser)
                            .filter(Objects::nonNull)
                            .distinct()
                            .filter(u -> hasRole(u, "LEADER") || hasRole(u, "MEMBER"))
                            .toList();

                    // 팀 VM 목록
                    List<VmInstance> teamVms = vmInstanceRepository.findByTeamId(teamId);

                    Map<Long, List<MyPageVmResponse>> vmsByUserId = teamVms.stream()
                            .filter(vm -> vm.getCreatedBy() != null)
                            .map(this::toVmResponse)
                            .collect(Collectors.groupingBy(
                                    MyPageVmResponse::getOwnerId,
                                    Collectors.toList()
                            ));

                    // 팀원 + 서버 리스트 매핑
                    List<HeadTeamMemberResponse> members = teamUsers.stream()
                            .map(user -> HeadTeamMemberResponse.builder()
                                    .teamMember(user.getUsername() + " (EMP-" + user.getEmpno() + ")")
                                    .servers(vmsByUserId.getOrDefault(user.getId(), List.of()))
                                    .build())
                            .toList();

                    // 팀장 이름 (roleCode = "LEADER" 가정)
                    String teamLeader = teamUsers.stream()
                            .filter(u -> hasRole(u, "LEADER"))
                            .map(User::getUsername)
                            .findFirst()
                            .orElse("-");

                    return HeadTeamResponse.builder()
                            .teamName(teamName)
                            .teamLeader(teamLeader)
                            .members(members)
                            .build();
                })
                .toList();

        String roleName = (head.getUserRoles() == null
                || head.getUserRoles().isEmpty()
                || head.getUserRoles().get(0).getRole() == null)
                ? "부장"
                : head.getUserRoles().get(0).getRole().getName();

        return HeadMyPageResponse.builder()
                .managerName(head.getUsername())
                .managerEmployeeId("MGR-" + head.getEmpno())
                .department("개발본부")  // TODO: 실제 부서 필드 연결하면 교체
                .roleName(roleName)
                .teams(headTeams)
                .build();
    }
}
