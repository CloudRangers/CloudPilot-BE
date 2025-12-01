package com.cloudrangers.cloudpilot.dto.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProvisionProgressMessage {
    private String jobId;
    private String stage;          // TERRAFORM_INIT, TERRAFORM_APPLY, ANSIBLE_START, COMPLETE
    private String description;    // 사용자 표시 메시지
    private Integer progress;      // 0-100
    private Integer elapsedSeconds; // 경과 시간 (초)
    private String vmIpAddress;    // VM IP (생성 완료 후)
    private String status;         // RUNNING, SUCCEEDED, FAILED
    private String logLine;        // 최신 로그 라인
}