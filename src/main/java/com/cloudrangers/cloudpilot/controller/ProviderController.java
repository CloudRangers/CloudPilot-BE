// src/main/java/com/cloudrangers/cloudpilot/controller/ProviderController.java
package com.cloudrangers.cloudpilot.controller;

import com.cloudrangers.cloudpilot.common.ApiResponse;
import com.cloudrangers.cloudpilot.dto.response.ProviderSummaryResponse;
import com.cloudrangers.cloudpilot.service.catalog.ProviderQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import com.cloudrangers.cloudpilot.dto.common.PageResponse;

@RestController
@RequestMapping("/catalog/providers")
@RequiredArgsConstructor
public class ProviderController {

    private final ProviderQueryService providerQueryService;

    @GetMapping
    public ApiResponse<PageResponse<ProviderSummaryResponse>> getProviders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String q,                   // 이름 부분검색 (옵션)
            @RequestParam(required = false) String providerType,        // AWS / VSPHERE 등 (옵션)
            @RequestParam(required = false, defaultValue = "name,asc") String sort // 정렬 "필드,asc|desc"
    ) {
        return ApiResponse.ok(
                providerQueryService.getProviders(page, size, q, providerType, sort)
        );
    }
}
