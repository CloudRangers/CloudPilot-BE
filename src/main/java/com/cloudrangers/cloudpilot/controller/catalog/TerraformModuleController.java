package com.cloudrangers.cloudpilot.controller.catalog;

import com.cloudrangers.cloudpilot.common.ApiResponse;
import com.cloudrangers.cloudpilot.dto.common.PageResponse;
import com.cloudrangers.cloudpilot.dto.response.TerraformModuleResponse;
import com.cloudrangers.cloudpilot.service.catalog.TerraformModuleQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/catalog/tf-modules")
@RequiredArgsConstructor
public class TerraformModuleController {

    private final TerraformModuleQueryService service;

    @GetMapping
    public ApiResponse<PageResponse<TerraformModuleResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String providerType,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String version,
            @RequestParam(required = false) String sort
    ) {
        return ApiResponse.ok(
                service.getModules(page, size, providerType, q, version, sort)
        );
    }
}
