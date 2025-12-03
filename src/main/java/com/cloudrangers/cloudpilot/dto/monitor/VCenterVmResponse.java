// src/main/java/com/cloudrangers/cloudpilot/dto/monitor/VCenterVmResponse.java
package com.cloudrangers.cloudpilot.dto.monitor;

import com.cloudrangers.cloudpilot.domain.vm.VmInstance;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class VCenterVmResponse {

    Long id;
    String name;

    Integer cpuCores;    // vcpu
    Integer memoryGb;    // memoryMb / 1024
    Integer diskGb;      // rootDiskGb

    String osName;       // tags 에서 끌어다 쓸 수 있음
    String powerState;   // POWERED_ON / POWERED_OFF / ...
    String alarmStatus;  // FE에서 OK / WARNING / CRITICAL 로 사용

    Long teamId;
    String teamName;     // 필요 시 매핑
    String clusterName;  // 필요 시 매핑

    Instant createdAt;

    public static VCenterVmResponse from(VmInstance vm) {
        Integer memoryMb = vm.getMemoryMb();
        Integer memoryGb = memoryMb != null ? memoryMb / 1024 : null;

        // alarmStatus는 일단 lifecycle 기준으로 대략 매핑
        String lifecycle = vm.getLifecycle();
        String alarmStatus = "OK";
        if (lifecycle != null) {
            if ("FAILED".equalsIgnoreCase(lifecycle)) {
                alarmStatus = "CRITICAL";
            } else if ("CREATING".equalsIgnoreCase(lifecycle)) {
                alarmStatus = "WARNING";
            }
        }

        return VCenterVmResponse.builder()
                .id(vm.getId())
                .name(vm.getName())
                .cpuCores(vm.getVcpu())
                .memoryGb(memoryGb)
                .diskGb(vm.getRootDiskGb())
                .osName(null)              // TODO: tags JSON에서 osType 빼고 싶으면 여기서
                .powerState(vm.getPowerState())
                .alarmStatus(alarmStatus)
                .teamId(vm.getTeamId())
                .teamName(null)            // TODO: 팀 이름 매핑 필요하면 나중에
                .clusterName(null)         // TODO: cluster 필드 생기면 매핑
                .createdAt(vm.getCreatedAt())
                .build();
    }
}
