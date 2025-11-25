package com.cloudrangers.cloudpilot.monitor.prometheus.controller;

import com.cloudrangers.cloudpilot.common.ApiResponse;
import com.cloudrangers.cloudpilot.monitor.prometheus.dto.VmMetricSummaryDto;
import com.cloudrangers.cloudpilot.monitor.prometheus.service.PrometheusMetricsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/monitor/prometheus")
@RequiredArgsConstructor
public class PrometheusController {

    private final PrometheusMetricsService prometheusMetricsService;

    /**
     * 지정한 VM 이름 목록에 대한 메트릭 요약 조회
     * GET /monitor/prometheus/vms/metrics?names=vm1&names=vm2...
     */
    @GetMapping("/vms/metrics")
    public ApiResponse<Map<String, VmMetricSummaryDto>> getMetricsForVmNames(
            @RequestParam("names") List<String> vmNames
    ) {
        Map<String, VmMetricSummaryDto> metrics =
                prometheusMetricsService.getMetricsForVmNames(vmNames);

        return ApiResponse.success(metrics);
    }
}
