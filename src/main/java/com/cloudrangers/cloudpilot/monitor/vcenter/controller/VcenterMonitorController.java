package com.cloudrangers.cloudpilot.monitor.vcenter.controller;

import com.cloudrangers.cloudpilot.common.ApiResponse;
import com.cloudrangers.cloudpilot.monitor.prometheus.dto.VmMetricSummaryDto;
import com.cloudrangers.cloudpilot.monitor.prometheus.service.PrometheusMetricsService;
import com.cloudrangers.cloudpilot.monitor.vcenter.dto.VcenterSummaryResponse;
import com.cloudrangers.cloudpilot.monitor.vcenter.service.VcenterMonitorService;
import com.cloudrangers.cloudpilot.dto.monitor.VCenterVmResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/monitor/vcenter")
@RequiredArgsConstructor
public class VcenterMonitorController {

    private final VcenterMonitorService vcenterMonitorService;
    private final PrometheusMetricsService prometheusMetricsService;

    @GetMapping("/monitor/vcenter/summary")
    public ApiResponse<VcenterSummaryResponse> getVcenterSummary() {
        return ApiResponse.success(vcenterMonitorService.getSummary());
    }

    /**
     * 기존 엔드포인트 (필요하면 유지)
     */
    @GetMapping("/vms")
    public ApiResponse<List<VCenterVmResponse>> getVmList() {
        return getLiveVmsInternal();
    }

    /**
     * ⭐ FE에서 호출 중인 URL: /monitor/vcenter/live-vms
     *   → 같은 로직으로 매핑만 하나 더 추가 (alias)
     */
    @GetMapping("/live-vms")
    public ApiResponse<List<VCenterVmResponse>> getLiveVms() {
        return getLiveVmsInternal();
    }

    // 공통 처리 로직
    private ApiResponse<List<VCenterVmResponse>> getLiveVmsInternal() {
        try {
            List<Map<String, Object>> liveVms = vcenterMonitorService.getLiveVmList();

            List<VCenterVmResponse> dtoList = liveVms.stream()
                    .map(VCenterVmResponse::fromVcenterMap)
                    .toList();

            return ApiResponse.success(dtoList);
        } catch (Exception e) {
            return ApiResponse.fail("vCenter VM 목록 조회 실패: " + e.getMessage());
        }
    }

    @GetMapping("/metrics")
    public ApiResponse<Map<String, VmMetricSummaryDto>> getVmMetrics(
            @RequestParam(required = false) Long teamId
    ) {
        List<Map<String, Object>> vms = vcenterMonitorService.getLiveVmList();
        List<String> names = vms.stream()
                .map(vm -> (String) vm.get("name"))
                .distinct()
                .toList();

        Map<String, VmMetricSummaryDto> metrics =
                prometheusMetricsService.getMetricsForVmNames(names, teamId);

        return ApiResponse.success(metrics);
    }
}
