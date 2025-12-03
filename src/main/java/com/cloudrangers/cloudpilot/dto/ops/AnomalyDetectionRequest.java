// AnomalyDetectionRequest.java
package com.cloudrangers.cloudpilot.dto.ops;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnomalyDetectionRequest {

    // 어떤 VM 대상인지
    private String vmId;

    // 어떤 메트릭들을 분석했는지 (cpu_usage, memory_usage 등)
    private List<String> metricNames;

    /**
     * 예:
     * {
     *   "cpu_usage": [0.12, 0.18, 0.34, ...],
     *   "memory_usage": [0.65, 0.67, 0.7, ...]
     * }
     * 실제 AI 팀에서 원하는 형식으로 조정 가능
     */
    private Map<String, List<Double>> metrics;
}
