package com.cloudrangers.cloudpilot.controller;

import com.cloudrangers.cloudpilot.common.ApiResponse;
import com.cloudrangers.cloudpilot.dto.response.VmStatusResponse;
import com.cloudrangers.cloudpilot.service.vm.VmQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import com.cloudrangers.cloudpilot.dto.common.PageResponse;

@RestController
@RequestMapping("/users/{userId}/vms")
@RequiredArgsConstructor
public class UserVmController {

    private final VmQueryService vmQueryService;

    @GetMapping
    public ApiResponse<PageResponse<VmStatusResponse>> getUserVms(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        // ownerUserId = userId 강제
        return ApiResponse.ok(
                vmQueryService.getVms(
                        page, size,
                        null, // providerType
                        null, // zoneId
                        null, // status
                        null, // powerState
                        null, // name
                        userId, // <-- 핵심
                        null, // teamId
                        null, null, null, null
                )
        );
    }
}
