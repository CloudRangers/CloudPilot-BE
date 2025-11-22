package com.cloudrangers.cloudpilot.service.pkg;

import com.cloudrangers.cloudpilot.domain.pkg.PkgApproval;
import com.cloudrangers.cloudpilot.domain.pkg.PkgRequest;
import com.cloudrangers.cloudpilot.domain.user.User;
import com.cloudrangers.cloudpilot.dto.request.PkgRequestCreateRequest;
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

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class PackageService {

    private final PkgRequestRepository pkgRequestRepository;
    private final PkgApprovalRepository pkgApprovalRepository;
    private final UserRepository userRepository;
    private final PermissionChecker permissionChecker;

    // 1) 패키지 설치 요청 생성
    @Transactional
    public PkgRequestResponse createRequest(PkgRequestCreateRequest reqDto, Long requesterId) {

        // 1. 요청한 사용자 찾기
        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + requesterId));

        // 2. 이 사람이 "팀장 권한(L1 승인 가능)"인지 확인
        //    - 팀장이면: 요청 넣자마자 L1 승인된 상태로 저장
        //    - 팀장이 아니면: 그냥 pending 상태로 저장
        PkgRequestStatus initialStatus;
        if (permissionChecker.canApproveL1(requesterId)) {
            initialStatus = PkgRequestStatus.l1_approved;
        } else {
            initialStatus = PkgRequestStatus.pending;
        }

        // 3. 요청 엔티티 생성
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

        return PkgRequestResponse.from(saved);
    }

    // === 아래 L1/FINAL 승인/거절 메서드는 그대로 두면 됨 ===

    @Transactional
    public PkgRequestResponse approveL1(Long requestId, Long approverId) {
        if (!permissionChecker.canApproveL1(approverId)) {
            throw new IllegalStateException("L1 approval not allowed for user=" + approverId);
        }

        PkgRequest request = getRequestOrThrow(requestId);

        if (request.getStatus() != PkgRequestStatus.pending) {
            throw new IllegalStateException("Only pending requests can be L1-approved");
        }

        User approver = userRepository.findById(approverId)
                .orElseThrow(() -> new EntityNotFoundException("Approver not found: " + approverId));

        PkgApproval approval = PkgApproval.builder()
                .pkgRequest(request)
                .step(PkgApprovalStep.L1)
                .approver(approver)
                .result(PkgApprovalResult.approved)
                .build();
        pkgApprovalRepository.save(approval);

        request.setStatus(PkgRequestStatus.l1_approved);
        pkgRequestRepository.save(request);

        return PkgRequestResponse.from(request);
    }

    @Transactional
    public PkgRequestResponse rejectL1(Long requestId, Long approverId) {
        if (!permissionChecker.canApproveL1(approverId)) {
            throw new IllegalStateException("L1 approval not allowed for user=" + approverId);
        }

        PkgRequest request = getRequestOrThrow(requestId);

        if (request.getStatus() != PkgRequestStatus.pending) {
            throw new IllegalStateException("Only pending requests can be rejected at L1");
        }

        User approver = userRepository.findById(approverId)
                .orElseThrow(() -> new EntityNotFoundException("Approver not found: " + approverId));

        PkgApproval approval = PkgApproval.builder()
                .pkgRequest(request)
                .step(PkgApprovalStep.L1)
                .approver(approver)
                .result(PkgApprovalResult.rejected)
                .build();
        pkgApprovalRepository.save(approval);

        request.setStatus(PkgRequestStatus.rejected);
        request.setDecidedAt(Instant.now());
        pkgRequestRepository.save(request);

        return PkgRequestResponse.from(request);
    }

    @Transactional
    public PkgRequestResponse approveFinal(Long requestId, Long approverId) {
        if (!permissionChecker.canApproveFinal(approverId)) {
            throw new IllegalStateException("Final approval not allowed for user=" + approverId);
        }

        PkgRequest request = getRequestOrThrow(requestId);

        if (request.getStatus() != PkgRequestStatus.l1_approved &&
                request.getStatus() != PkgRequestStatus.pending) {
            throw new IllegalStateException("Only pending or l1_approved requests can be finally approved");
        }

        User approver = userRepository.findById(approverId)
                .orElseThrow(() -> new EntityNotFoundException("Approver not found: " + approverId));

        PkgApproval approval = PkgApproval.builder()
                .pkgRequest(request)
                .step(PkgApprovalStep.FINAL)
                .approver(approver)
                .result(PkgApprovalResult.approved)
                .build();
        pkgApprovalRepository.save(approval);

        request.setStatus(PkgRequestStatus.approved);
        request.setDecidedAt(Instant.now());
        pkgRequestRepository.save(request);

        return PkgRequestResponse.from(request);
    }

    @Transactional
    public PkgRequestResponse rejectFinal(Long requestId, Long approverId) {
        if (!permissionChecker.canApproveFinal(approverId)) {
            throw new IllegalStateException("Final approval not allowed for user=" + approverId);
        }

        PkgRequest request = getRequestOrThrow(requestId);

        if (request.getStatus() == PkgRequestStatus.rejected) {
            throw new IllegalStateException("Already rejected");
        }

        User approver = userRepository.findById(approverId)
                .orElseThrow(() -> new EntityNotFoundException("Approver not found: " + approverId));

        PkgApproval approval = PkgApproval.builder()
                .pkgRequest(request)
                .step(PkgApprovalStep.FINAL)
                .approver(approver)
                .result(PkgApprovalResult.rejected)
                .build();
        pkgApprovalRepository.save(approval);

        request.setStatus(PkgRequestStatus.rejected);
        request.setDecidedAt(Instant.now());
        pkgRequestRepository.save(request);

        return PkgRequestResponse.from(request);
    }

    private PkgRequest getRequestOrThrow(Long id) {
        return pkgRequestRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("PkgRequest not found: " + id));
    }
}
