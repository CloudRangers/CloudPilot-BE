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

    /**
     * power_state 문자열 정규화
     * - null   → ""
     * - 양쪽 공백 제거
     * - 대문자 변환
     * - "_", "-", " " 제거해서 형식 차이 흡수
     *
     * 예)
     *   "POWERED_ON"   → "POWEREDON"
     *   "poweredOn"    → "POWEREDON"
     *   " powered-off" → "POWEREDOFF"
     */
    private String normPowerState(Object state) {
        if (state == null) return "";
        String s = String.valueOf(state).trim().toUpperCase();
        return s.replace("_", "")
                .replace("-", "")
                .replace(" ", "");
    }

    /**
     * connection_state 정규화 (host용)
     * - 대소문자/공백 정도만 정리
     */
    private String normConnectionState(Object state) {
        if (state == null) return "";
        return String.valueOf(state).trim().toUpperCase();
    }

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
                .map(vm -> normPowerState(vm.get("power_state")))
                .filter(s -> s.equals("POWEREDON"))
                .count();

        long poweredOffVms = vms.stream()
                .map(vm -> normPowerState(vm.get("power_state")))
                .filter(s -> s.equals("POWEREDOFF"))
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
                .map(h -> normConnectionState(h.get("connection_state")))
                .filter(s -> s.equals("CONNECTED"))
                .count();

        long disconnectedHosts = hosts.stream()
                .map(h -> normConnectionState(h.get("connection_state")))
                .filter(s -> s.equals("DISCONNECTED"))
                .count();

        return new VCenterSummaryResponse(
                totalVms,
                poweredOnVms,
                poweredOffVms,
                totalHosts,
                connectedHosts,
                disconnectedHosts
        );
    }
}
