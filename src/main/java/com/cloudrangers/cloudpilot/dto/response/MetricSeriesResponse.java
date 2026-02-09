package com.cloudrangers.cloudpilot.dto.response;

import com.cloudrangers.cloudpilot.enums.TimeSeriesPoint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MetricSeriesResponse {

    private String metricName;
    private String vmId;
    private java.util.List<TimeSeriesPoint> points;
}
