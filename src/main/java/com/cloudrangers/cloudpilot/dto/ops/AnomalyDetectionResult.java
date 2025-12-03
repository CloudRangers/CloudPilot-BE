// AnomalyDetectionResult.java
package com.cloudrangers.cloudpilot.dto.ops;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnomalyDetectionResult {

    private String vmId;

    // 이상징후 여부
    private boolean anomaly;

    // 0.0 ~ 1.0 정도로, 1에 가까울수록 심각
    private double score;

    // ex) "CPU Spike Detected", "Memory Leak Suspected"
    private String summary;

    // 선택: 사람에게 보여줄 상세 설명
    private String explanation;

    // 경고 레벨 (info / warning / critical 등)
    private String severity;
}
