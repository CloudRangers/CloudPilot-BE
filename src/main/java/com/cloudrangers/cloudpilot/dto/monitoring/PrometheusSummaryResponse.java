package com.cloudrangers.cloudpilot.dto.monitoring;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * FE Prometheus 모니터링에서 사용하는 up() 요약 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PrometheusSummaryResponse {

    private int totalTargets;
    private int upTargets;
    private int downTargets;
}
