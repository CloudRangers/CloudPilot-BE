package com.cloudrangers.cloudpilot.monitor.prometheus.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * vSphere 호스트(CPU/메모리) 시계열 응답 DTO (Recharts용)
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class HostResourceChartResponse {

    private String hostName;
    private List<ChartPoint> cpuSeries;
    private List<ChartPoint> memorySeries;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChartPoint {
        /**
         * X축 레이블로 그대로 사용할 시간 문자열 (예: "10:30")
         */
        private String timestamp;

        /**
         * 사용률 값 (%)
         */
        private double value;
    }
}
