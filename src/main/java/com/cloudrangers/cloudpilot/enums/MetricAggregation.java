package com.cloudrangers.cloudpilot.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MetricAggregation {

    private String vmId;
    private String metricName;

    private Double avg;
    private Double max;
    private Double min;
    private Double latest;
}
