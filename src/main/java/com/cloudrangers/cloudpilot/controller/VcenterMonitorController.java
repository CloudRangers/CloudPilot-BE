package com.cloudrangers.cloudpilot.controller;

import com.cloudrangers.cloudpilot.common.ApiResponse;
import com.cloudrangers.cloudpilot.dto.monitor.VCenterVmResponse;
import com.cloudrangers.cloudpilot.domain.vm.VmInstance;
import com.cloudrangers.cloudpilot.dto.VmMetricSummaryDto;
import com.cloudrangers.cloudpilot.service.PrometheusMetricsService;
import com.cloudrangers.cloudpilot.dto.VcenterSummaryResponse;
import com.cloudrangers.cloudpilot.service.VcenterMonitorService;
import com.cloudrangers.cloudpilot.repository.vm.VmInstanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/monitor/vcenter")
@RequiredArgsConstructor
public class VcenterMonitorController {

    private final VcenterMonitorService vcenterMonitorService;
    private final PrometheusMetricsService prometheusMetricsService;
    private final VmInstanceRepository vmInstanceRepository;   // ⭐ DB 조인용

    // 🔹 URL: /monitor/vcenter/summary
    @GetMapping("/summary")
    public ApiResponse<VcenterSummaryResponse> getVcenterSummary() {
        return ApiResponse.success(vcenterMonitorService.getSummary());
    }

    /**
     * 기존 엔드포인트 (필요하면 유지)
     *  - /monitor/vcenter/vms
     */
    @GetMapping("/vms")
    public ApiResponse<List<VCenterVmResponse>> getVmList(
            @RequestParam(required = false) Long teamId   // ⭐ 옵션 팀 필터
    ) {
        return getLiveVmsInternal(teamId);
    }

    /**
     * ⭐ FE에서 호출 중인 URL: /monitor/vcenter/live-vms
     *   → 같은 로직으로 매핑만 하나 더 추가 (alias)
     */
    @GetMapping("/live-vms")
    public ApiResponse<List<VCenterVmResponse>> getLiveVms(
            @RequestParam(required = false) Long teamId   // 쿼리로 teamId 넘기면 필터
    ) {
        return getLiveVmsInternal(teamId);
    }

    // 🔹 vCenter 실시간 + DB VmInstance 조인해서 풍부한 정보 반환
    private ApiResponse<List<VCenterVmResponse>> getLiveVmsInternal(Long teamId) {
        try {
            // 1) vCenter에서 실시간 VM 목록 가져오기 (소스 오브 트루스)
            List<Map<String, Object>> liveVms = vcenterMonitorService.getLiveVmList();

            // 2) vCenter에서 가져온 이름 목록으로 DB를 한 번에 조회
            List<String> names = liveVms.stream()
                    .map(vm -> (String) vm.get("name"))
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();

            List<VmInstance> instances = (teamId != null)
                    ? vmInstanceRepository.findByNameInAndTeamId(names, teamId)
                    : vmInstanceRepository.findByNameIn(names);

            Map<String, VmInstance> dbByName = instances.stream()
                    .collect(Collectors.toMap(
                            VmInstance::getName,
                            Function.identity(),
                            (a, b) -> a   // 이름 중복 시 첫 번째 것 유지
                    ));

            // 3) vCenter 기준으로 돌면서 DB 정보 얹기
            List<VCenterVmResponse> dtoList = liveVms.stream()
                    .map(vm -> {
                        String name = (String) vm.get("name");
                        VmInstance dbVm = dbByName.get(name);

                        // 팀 필터 모드라면: dbVm가 없으면(=우리 시스템이 만든 VM 아님) 제외
                        if (teamId != null && dbVm == null) {
                            return null;
                        }

                        return VCenterVmResponse.fromVcenterAndDb(vm, dbVm);
                    })
                    .filter(Objects::nonNull)
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
