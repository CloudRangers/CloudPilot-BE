package com.cloudrangers.cloudpilot.controller.monitoring;

import com.cloudrangers.cloudpilot.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/monitoring/prometheus")
@RequiredArgsConstructor
public class PrometheusMonitoringController {

    private final PrometheusMonitoringService prometheusMonitoringService;

    @GetMapping("/summary")
    public ApiResponse<PrometheusSummary> getSummary() {
        PrometheusSummary summary = prometheusMonitoringService.getPrometheusSummary();
        return ApiResponse.success(summary);
    }
}
