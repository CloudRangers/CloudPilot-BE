package com.cloudrangers.cloudpilot.controller.catalog;

import com.cloudrangers.cloudpilot.common.ApiResponse;
import com.cloudrangers.cloudpilot.dto.common.PageResponse;
import com.cloudrangers.cloudpilot.dto.response.PackageResponse;
import com.cloudrangers.cloudpilot.service.catalog.PackageCatalogQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/catalog/packages")
@RequiredArgsConstructor
public class PackageCatalogController {

    private final PackageCatalogQueryService service;

    @GetMapping
    public ApiResponse<PageResponse<PackageResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String osFamily,
            @RequestParam(required = false) String arch,
            @RequestParam(required = false) String repo
    ) {
        var result = service.getPackages(page, size, q, osFamily, arch, repo);
        return ApiResponse.ok(result);
    }
}
