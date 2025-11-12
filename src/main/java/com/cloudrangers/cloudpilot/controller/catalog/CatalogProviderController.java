// src/main/java/com/cloudrangers/cloudpilot/controller/catalog/CatalogProviderController.java
package com.cloudrangers.cloudpilot.controller.catalog;

import com.cloudrangers.cloudpilot.common.ApiResponse;
import com.cloudrangers.cloudpilot.dto.common.PageResponse;
import com.cloudrangers.cloudpilot.dto.response.ProviderSummaryResponse;
import com.cloudrangers.cloudpilot.service.catalog.ProviderQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/catalog/providers")
@RequiredArgsConstructor
public class CatalogProviderController {

    private final ProviderQueryService providerQueryService;

    @GetMapping
    public ApiResponse<PageResponse<ProviderSummaryResponse>> getProviders(
            @RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="20") int size,
            @RequestParam(required=false) String q,                // 이름 검색
            @RequestParam(required=false) String providerType,     // "AWS", "VSPHERE"...
            @RequestParam(required=false, defaultValue="name,asc") String sort
    ) {
        return ApiResponse.ok(
                providerQueryService.getProviders(page, size, q, providerType, sort)
        );
    }
}
