package com.cloudrangers.cloudpilot.controller.catalog;

import com.cloudrangers.cloudpilot.common.ApiResponse;
import com.cloudrangers.cloudpilot.dto.common.PageResponse;
import com.cloudrangers.cloudpilot.dto.response.SecurityProfileResponse;
import com.cloudrangers.cloudpilot.service.catalog.SecurityProfileQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/catalog/security-profiles")
@RequiredArgsConstructor
public class SecurityProfileController {

    private final SecurityProfileQueryService securityProfileQueryService;

    @GetMapping
    public ApiResponse<PageResponse<SecurityProfileResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long providerId,
            @RequestParam(required = false) Long zoneId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String sort
    ) {
        return ApiResponse.ok(
                securityProfileQueryService.getProfiles(page, size, providerId, zoneId, q, sort)
        );
    }
}
