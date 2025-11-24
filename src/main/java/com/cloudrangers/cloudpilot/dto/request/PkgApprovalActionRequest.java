package com.cloudrangers.cloudpilot.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * 패키지 승인/반려 시 사용하는 요청 바디
 * action: "approve" 또는 "reject"
 */
@Getter
@Setter
public class PkgApprovalActionRequest {
    @NotBlank
    private String step;
    @NotBlank
    private String action;  // approve / reject
    private String reason;  // 승인/반려 사유
}

