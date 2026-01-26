package com.cloudrangers.cloudpilot.ops.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MetricSeriesResponse {

    private String metricName;
    private String vmId;
    private java.util.List<TimeSeriesPoint> points;
}
