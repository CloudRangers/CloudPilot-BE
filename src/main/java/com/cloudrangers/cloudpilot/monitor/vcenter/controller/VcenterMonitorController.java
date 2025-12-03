// src/main/java/com/cloudrangers/cloudpilot/monitor/vcenter/controller/VcenterMonitorController.java
package com.cloudrangers.cloudpilot.monitor.vcenter.controller;

import com.cloudrangers.cloudpilot.common.ApiResponse;
import com.cloudrangers.cloudpilot.domain.vm.VmInstance;
import com.cloudrangers.cloudpilot.monitor.prometheus.dto.VmMetricSummaryDto;
import com.cloudrangers.cloudpilot.monitor.prometheus.service.PrometheusMetricsService;
import com.cloudrangers.cloudpilot.monitor.vcenter.dto.VcenterSummaryResponse;
import com.cloudrangers.cloudpilot.monitor.vcenter.dto.VcenterVmInfoDto;
import com.cloudrangers.cloudpilot.monitor.vcenter.dto.DemoVmDto;
import com.cloudrangers.cloudpilot.monitor.vcenter.service.VcenterMonitorService;
import com.cloudrangers.cloudpilot.dto.monitor.VCenterVmResponse;
import com.cloudrangers.cloudpilot.repository.vm.VmInstanceRepository;

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
    private final VmInstanceRepository vmInstanceRepository;   // ✅ 추가

    @GetMapping("/summary")
    public ApiResponse<VcenterSummaryResponse> getSummary() {
        return ApiResponse.success(vcenterMonitorService.getSummary());
    }

    /**
     * ✅ vCenter VM 리스트 (AdminPage 테이블용)
     */
    @GetMapping("/vms")
    public ApiResponse<List<VCenterVmResponse>> getVmList() {

        // TODO: 필요 시 providerType == "VCENTER" 로 필터링
        List<VmInstance> vms = vmInstanceRepository.findAll();

        List<VCenterVmResponse> dtoList = vms.stream()
                .map(VCenterVmResponse::from)
                .toList();

        return ApiResponse.success(dtoList);
    }

    @GetMapping("/metrics")
    public ApiResponse<Map<String, VmMetricSummaryDto>> getVmMetrics(
            @RequestParam(required = false) Long teamId
    ) {
        List<VcenterVmInfoDto> vms = vcenterMonitorService.getVmList();
        List<String> names = vms.stream().map(VcenterVmInfoDto::getName).distinct().toList();

        Map<String, VmMetricSummaryDto> metrics =
                prometheusMetricsService.getMetricsForVmNames(names, teamId);

        return ApiResponse.success(metrics);
    }

    @GetMapping("/demo-vms")
    public ApiResponse<List<DemoVmDto>> getDemoVms(
            @RequestParam(required = false) Long teamId
    ) {
        List<String> demoVmNames = List.of(
                "Vmprovision-db-ip-test",
                "teamA-DB-dev",
                "harbor-VM",
                "teamA-ELK"
        );

        Map<String, VmMetricSummaryDto> metrics =
                prometheusMetricsService.getMetricsForVmNames(demoVmNames, teamId);

        List<DemoVmDto> result = demoVmNames.stream()
                .map(name -> {
                    VmMetricSummaryDto m = metrics.get(name);
                    if (m == null) {
                        return new DemoVmDto(name, null, null);
                    }
                    return new DemoVmDto(name, m.getCpuUsage(), m.getMemoryUsage());
                })
                .toList();

        return ApiResponse.success(result);
    }
}
