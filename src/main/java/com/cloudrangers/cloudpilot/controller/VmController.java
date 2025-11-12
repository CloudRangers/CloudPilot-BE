package com.cloudrangers.cloudpilot.controller;

import com.cloudrangers.cloudpilot.common.ApiResponse;
import com.cloudrangers.cloudpilot.dto.response.DeleteVmResponse;
import com.cloudrangers.cloudpilot.dto.response.VmDetailResponse;
import com.cloudrangers.cloudpilot.dto.response.VmStatusResponse;
import com.cloudrangers.cloudpilot.service.vm.VmDeleteService;
import com.cloudrangers.cloudpilot.service.vm.VmQueryService;
import com.cloudrangers.cloudpilot.service.vm.VmReadService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import com.cloudrangers.cloudpilot.dto.common.PageResponse;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/vms")
@RequiredArgsConstructor
public class VmController {

    private final VmQueryService vmQueryService;
    private final VmReadService vmReadService;
    private final VmDeleteService vmDeleteService;

    /**
     * VM 리스트 조회 (필터/정렬/태그)
     */
    @GetMapping
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

        return ApiResponse.ok(
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
    public ApiResponse<VmDetailResponse> getVmDetail(@PathVariable Long vmId) {
        return ApiResponse.ok(vmQueryService.getVmDetail(vmId));
    }

    /**
     * VM 삭제(비동기 큐잉) - 실제 삭제는 워커가 처리한다는 가정
     */
    @DeleteMapping("/{vmId}")
    public ApiResponse<DeleteVmResponse> deleteVm(
            @PathVariable Long vmId,
            @RequestHeader(value = "X-USER-ID", required = false) Long requestedBy // 임시: 인증 연동 전
    ) {
        return ApiResponse.ok(vmDeleteService.enqueueDeletion(vmId, requestedBy));
    }
    @DeleteMapping("/{vmId}")
    public ApiResponse<Void> requestDelete(@PathVariable Long vmId) {
        vmQueryService.requestDelete(vmId);
        return ApiResponse.ok(null);
    }

}
