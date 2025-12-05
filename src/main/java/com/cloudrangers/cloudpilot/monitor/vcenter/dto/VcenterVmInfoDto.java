package com.cloudrangers.cloudpilot.monitor.vcenter.dto;

import lombok.Builder;
import lombok.Value;

import java.util.Map;

@Value
@Builder
public class VcenterVmInfoDto {
    String vmId;
    String name;
    String powerState;
    Integer cpuCount;
    Integer memorySizeMiB;

    /**
     * 🔹 vCenter raw 응답(Map) → DTO 변환
     */
    public static VcenterVmInfoDto fromMap(Map<String, Object> vm) {
        return VcenterVmInfoDto.builder()
                .vmId(String.valueOf(vm.get("vm")))
                .name((String) vm.get("name"))
                .powerState((String) vm.get("power_state"))
                .cpuCount((Integer) vm.getOrDefault("cpu_count", 0))
                .memorySizeMiB((Integer) vm.getOrDefault("memory_size_MiB", 0))
                .build();
    }
}
