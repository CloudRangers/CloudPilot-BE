package com.cloudrangers.cloudpilot.dto.response;

import com.cloudrangers.cloudpilot.domain.pkg.PkgApproval;
import com.cloudrangers.cloudpilot.enums.PkgApprovalResult;
import com.cloudrangers.cloudpilot.enums.PkgApprovalStep;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class PkgApprovalHistoryResponse {

    private Long id;                // approval row id
    private PkgApprovalStep step;   // L1 / FINAL
    private PkgApprovalResult result; // approved / rejected
    private String description;     // 승인/반려 사유
    private Instant decidedAt;      // 결정 시각

    private Long approverId;        // 승인자 user id
    private String approverName;    // 승인자 이름 (username 기준)

    public static PkgApprovalHistoryResponse from(PkgApproval entity) {
        return PkgApprovalHistoryResponse.builder()
                .id(entity.getId())
                .step(entity.getStep())
                .result(entity.getResult())
                .description(entity.getDescription())
                .decidedAt(entity.getDecidedAt())
                .approverId(entity.getApprover().getId())
                .approverName(entity.getApprover().getUsername())
                .build();
    }
}
