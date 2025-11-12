// src/main/java/com/cloudrangers/cloudpilot/controller/catalog/ZoneController.java
package com.cloudrangers.cloudpilot.controller;

import com.cloudrangers.cloudpilot.common.ApiResponse;
import com.cloudrangers.cloudpilot.dto.response.ZoneResponse;
import com.cloudrangers.cloudpilot.service.catalog.ZoneQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import com.cloudrangers.cloudpilot.dto.common.PageResponse;
@RestController
@RequestMapping("/catalog/zones")
@RequiredArgsConstructor
public class ZoneController {

    private final ZoneQueryService zoneQueryService;

    @GetMapping
    public ApiResponse<PageResponse<ZoneResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String providerType,
            @RequestParam(required = false) Long providerId,
            @RequestParam(required = false, defaultValue = "name,asc") String sort
    ) {
        return ApiResponse.ok(
                zoneQueryService.getZones(page, size, q, providerType, providerId, sort)
        );
    }
}
