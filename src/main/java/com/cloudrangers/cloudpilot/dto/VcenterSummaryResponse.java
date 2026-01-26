package com.cloudrangers.cloudpilot.monitor.vcenter.dto;

import lombok.*;

/**
 * vCenter VM 상태 요약 응답 DTO
 *  - 총 VM 수
 *  - POWERED_ON
 *  - POWERED_OFF
 *  - SUSPENDED
 *  - 그 외 상태(UNKNOWN 등)
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VcenterSummaryResponse {

    // 전체 VM 수
    private int totalVms;

    // 상태별 개수
    private int poweredOn;
    private int poweredOff;
    private int suspended;
    private int unknown;
}
