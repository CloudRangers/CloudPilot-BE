package com.cloudrangers.cloudpilot.monitor.prometheus.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * vCenter VM CPU/메모리 사용률 시계열 응답 DTO (Recharts용)
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class VmResourceChartResponse {

    private String vmName;
    private List<ChartPoint> cpuSeries;
    private List<ChartPoint> memorySeries;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChartPoint {
        /** X축 레이블로 쓸 시간 문자열 (예: "14:30") */
        private String timestamp;
        /** 사용률 값 (%) */
        private double value;
    }
}
