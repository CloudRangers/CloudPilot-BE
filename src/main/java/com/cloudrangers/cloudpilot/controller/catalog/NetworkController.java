package com.cloudrangers.cloudpilot.controller.catalog;

import com.cloudrangers.cloudpilot.common.ApiResponse;
import com.cloudrangers.cloudpilot.dto.common.PageResponse;
import com.cloudrangers.cloudpilot.dto.response.NetworkResponse;
import com.cloudrangers.cloudpilot.service.catalog.NetworkQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/catalog/networks")
@RequiredArgsConstructor
public class NetworkController {

    private final NetworkQueryService networkQueryService;

    @GetMapping
    public ApiResponse<PageResponse<NetworkResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long providerId,
            @RequestParam(required = false) Long zoneId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String sort
    ) {
        return ApiResponse.ok(
                networkQueryService.getNetworks(page, size, providerId, zoneId, q, sort)
        );
    }
}
