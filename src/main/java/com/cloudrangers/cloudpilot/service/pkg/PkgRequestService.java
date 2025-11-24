package com.cloudrangers.cloudpilot.service.pkg;

import com.cloudrangers.cloudpilot.domain.pkg.PkgApproval;
import com.cloudrangers.cloudpilot.domain.pkg.PkgRequest;
import com.cloudrangers.cloudpilot.domain.user.User;
import com.cloudrangers.cloudpilot.dto.request.PkgApprovalActionRequest;
import com.cloudrangers.cloudpilot.dto.request.PkgRequestCreateRequest;
import com.cloudrangers.cloudpilot.dto.response.PkgRequestDetailResponse;
import com.cloudrangers.cloudpilot.dto.response.PkgRequestResponse;
import com.cloudrangers.cloudpilot.enums.PkgApprovalResult;
import com.cloudrangers.cloudpilot.enums.PkgApprovalStep;
import com.cloudrangers.cloudpilot.enums.PkgRequestStatus;
import com.cloudrangers.cloudpilot.repository.pkg.PkgApprovalRepository;
import com.cloudrangers.cloudpilot.repository.pkg.PkgRequestRepository;
import com.cloudrangers.cloudpilot.repository.user.UserRepository;
import com.cloudrangers.cloudpilot.security.PermissionChecker;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
// 추가 import
import com.cloudrangers.cloudpilot.domain.user.UserRole;
import com.cloudrangers.cloudpilot.repository.user.UserRoleRepository;
import com.cloudrangers.cloudpilot.dto.response.PkgApprovalHistoryResponse;


