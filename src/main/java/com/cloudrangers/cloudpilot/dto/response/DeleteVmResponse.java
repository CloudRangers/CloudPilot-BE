package com.cloudrangers.cloudpilot.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeleteVmResponse {

    private String jobId;           // 삭제 Job ID (UUID)
    private Long vmId;              // VM Instance ID
    private String vmName;          // VM 이름
    private String status;          // QUEUED / DELETING / DELETED / FAILED
    private Long requestedBy;       // 요청자 User ID
    private Instant requestedAt;    // 요청 시간
    private String message;         // 상태 메시지

    // 추가 정보 (선택)
    private String providerType;    // VSPHERE / AWS
    private Long zoneId;            // Zone ID
}