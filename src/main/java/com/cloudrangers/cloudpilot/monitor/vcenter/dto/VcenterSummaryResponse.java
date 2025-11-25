package com.cloudrangers.cloudpilot.monitor.vcenter.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VcenterSummaryResponse {

    private int totalVms;       // 전체 VM 수
    private int poweredOn;      // POWERED_ON 개수
    private int poweredOff;     // POWERED_OFF 개수
    private int suspended;      // SUSPENDED 개수
    private int unknown;        // 그 외 상태 (에러, 알 수 없음 등)
}
