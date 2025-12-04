package com.cloudrangers.cloudpilot.monitor.prometheus.controller;

import com.cloudrangers.cloudpilot.common.ApiResponse;
import com.cloudrangers.cloudpilot.monitor.prometheus.dto.VmMetricSummaryDto;
import com.cloudrangers.cloudpilot.monitor.prometheus.service.PrometheusMetricsService;
import com.cloudrangers.cloudpilot.monitor.prometheus.dto.HostResourceChartResponse;
import com.cloudrangers.cloudpilot.monitor.prometheus.service.HostResourceMetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/monitor/prometheus")
@RequiredArgsConstructor
public class PrometheusController {

    private final PrometheusMetricsService prometheusMetricsService;
    private final HostResourceMetricsService hostResourceMetricsService;

    /**
     * 지정한 VM 이름 목록에 대한 메트릭 요약 조회
     *
     * GET /monitor/prometheus/vms/metrics?names=vm1&names=vm2&teamId=1
     *
     * - names : vCenter VM 이름 리스트 (반드시 필요)
     * - teamId : (옵션) 팀별 필터링. null 이면 전체에서 조회
     */
    @GetMapping("/vms/metrics")
    public ApiResponse<Map<String, VmMetricSummaryDto>> getMetricsForVmNames(
            @RequestParam("names") List<String> vmNames,
            @RequestParam(value = "teamId", required = false) Long teamId
    ) {
        log.info("[PrometheusController] vmNames={}, teamId={}", vmNames, teamId);

        Map<String, VmMetricSummaryDto> metrics =
                prometheusMetricsService.getMetricsForVmNames(vmNames, teamId);

        return ApiResponse.success(metrics);
    }

    /**
     * vSphere 호스트 CPU/메모리 사용률 시계열 (Recharts용)
     *
     * 예:
     *  GET /monitor/prometheus/hosts/172.16.0.30/resources?rangeMinutes=60&stepSeconds=60
     */
    @GetMapping("/hosts/{hostName:.+}/resources")
    public ApiResponse<HostResourceChartResponse> getHostResourceSeries(
            @PathVariable("hostName") String hostName,
            @RequestParam(value = "rangeMinutes", required = false) Integer rangeMinutes,
            @RequestParam(value = "stepSeconds", required = false) Integer stepSeconds
    ) {
        log.info("[PrometheusController] host resource series. hostName={}, rangeMinutes={}, stepSeconds={}",
                hostName, rangeMinutes, stepSeconds);

        HostResourceChartResponse data =
                hostResourceMetricsService.getHostResourceSeries(hostName, rangeMinutes, stepSeconds);

        return ApiResponse.success(data);
    }
}
