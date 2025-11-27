package com.cloudrangers.cloudpilot.controller;

import com.cloudrangers.cloudpilot.common.ApiResponse;
import com.cloudrangers.cloudpilot.dto.response.PackageDefResponse;
import com.cloudrangers.cloudpilot.service.catalog.PackageDefService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/packages")
@RequiredArgsConstructor
public class PackageDefController {

    private final PackageDefService packageDefService;

    @GetMapping
    public ApiResponse<List<PackageDefResponse>> getAllPackageDefs() {
        return ApiResponse.success(packageDefService.findAll());
    }
}
