package com.cloudrangers.cloudpilot.controller;

import com.cloudrangers.cloudpilot.common.ApiResponse;
import com.cloudrangers.cloudpilot.dto.request.PkgApprovalActionRequest;
import com.cloudrangers.cloudpilot.dto.request.PkgRequestCreateRequest;
import com.cloudrangers.cloudpilot.dto.response.PkgRequestResponse;
import com.cloudrangers.cloudpilot.security.AuthUtil;
import com.cloudrangers.cloudpilot.service.pkg.PackageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import com.cloudrangers.cloudpilot.dto.response.PkgRequestDetailResponse;


import java.util.List;

@RestController
@RequestMapping("/packages")
@RequiredArgsConstructor
public class PkgRequestController {

    private final PackageService packageService;

    /** 1) 패키지 설치 요청 생성 */
    @PostMapping("/requests")
    public ApiResponse<PkgRequestResponse> createRequest(
            @RequestBody PkgRequestCreateRequest request
    ) {
        Long userId = AuthUtil.getUserId();
        if (userId == null) {
            throw new RuntimeException("인증 정보가 없습니다. 로그인 후 다시 시도해주세요.");
        }
        PkgRequestResponse response = packageService.createRequest(request, userId);
        return ApiResponse.success(response);
    }

    /** 7) [상세조회] 단일 요청 + 승인/반려 이력까지 모두 반환 */
    @GetMapping("/requests/{requestId}")
    public ApiResponse<PkgRequestDetailResponse> getRequestDetail(
            @PathVariable Long requestId
    ) {
        Long userId = AuthUtil.getUserId();
        if (userId == null) {
            throw new RuntimeException("인증 정보가 없습니다. 로그인 후 다시 시도해주세요.");
        }

        PkgRequestDetailResponse detail =
                packageService.getRequestDetail(requestId, userId);

        return ApiResponse.success(detail);
    }

    @GetMapping("/requests")
    public ApiResponse<List<PkgRequestResponse>> getRequests(
            @RequestParam(name = "view", defaultValue = "my") String view
    ) {
        Long userId = AuthUtil.getUserId();
        if (userId == null) {
            throw new RuntimeException("인증 정보가 없습니다. 로그인 후 다시 시도해주세요.");
        }

        List<PkgRequestResponse> list;

        switch (view) {
            case "my" ->       // 내가 올린 요청
                    list = packageService.getMyRequests(userId);
            case "history" ->  // 내가 결재한 이력
                    list = packageService.getMyApprovalHistory(userId);
            case "todo" ->     // 지금 내가 결재해야 하는 것들
                    list = packageService.getMyTodoApprovals(userId);
            default ->
                    throw new IllegalArgumentException("지원하지 않는 view 값입니다: " + view);
        }

        return ApiResponse.success(list);
    }



    /** 5) [팀장] L1 승인/반려 */
    @PostMapping("/requests/{requestId}/approve")
    public ApiResponse<PkgRequestResponse> approveOrRejectL1(
            @PathVariable Long requestId,
            @RequestBody PkgApprovalActionRequest body
    ) {
        Long approverId = AuthUtil.getUserId();
        if (approverId == null) {
            throw new RuntimeException("인증 정보가 없습니다. 로그인 후 다시 시도해주세요.");
        }
        // 팀장 결재 처리
        PkgRequestResponse res = packageService.handleL1Approval(requestId, approverId, body);
        return ApiResponse.success(res);
    }

    /** 6) [부장] 최종 승인/반려 */
    @PostMapping("/requests/{requestId}/approve")
    public ApiResponse<PkgRequestResponse> approveOrRejectFinal(
            @PathVariable Long requestId,
            @RequestBody PkgApprovalActionRequest body
    ) {
        Long approverId = AuthUtil.getUserId();
        if (approverId == null) {
            throw new RuntimeException("인증 정보가 없습니다. 로그인 후 다시 시도해주세요.");
        }
        // 부장 결재 처리
        PkgRequestResponse res = packageService.handleFinalApproval(requestId, approverId, body);
        return ApiResponse.success(res);
    }
}
