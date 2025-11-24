package com.cloudrangers.cloudpilot.dto.response;

import com.cloudrangers.cloudpilot.domain.pkg.PkgRequest;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class PkgRequestDetailResponse {

    // 기존 PkgRequestResponse 내용(요청 기본 정보)
    private PkgRequestResponse request;

    // 승인/반려 이력 목록
    private List<PkgApprovalHistoryResponse> approvals;

    public static PkgRequestDetailResponse of(
            PkgRequest request,
            List<PkgApprovalHistoryResponse> approvals
    ) {
        return PkgRequestDetailResponse.builder()
                .request(PkgRequestResponse.from(request))
                .approvals(approvals)
                .build();
    }
}
