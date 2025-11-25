package com.cloudrangers.cloudpilot.ops.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class VmMetricResponse {

    private String vmId;
    private List<MetricSeriesResponse> series;
}
