package com.cloudrangers.cloudpilot.controller;

import com.cloudrangers.cloudpilot.common.ApiResponse;
import com.cloudrangers.cloudpilot.dto.request.InstallPackagesRequest;
import com.cloudrangers.cloudpilot.security.CustomUserDetails;
import com.cloudrangers.cloudpilot.service.pkg.PackageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/packages")
public class InstallPackagesController {

    private final PackageService packageService;

    @PostMapping("/install")
    public ApiResponse<InstallResponse> installPackages(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody InstallPackagesRequest request
    ) {
        List<String> jobIds = packageService.installPackages(user, request);
        return ApiResponse.success(new InstallResponse(jobIds));
    }

    public record InstallResponse(List<String> jobIds) {}
}
