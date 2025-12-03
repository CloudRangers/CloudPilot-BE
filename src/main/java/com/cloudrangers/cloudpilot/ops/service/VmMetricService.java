package com.cloudrangers.cloudpilot.ops.service;

import com.cloudrangers.cloudpilot.ops.dto.MetricAggregation;
import com.cloudrangers.cloudpilot.ops.dto.RechartsDataResponse;
import com.cloudrangers.cloudpilot.ops.dto.VmMetricResponse;

public interface VmMetricService {

    VmMetricResponse getVmMetrics(String vmId,
                                  String metricName,
                                  int rangeMinutes,
                                  int stepSeconds);

    MetricAggregation getMetricAggregation(String vmId,
                                           String metricName,
                                           int rangeMinutes);

    RechartsDataResponse getMetricsForRecharts(String vmId,
                                               String metricName,
                                               int rangeMinutes,
                                               int stepSeconds);
}
