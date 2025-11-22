package com.cloudrangers.cloudpilot.controller;

import com.cloudrangers.cloudpilot.common.ApiResponse;
import com.cloudrangers.cloudpilot.dto.request.PkgRequestCreateRequest;
import com.cloudrangers.cloudpilot.dto.response.PkgRequestResponse;
import com.cloudrangers.cloudpilot.security.AuthUtil;
import com.cloudrangers.cloudpilot.service.pkg.PackageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/packages")
@RequiredArgsConstructor
public class PkgRequestController {

    private final PackageService packageService;

    @PostMapping("/requests")
    public ApiResponse<PkgRequestResponse> createRequest(
            @RequestBody PkgRequestCreateRequest request
    ) {
        // JWT에서 userId 꺼냄
        Long userId = AuthUtil.getUserId();

        if (userId == null) {
            throw new RuntimeException("인증 정보가 없습니다. 로그인 후 다시 시도해주세요.");
        }

        // 서비스에 dto + userId 넘김
        PkgRequestResponse response = packageService.createRequest(request, userId);

        return ApiResponse.success(response);
    }
}
