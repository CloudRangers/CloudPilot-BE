package com.cloudrangers.cloudpilot.controller.catalog;

import com.cloudrangers.cloudpilot.common.ApiResponse;
import com.cloudrangers.cloudpilot.dto.common.PageResponse;
import com.cloudrangers.cloudpilot.dto.response.InstanceTypeResponse;
import com.cloudrangers.cloudpilot.service.catalog.InstanceTypeQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/catalog/instance-types")
@RequiredArgsConstructor
public class InstanceTypeController {

    private final InstanceTypeQueryService service;

    @GetMapping
    public ApiResponse<PageResponse<InstanceTypeResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long providerId,
            @RequestParam(required = false) Long zoneId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Integer vcpuMin,
            @RequestParam(required = false) Integer vcpuMax,
            @RequestParam(required = false) Integer memMinGiB,
            @RequestParam(required = false) Integer memMaxGiB,
            @RequestParam(required = false) Boolean burstable,
            @RequestParam(required = false) String sort
    ) {
        return ApiResponse.ok(
                service.getInstanceTypes(page, size, providerId, zoneId, q, vcpuMin, vcpuMax, memMinGiB, memMaxGiB, burstable, sort)
        );
    }
}
