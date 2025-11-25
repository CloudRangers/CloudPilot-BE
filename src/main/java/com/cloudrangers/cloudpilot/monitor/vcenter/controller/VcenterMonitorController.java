package com.cloudrangers.cloudpilot.monitor.vcenter.controller;

import com.cloudrangers.cloudpilot.common.ApiResponse;
import com.cloudrangers.cloudpilot.monitor.prometheus.dto.VmMetricSummaryDto;
import com.cloudrangers.cloudpilot.monitor.prometheus.service.PrometheusMetricsService;
import com.cloudrangers.cloudpilot.monitor.vcenter.dto.VcenterSummaryResponse;
import com.cloudrangers.cloudpilot.monitor.vcenter.dto.VcenterVmInfoDto;
import com.cloudrangers.cloudpilot.monitor.vcenter.service.VcenterMonitorService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/monitor/vcenter")
@RequiredArgsConstructor
public class VcenterMonitorController {

    private final VcenterMonitorService vcenterMonitorService;

    // ✅ Prometheus 메트릭 서비스 주입
    private final PrometheusMetricsService prometheusMetricsService;

    /**
     * vCenter VM 요약 정보
     *
     * GET /monitor/vcenter/summary
     */
    @GetMapping("/summary")
    public ApiResponse<VcenterSummaryResponse> getSummary() {
        VcenterSummaryResponse summary = vcenterMonitorService.getSummary();
        return ApiResponse.success(summary);
    }

    /**
     * vCenter VM 전체 목록
     *
     * GET /monitor/vcenter/vms
     */
    @GetMapping("/vms")
    public ApiResponse<List<VcenterVmInfoDto>> getVmList() {
        List<VcenterVmInfoDto> vms = vcenterMonitorService.getVmList();
        return ApiResponse.success(vms);
    }

    /**
     * vCenter VM 목록 기준으로 Prometheus 메트릭 요약 조회
     *
     * GET /monitor/vcenter/metrics
     *
     * 반환 형태:
     *  {
     *    "data": {
     *      "vm-name-1": { "hasMetrics": true, "cpuUsage": 0.53, "memoryUsage": 0.42 },
     *      "vm-name-2": { "hasMetrics": false, "cpuUsage": null, "memoryUsage": null },
     *      ...
     *    }
     *  }
     */
    @GetMapping("/metrics")
    public ApiResponse<Map<String, VmMetricSummaryDto>> getVmMetrics() {

        // 1) vCenter VM 목록에서 이름만 추출
        List<VcenterVmInfoDto> vms = vcenterMonitorService.getVmList();
        List<String> names = vms.stream()
                .map(VcenterVmInfoDto::getName)
                .distinct()
                .toList();

        // 2) Prometheus에서 VM 단위 메트릭 조회
        Map<String, VmMetricSummaryDto> metrics =
                prometheusMetricsService.getMetricsForVmNames(names);

        return ApiResponse.success(metrics);
    }
}
