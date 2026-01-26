package com.cloudrangers.cloudpilot.monitor.prometheus.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * VM 단위 메트릭 요약 DTO
 * - 지금은 최소 필드만 정의 (나중에 Prometheus 연동되면 확장 가능)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VmMetricSummaryDto {

    /**
     * Prometheus에서 이 VM에 대한 메트릭을 찾았는지 여부
     */
    private boolean hasMetrics;

    /**
     * 평균 CPU 사용률 (%)
     */
    private Double cpuUsage;

    /**
     * 평균 메모리 사용률 (%)
     */
    private Double memoryUsage;
}
