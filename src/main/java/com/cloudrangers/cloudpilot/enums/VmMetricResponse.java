package com.cloudrangers.cloudpilot.ops.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * VM 단일 메트릭의 시계열 응답 DTO
 * - 프론트에서 라인 차트 그릴 때 직접 사용 가능
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VmMetricResponse {

    private String vmId;
    private String metricName;

    /**
     * 🔗 TimeSeriesPoint(timestamp: Instant, value: double)
     */
    private List<TimeSeriesPoint> data;
}