import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PkgRequestService {

    private final PkgRequestRepository pkgRequestRepository;
    private final PkgApprovalRepository pkgApprovalRepository;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final PermissionChecker permissionChecker;

    /**
     * 1) 패키지 설치 요청 생성
     *
     * - 일반 사용자: status = pending
     * - 팀장(L1 승인 권한 있는 사용자): status = l1_approved 로 바로 저장
     * - 부장(L2 승인 권한 있는 사용자): status = approved (+pkg_approval FINAL 이력도 저장)
     */
    @Transactional
    public PkgRequestResponse createRequest(PkgRequestCreateRequest reqDto, Long requesterId) {

        // 1. 요청자 조회
        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + requesterId));

        // 2. 이 유저의 권한에 따라 기본 상태 결정
        PkgRequestStatus initialStatus;

        boolean isFinalApprover = permissionChecker.canApproveFinal(requesterId);
        boolean isL1Approver    = permissionChecker.canApproveL1(requesterId);

        if (isFinalApprover) {
            // 🔹 부장(HEAD) 이면 바로 최종 승인 상태로
            initialStatus = PkgRequestStatus.approved;
        } else if (isL1Approver) {
            // 🔹 팀장(L1) 이면 L1 승인된 상태로
            initialStatus = PkgRequestStatus.l1_approved;
        } else {
            // 🔹 일반 유저는 pending
            initialStatus = PkgRequestStatus.pending;
        }

        // 3. pkg_request 엔터티 생성
        PkgRequest request = PkgRequest.builder()
                .requestedBy(requester)
                .packageName(reqDto.getPackageName())
                .packageVer(reqDto.getPackageVer())
                .description(reqDto.getDescription())
                .status(initialStatus)
                .build();

        // 4. 저장
        PkgRequest saved = pkgRequestRepository.save(request);
        log.info("Created pkg_request id={} by user={}, initialStatus={}",
                saved.getId(), requesterId, initialStatus);

        // 5. 만약 부장이 직접 신청해서 바로 최종 승인된 경우 → 승인 이력도 남기고 싶으면 여기서 처리
        if (isFinalApprover) {
            PkgApproval autoApproval = PkgApproval.builder()
                    .pkgRequest(saved)
                    .step(PkgApprovalStep.FINAL)
                    .approver(requester)
                    .result(PkgApprovalResult.approved)
                    .description(saved.getDescription())   // ⭐ 요청자가 적은 description 사용
                    .decidedAt(Instant.now())
                    .build();

            pkgApprovalRepository.save(autoApproval);

            saved.setDecidedAt(Instant.now());
            pkgRequestRepository.save(saved);
        }

        // 6. 팀장인데 부장은 아닌 경우: L1 승인 이력 자동 생성
        if (!isFinalApprover && isL1Approver) {
            PkgApproval autoL1 = PkgApproval.builder()
                    .pkgRequest(saved)
                    .step(PkgApprovalStep.L1)
                    .approver(requester)
                    .result(PkgApprovalResult.approved)
                    // 🔥 승인 사유도 요청 description 그대로
                    .description(saved.getDescription())
                    .decidedAt(Instant.now())
                    .build();
            pkgApprovalRepository.save(autoL1);
        }


        return PkgRequestResponse.from(saved);
    }

    /**
     * 2) L1 승인/거절 공통 처리
     *  - action: "approve" or "reject"
     *  - reason: 승인/거절 사유 (pkg_approval.description에 저장)
     */
    @Transactional
    public PkgRequestResponse approveOrReject(
            Long requestId,
            Long approverId,
            PkgApprovalActionRequest body
    ) {

        // 부장 권한이 있으면 FINAL 처리
        if (permissionChecker.canApproveFinal(approverId)) {
            return handleFinalApproval(requestId, approverId, body);
        }

        // 팀장 권한이 있으면 L1 처리
        if (permissionChecker.canApproveL1(approverId)) {
            return handleL1Approval(requestId, approverId, body);
        }

        // 둘 다 아니면 권한 없음
        throw new IllegalStateException("권한이 존재하지 않는 user: " + approverId);
    }
    @Transactional
    public PkgRequestResponse handleL1Approval(Long requestId, Long approverId, PkgApprovalActionRequest body) {

        if (!permissionChecker.canApproveL1(approverId)) {
            throw new IllegalStateException("L1 승인 권한이 존재하지 않습니다. userId=" + approverId);
        }

        // 우선 요청 엔티티는 읽어서, 승인 이력 작성용으로 사용
        PkgRequest request = getRequestOrThrow(requestId);

        User approver = userRepository.findById(approverId)
                .orElseThrow(() -> new EntityNotFoundException("Approver not found: " + approverId));

        String action = body.getAction();
        String reason = body.getReason(); // nullable 가능

        Instant now = Instant.now();

        if ("approve".equalsIgnoreCase(action)) {
            // 🔹 DB 조건부 UPDATE: pending → l1_approved 인 경우에만 1 row 업데이트
            int updated = pkgRequestRepository.updateStatusIfMatches(
                    requestId,
                    PkgRequestStatus.pending,       // 기대 상태
                    PkgRequestStatus.l1_approved,   // 바꿀 상태
                    now
            );

            if (updated == 0) {
                // 누가 먼저 처리했거나, 이미 상태가 바뀐 케이스
                throw new IllegalStateException("이미 다른 승인자가 먼저 처리한 요청입니다. (L1)");
            }

            // 승인 이력 저장
            PkgApproval approval = PkgApproval.builder()
                    .pkgRequest(request)
                    .step(PkgApprovalStep.L1)
                    .approver(approver)
                    .result(PkgApprovalResult.approved)
                    .description(reason)
                    .decidedAt(now)
                    .build();
            pkgApprovalRepository.save(approval);

            // 메모리 상 엔티티도 DB 상태와 맞춰 줌
            request.setStatus(PkgRequestStatus.l1_approved);
            request.setDecidedAt(now);

        } else if ("reject".equalsIgnoreCase(action)) {

            int updated = pkgRequestRepository.updateStatusIfMatches(
                    requestId,
                    PkgRequestStatus.pending,       // pending인 경우에만 거절 가능
                    PkgRequestStatus.rejected,
                    now
            );

            if (updated == 0) {
                throw new IllegalStateException("이미 다른 승인자가 먼저 처리한 요청입니다. (L1 거절)");
            }

            PkgApproval approval = PkgApproval.builder()
                    .pkgRequest(request)
                    .step(PkgApprovalStep.L1)
                    .approver(approver)
                    .result(PkgApprovalResult.rejected)
                    .description(reason)
                    .decidedAt(now)
                    .build();
            pkgApprovalRepository.save(approval);

            request.setStatus(PkgRequestStatus.rejected);
            request.setDecidedAt(now);

        } else {
            throw new IllegalArgumentException("Invalid action for L1: " + action);
        }

        return PkgRequestResponse.from(request);
    }


    /**
     * 3) FINAL(부장) 승인/거절 공통 처리
     */
    @Transactional
    public PkgRequestResponse handleFinalApproval(Long requestId, Long approverId, PkgApprovalActionRequest body) {

        if (!permissionChecker.canApproveFinal(approverId)) {
            throw new IllegalStateException("Final approval not allowed for user=" + approverId);
        }

        // 이 시점의 상태를 보고 유효한 상태인지 먼저 체크
        PkgRequest request = getRequestOrThrow(requestId);

        if (request.getStatus() != PkgRequestStatus.l1_approved
                && request.getStatus() != PkgRequestStatus.pending) {
            throw new IllegalStateException("FINAL 승인/반려는 pending 또는 l1_approved 상태에서만 가능합니다.");
        }

        User approver = userRepository.findById(approverId)
                .orElseThrow(() -> new EntityNotFoundException("승인자를 찾을 수 없습니다: " + approverId));

        String action = body.getAction();
        String reason = body.getReason();
        Instant now = Instant.now();

        // 현재 읽은 상태를 expectedStatus로 사용
        PkgRequestStatus currentStatus = request.getStatus();

        if ("approve".equalsIgnoreCase(action)) {

            int updated = pkgRequestRepository.updateStatusIfMatches(
                    requestId,
                    currentStatus,              // 지금 읽은 상태와 동일할 때만 승인
                    PkgRequestStatus.approved,  // 최종 승인
                    now
            );

            if (updated == 0) {
                throw new IllegalStateException("이미 다른 승인자가 먼저 처리한 요청입니다. (FINAL 승인)");
            }

            PkgApproval approval = PkgApproval.builder()
                    .pkgRequest(request)
                    .step(PkgApprovalStep.FINAL)
                    .approver(approver)
                    .result(PkgApprovalResult.approved)
                    .description(reason)
                    .decidedAt(now)
                    .build();
            pkgApprovalRepository.save(approval);

            request.setStatus(PkgRequestStatus.approved);
            request.setDecidedAt(now);

            // TODO: 여기서 ans_run 트리거 등 실제 설치 작업 호출 가능
            // ansibleService.triggerInstall(request);

        } else if ("reject".equalsIgnoreCase(action)) {

            if (request.getStatus() == PkgRequestStatus.rejected) {
                throw new IllegalStateException("이미 거절된 요청입니다.");
            }

            int updated = pkgRequestRepository.updateStatusIfMatches(
                    requestId,
                    currentStatus,              // 현재 상태에서만 거절 처리
                    PkgRequestStatus.rejected,
                    now
            );

            if (updated == 0) {
                throw new IllegalStateException("이미 다른 승인자가 먼저 처리한 요청입니다. (FINAL 거절)");
            }

            PkgApproval approval = PkgApproval.builder()
                    .pkgRequest(request)
                    .step(PkgApprovalStep.FINAL)
                    .approver(approver)
                    .result(PkgApprovalResult.rejected)
                    .description(reason)
                    .decidedAt(now)
                    .build();
            pkgApprovalRepository.save(approval);

            request.setStatus(PkgRequestStatus.rejected);
            request.setDecidedAt(now);

        } else {
            throw new IllegalArgumentException("Invalid action for FINAL: " + action);
        }

        return PkgRequestResponse.from(request);
    }

    @Transactional(readOnly = true)
    public List<PkgRequestResponse> getPendingRequestsByTeam(Long teamId) {
        // 팀 단위의 pending 요청들 조회 (팀장 화면 등에서 사용)
        List<PkgRequest> list = pkgRequestRepository.findTeamPendingRequests(
                teamId,
                PkgRequestStatus.pending
        );

        return list.stream()
                .map(PkgRequestResponse::from)
                .toList();
    }

    // 1) 내가 올린 요청들
    @Transactional(readOnly = true)
    public List<PkgRequestResponse> getMyRequests(Long userId) {
        List<PkgRequest> list = pkgRequestRepository.findByRequestedBy_Id(userId);
        return list.stream().map(PkgRequestResponse::from).toList();
    }
    /**
     * 🔍 패키지 요청 1건 상세 조회
     * - 일반유저 : 자기 요청만
     * - 팀장     : 자기 + 자기 팀원 요청
     * - 부장     : 전체
     */
    @Transactional(readOnly = true)
    public PkgRequestDetailResponse getRequestDetail(Long requestId, Long viewerId) {

        // 1) 요청 엔티티 가져오기
        PkgRequest request = pkgRequestRepository.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException("PkgRequest not found: " + requestId));

        Long ownerId = request.getRequestedBy().getId();

        // 2) 이 사람이 이 요청을 볼 수 있는지 권한 체크
        if (!canViewRequestDetail(viewerId, request)) {
            throw new RuntimeException("이 요청을 조회할 권한이 없습니다.");
        }

        // 3) 승인/반려 이력 조회
        var approvals = pkgApprovalRepository
                .findByPkgRequest_IdOrderByDecidedAtAsc(requestId)
                .stream()
                .map(PkgApprovalHistoryResponse::from)
                .toList();

        // 4) DTO 조합
        return PkgRequestDetailResponse.of(request, approvals);
    }

    /**
     * 내부 권한 체크 로직
     */
    private boolean canViewRequestDetail(Long viewerId, PkgRequest request) {

        Long ownerId = request.getRequestedBy().getId();

        // 1) 내가 올린 요청이면 무조건 OK
        if (viewerId.equals(ownerId)) {
            return true;
        }

        // 2) 부장(최종 승인 권한 있는 사람)은 전체 조회 가능
        if (permissionChecker.canApproveFinal(viewerId)) {
            return true;
        }

        // 3) 팀장(L1 승인 권한 있는 사람)은 "자기 팀 소속 유저의 요청"은 볼 수 있음
        if (permissionChecker.canApproveL1(viewerId)) {
            // 나(팀장)의 팀들
            var myRoles = userRoleRepository.findByUserId(viewerId);
            var myTeamIds = myRoles.stream()
                    .map(ur -> ur.getTeam().getId())
                    .distinct()
                    .toList();

            // 요청 올린 사람의 팀들
            var ownerRoles = userRoleRepository.findByUserId(ownerId);
            var ownerTeamIds = ownerRoles.stream()
                    .map(ur -> ur.getTeam().getId())
                    .distinct()
                    .toList();

            // 교집합이 있으면 같은 팀이라고 판단
            boolean sameTeam = ownerTeamIds.stream().anyMatch(myTeamIds::contains);
            if (sameTeam) {
                return true;
            }
        }

        // 4) 그 외(다른 팀 일반 유저)는 볼 수 없음
        return false;
    }





    // 2) 내가 결재한 이력 (팀장: L1, 부장: FINAL)
    @Transactional(readOnly = true)
    public List<PkgRequestResponse> getMyApprovalHistory(Long userId) {

        // 팀장/부장 여부는 PermissionChecker로 판단
        boolean isFinal = permissionChecker.canApproveFinal(userId);
        PkgApprovalStep step = isFinal ? PkgApprovalStep.FINAL : PkgApprovalStep.L1;

        List<PkgApproval> approvals =
                pkgApprovalRepository.findByApprover_IdAndStep(userId, step);

        return approvals.stream()
                .map(a -> PkgRequestResponse.from(a.getPkgRequest()))
                .toList();
    }

    /**
     * 3) 지금 내가 결재해야 하는 것들 (To-do)
     *
     *  - 부장(HEAD):
     *      상태가 l1_approved 인 요청 전체
     *
     *  - 팀장(L1):
     *      내 팀(team_id)에 속한 유저들이 올린 pending 요청들
     *
     *  - 사원:
     *      결재할 To-do 없음 → 빈 리스트
     */

    @Transactional(readOnly = true)
    public List<PkgRequestResponse> getMyTodoApprovals(Long userId) {

        // ✅ 부장
        if (permissionChecker.canApproveFinal(userId)) {
            List<PkgRequest> list =
                    pkgRequestRepository.findByStatusOrderByRequestedAtDesc(PkgRequestStatus.l1_approved);

            return list.stream()
                    .map(PkgRequestResponse::from)
                    .toList();
        }

        // ✅ 팀장
        if (permissionChecker.canApproveL1(userId)) {

            List<UserRole> roles = userRoleRepository.findByUserId(userId);
            if (roles.isEmpty()) {
                throw new EntityNotFoundException("UserRole not found for userId=" + userId);
            }

            Long teamId = roles.get(0).getTeam().getId();

            List<PkgRequest> list =
                    pkgRequestRepository.findTeamPendingRequests(teamId, PkgRequestStatus.pending);

            return list.stream()
                    .map(PkgRequestResponse::from)
                    .toList();
        }

        // ✅ 사원
        return List.of();
    }



    // === 공통 조회 메서드 ===
    private PkgRequest getRequestOrThrow(Long id) {
        return pkgRequestRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("PkgRequest not found: " + id));
    }
}
