package com.cloudrangers.cloudpilot.controller.catalog;

import com.cloudrangers.cloudpilot.common.ApiResponse;
import com.cloudrangers.cloudpilot.dto.common.PageResponse;
import com.cloudrangers.cloudpilot.dto.response.OsImageResponse;
import com.cloudrangers.cloudpilot.service.catalog.OsImageQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/catalog/os-images")
@RequiredArgsConstructor
public class OsImageController {

    private final OsImageQueryService osImageQueryService;

    @GetMapping
    public ApiResponse<PageResponse<OsImageResponse>> list(
            @RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="20") int size,
            @RequestParam(required=false) Long providerId,
            @RequestParam(required=false) Long zoneId,
            @RequestParam(required=false) String q,
            @RequestParam(required=false) String sort
    ) {
        return ApiResponse.ok(
                osImageQueryService.getOsImages(page, size, providerId, zoneId, q, sort)
        );
    }
}
