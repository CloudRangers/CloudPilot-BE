package com.cloudrangers.cloudpilot.service.vcenter;

import com.cloudrangers.cloudpilot.dto.vcenter.VCenterSummaryResponse;
import com.cloudrangers.cloudpilot.infra.vcenter.VCenterClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class VCenterClientService {

    private final VCenterClient vCenterClient;

    public VCenterSummaryResponse getSummary() {
        // 1) VM 목록 가져오기
        List<Map<String, Object>> vms = Collections.emptyList();
        try {
            vms = vCenterClient.listVms();
        } catch (Exception e) {
            log.error("[vCenter] VM 목록 조회 중 오류", e);
        }

        int totalVms = vms.size();
        long poweredOnVms = vms.stream()
                .filter(vm -> "POWERED_ON".equalsIgnoreCase(String.valueOf(vm.get("power_state"))))
                .count();
        long poweredOffVms = vms.stream()
                .filter(vm -> "POWERED_OFF".equalsIgnoreCase(String.valueOf(vm.get("power_state"))))
                .count();

        // 2) Host 목록 가져오기 (실패해도 치명적 X)
        List<Map<String, Object>> hosts;
        try {
            hosts = vCenterClient.listHosts();
        } catch (Exception e) {
            log.warn("[vCenter] Host 목록 조회 실패, Host 통계는 0으로 처리합니다.", e);
            hosts = Collections.emptyList();
        }

        int totalHosts = hosts.size();
        long connectedHosts = hosts.stream()
                .filter(h -> "CONNECTED".equalsIgnoreCase(String.valueOf(h.get("connection_state"))))
                .count();
        long disconnectedHosts = hosts.stream()
                .filter(h -> "DISCONNECTED".equalsIgnoreCase(String.valueOf(h.get("connection_state"))))
                .count();

        return new VCenterSummaryResponse(
                totalVms, poweredOnVms, poweredOffVms,
                totalHosts, connectedHosts, disconnectedHosts
        );
    }
}
