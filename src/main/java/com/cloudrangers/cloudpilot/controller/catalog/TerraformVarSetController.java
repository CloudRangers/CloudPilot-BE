package com.cloudrangers.cloudpilot.controller.catalog;

import com.cloudrangers.cloudpilot.common.ApiResponse;
import com.cloudrangers.cloudpilot.dto.common.PageResponse;
import com.cloudrangers.cloudpilot.dto.response.TerraformVarSetResponse;
import com.cloudrangers.cloudpilot.service.catalog.TerraformVarSetQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/catalog/tf-varsets")
@RequiredArgsConstructor
public class TerraformVarSetController {

    private final TerraformVarSetQueryService service;

    @GetMapping
    public ApiResponse<PageResponse<TerraformVarSetResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String scope,     // GLOBAL/TEAM/PROJECT
            @RequestParam(required = false) String moduleId,  // 특정 모듈 전용 세트만 보고 싶을 때
            @RequestParam(required = false) String q,         // name contains
            @RequestParam(required = false) String sort
    ) {
        return ApiResponse.ok(
                service.getVarSets(page, size, scope, moduleId, q, sort)
        );
    }
}
