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

    /** 2) [상태 확인] 내가 올린 모든 요청 (사원/팀장/부장 공통) */
    @GetMapping("/requests/me")
    public ApiResponse<List<PkgRequestResponse>> getMyRequests() {
        Long userId = AuthUtil.getUserId();
        if (userId == null) {
            throw new RuntimeException("인증 정보가 없습니다. 로그인 후 다시 시도해주세요.");
        }
        List<PkgRequestResponse> list = packageService.getMyRequests(userId);
        return ApiResponse.success(list);
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




    /** 3) [상태 확인] 내가 결재한 이력 (팀장: L1, 부장: FINAL) */
    @GetMapping("/requests/history/me")
    public ApiResponse<List<PkgRequestResponse>> getMyApprovalHistory() {
        Long userId = AuthUtil.getUserId();
        if (userId == null) {
            throw new RuntimeException("인증 정보가 없습니다. 로그인 후 다시 시도해주세요.");
        }
        List<PkgRequestResponse> list = packageService.getMyApprovalHistory(userId);
        return ApiResponse.success(list);
    }

    /** 4) [To-do] 지금 내가 결재해야 하는 요청 목록
     *   - 팀장 : 우리 팀 pending
     *   - 부장 : 전체 l1_approved
     */
    @GetMapping("/requests/todo")
    public ApiResponse<List<PkgRequestResponse>> getMyTodoApprovals() {
        Long userId = AuthUtil.getUserId();
        if (userId == null) {
            throw new RuntimeException("인증 정보가 없습니다. 로그인 후 다시 시도해주세요.");
        }
        List<PkgRequestResponse> list = packageService.getMyTodoApprovals(userId);
        return ApiResponse.success(list);
    }

    /** 5) [팀장] L1 승인/반려 */
    @PostMapping("/requests/{requestId}/l1")
    public ApiResponse<PkgRequestResponse> handleL1Approval(
            @PathVariable Long requestId,
            @RequestBody PkgApprovalActionRequest body
    ) {
        Long approverId = AuthUtil.getUserId();
        if (approverId == null) {
            throw new RuntimeException("인증 정보가 없습니다. 로그인 후 다시 시도해주세요.");
        }
        PkgRequestResponse res =
                packageService.handleL1Approval(requestId, approverId, body);
        return ApiResponse.success(res);
    }

    /** 6) [부장] 최종 승인/반려 */
    @PostMapping("/requests/{requestId}/final")
    public ApiResponse<PkgRequestResponse> handleFinalApproval(
            @PathVariable Long requestId,
            @RequestBody PkgApprovalActionRequest body
    ) {
        Long approverId = AuthUtil.getUserId();
        if (approverId == null) {
            throw new RuntimeException("인증 정보가 없습니다. 로그인 후 다시 시도해주세요.");
        }
        PkgRequestResponse res =
                packageService.handleFinalApproval(requestId, approverId, body);
        return ApiResponse.success(res);
    }
}
