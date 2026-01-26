package com.cloudrangers.cloudpilot.ops.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RechartsDataResponse {

    private String vmId;
    private String metricName;
    private List<RechartsPoint> data;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RechartsPoint {
        private String timestamp; // 프론트에서 그대로 X축 레이블로 쓸 문자열
        private double value;
    }
}
