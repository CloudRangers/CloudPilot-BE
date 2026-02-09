package com.cloudrangers.cloudpilot.service;

import com.cloudrangers.cloudpilot.enums.MetricAggregation;
import com.cloudrangers.cloudpilot.enums.RechartsDataResponse;
import com.cloudrangers.cloudpilot.enums.VmMetricResponse;

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
