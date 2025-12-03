package com.cloudrangers.cloudpilot.controller.monitoring;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * /api/monitoring/prometheus/summary 응답에 쓰는 DTO
 *  - totalTargets : Prometheus up() 결과 전체 타겟 수
 *  - upTargets    : 값이 1인 타겟 수
 *  - downTargets  : 값이 0 또는 그 외인 타겟 수
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PrometheusSummary {
    private int totalTargets;
    private int upTargets;
    private int downTargets;
}
