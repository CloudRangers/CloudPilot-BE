package com.cloudrangers.cloudpilot.controller;

import com.cloudrangers.cloudpilot.common.ApiResponse;
import com.cloudrangers.cloudpilot.dto.response.DeleteVmResponse;
import com.cloudrangers.cloudpilot.dto.response.VmDetailResponse;
import com.cloudrangers.cloudpilot.dto.response.VmStatusResponse;
import com.cloudrangers.cloudpilot.security.AuthUtil;
import com.cloudrangers.cloudpilot.service.vm.VmDeleteService;
import com.cloudrangers.cloudpilot.service.vm.VmQueryService;
import com.cloudrangers.cloudpilot.service.vm.VmReadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import com.cloudrangers.cloudpilot.dto.common.PageResponse;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/vms")
@RequiredArgsConstructor
@Tag(name = "VM Management", description = "가상 머신 관리 API")
public class VmController {

    private final VmQueryService vmQueryService;
    private final VmReadService vmReadService;
    private final VmDeleteService vmDeleteService;

    /**
     * VM 리스트 조회 (필터/정렬/태그)
     */
    @GetMapping
    @Operation(summary = "VM 목록 조회", description = "필터, 정렬, 태그 조건으로 VM 목록을 조회합니다")
    public ApiResponse<PageResponse<VmStatusResponse>> getVms(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String providerType,
            @RequestParam(required = false) Long zoneId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String powerState,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long ownerUserId,
            @RequestParam(required = false) Long teamId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdFrom,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdTo,
            @RequestParam(required = false, defaultValue = "createdAt,desc") String sort,
            @RequestParam Map<String, String> allParams // tag.* 추출용
    ) {
        Map<String, String> tagEquals = new HashMap<>();
        allParams.forEach((k, v) -> {
            if (k != null && k.startsWith("tag.") && v != null) {
                tagEquals.put(k.substring("tag.".length()), v);
            }
        });

        return ApiResponse.success(
                vmQueryService.getVms(
                        page, size, providerType, zoneId, status, powerState, name,
                        ownerUserId, teamId, createdFrom, createdTo, tagEquals, sort
                )
        );
    }

    /**
     * VM 상세 조회
     */
    @GetMapping("/{vmId}")
    @Operation(summary = "VM 상세 조회", description = "VM의 상세 정보를 조회합니다")
    public ApiResponse<VmDetailResponse> getVmDetail(
            @Parameter(description = "VM ID", required = true)
            @PathVariable Long vmId
    ) {
        return ApiResponse.success(vmQueryService.getVmDetail(vmId));
    }

    /**
     * ⭐ VM 삭제 (비동기 처리)
     * - JWT 토큰에서 사용자 정보 추출
     * - lifecycle 상태를  'deleting'으로 변경
     * - RabbitMQ를 통해 Worker에게 terraform destroy 요청
     * - 실제 삭제는 Worker가 비동기로 처리
     *
     * @param vmId 삭제할 VM ID
     * @return 삭제 Job 정보 (jobId, status 등)
     */
    @DeleteMapping("/{vmId}")
    @ResponseStatus(HttpStatus.ACCEPTED)  // ⭐ 202 Accepted
    @Operation(
            summary = "VM 삭제",
            description = "VM을 비동기로 삭제합니다. JWT 토큰으로 인증된 사용자만 요청할 수 있으며, 삭제 요청이 큐에 적재되고 Worker가 terraform destroy를 실행합니다."
    )
    public ApiResponse<DeleteVmResponse> deleteVm(
            @Parameter(description = "삭제할 VM ID", required = true)
            @PathVariable("vmId") Long vmId  // ⭐ "vmId" 명시
    ) {
        // ⭐ JWT 토큰에서 현재 로그인한 사용자 ID 추출
        Long requestedBy = AuthUtil.getUserId();

        if (requestedBy == null) {
            throw new IllegalStateException("인증되지 않은 사용자입니다. JWT 토큰을 확인해주세요.");
        }

        // VmDeleteService를 통해 삭제 Job 큐에 적재
        DeleteVmResponse response = vmDeleteService.enqueueDeletion(vmId, requestedBy);

        return ApiResponse.success(response);
    }

    /**
     * VM 삭제 요청 (기존 메서드 - 필요시 유지)
     *
     * @deprecated deleteVm() 메서드를 사용하세요
     */
    @PostMapping("/{vmId}/delete-request")
    @Operation(
            summary = "VM 삭제 요청 (레거시)",
            description = "레거시 API입니다. DELETE /{vmId} 사용을 권장합니다."
    )
    @Deprecated
    public ApiResponse<Void> requestDelete(
            @Parameter(description = "VM ID", required = true)
            @PathVariable Long vmId
    ) {
        vmQueryService.requestDelete(vmId);
        return ApiResponse.success(null);
    }

    /**
     * ⭐ VM 삭제 상태 조회 (선택적 추가)
     * - lifecycle 필드를 통해 삭제 진행 상태 확인
     *
     * @param vmId VM ID
     * @return VM lifecycle 상태 (deleting, deleted, running 등)
     */
//    @GetMapping("/{vmId}/deletion-status")
//    @Operation(
//            summary = "VM 삭제 상태 조회",
//            description = "VM의 현재 삭제 상태를 조회합니다 (lifecycle 필드)"
//    )
//    public ApiResponse<Map<String, String>> getDeletionStatus(
//            @Parameter(description = "VM ID", required = true)
//            @PathVariable Long vmId
//    ) {
//        // VmDetailResponse에서 lifecycle 추출
//        VmDetailResponse detail = vmQueryService.getVmDetail(vmId);
//
//        Map<String, String> status = new HashMap<>();
//        status.put("vmId", String.valueOf(vmId));
//        status.put("vmName", detail.getName());
//        status.put("lifecycle", detail.getLifecycle());
//        status.put("powerState", detail.getPowerState());
//
//        return ApiResponse.success(status);
//    }
}