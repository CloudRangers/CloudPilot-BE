package com.cloudrangers.cloudpilot.monitor.vcenter.service;

import com.cloudrangers.cloudpilot.infra.vcenter.VCenterClient;
import com.cloudrangers.cloudpilot.monitor.vcenter.dto.VcenterSummaryResponse;
import com.cloudrangers.cloudpilot.monitor.vcenter.dto.VcenterVmInfoDto;
import com.cloudrangers.cloudpilot.service.monitoring.PrometheusMonitoringService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class VcenterMonitorServiceImpl implements VcenterMonitorService {

    private final VCenterClient vCenterClient;
    private final PrometheusMonitoringService prometheusMonitoringService;

    /**
     * ✅ 상단 요약 카드용 summary
     *  - 이제 vCenter live VM 목록이 아니라
     *    "Prometheus에 등록된 vmware_vm_power_state" 기준으로 계산
     */
    @Override
    public VcenterSummaryResponse getSummary() {
        try {
            return prometheusMonitoringService.getVcenterVmSummaryFromPrometheus();
        } catch (Exception e) {
            log.error("[vCenter] summary 조회 실패 (Prometheus 기반)", e);
            return new VcenterSummaryResponse(0, 0, 0, 0, 0);
        }
    }

    /**
     * 기존 VM 리스트 (DB 저장용 DTO 변환)
     *  - vCenter live VM 목록은 그대로 사용
     */
    @Override
    public List<VcenterVmInfoDto> getVmList() {
        return vCenterClient.listVms().stream()
                .map(VcenterVmInfoDto::fromMap)   // ✅ 기존 팩토리 메서드 유지
                .toList();
    }

    /**
     * ⭐ live-vms API용: raw vCenter 응답 그대로 반환
     */
    @Override
    public List<Map<String, Object>> getLiveVmList() {
        return vCenterClient.listVms();
    }
}
