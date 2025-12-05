// src/main/java/com/cloudrangers/cloudpilot/dto/monitor/VCenterVmResponse.java
package com.cloudrangers.cloudpilot.dto.monitor;

import com.cloudrangers.cloudpilot.domain.vm.VmInstance;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Value
@Builder
public class VCenterVmResponse {

    Long id;        // DB 기반일 때만 사용, vCenter 기반에서는 null
    String name;

    Integer cpuCores;
    Integer memoryGb;
    Integer diskGb;

    String osName;
    String powerState;
    String alarmStatus;

    Long teamId;
    String teamName;
    String clusterName;

    Instant createdAt;

    /** 기존 DB 기반 변환 */
    public static VCenterVmResponse from(VmInstance vm) {
        Integer memoryMb = vm.getMemoryMb();
        Integer memoryGb = memoryMb != null ? memoryMb / 1024 : null;

        String lifecycle = vm.getLifecycle();
        String alarmStatus = "OK";
        if (lifecycle != null) {
            if ("FAILED".equalsIgnoreCase(lifecycle)) alarmStatus = "CRITICAL";
            else if ("CREATING".equalsIgnoreCase(lifecycle)) alarmStatus = "WARNING";
        }

        return VCenterVmResponse.builder()
                .id(vm.getId())
                .name(vm.getName())
                .cpuCores(vm.getVcpu())
                .memoryGb(memoryGb)
                .diskGb(vm.getRootDiskGb())
                .osName(null)
                .powerState(vm.getPowerState())
                .alarmStatus(alarmStatus)
                .teamId(vm.getTeamId())
                .teamName(null)
                .clusterName(null)
                .createdAt(vm.getCreatedAt())
                .build();
    }

    /** 기존: vCenter 실시간 MAP → DTO (팀 정보 없음) */
    public static VCenterVmResponse fromVcenterMap(Map<String, Object> vm) {

        Integer memoryMiB = (Integer) vm.getOrDefault("memory_size_MiB", 0);

        long diskBytes = 0;
        Object disksObj = vm.get("disks");
        if (disksObj instanceof List<?> disks) {
            for (Object d : disks) {
                if (d instanceof Map<?, ?> diskMap) {
                    Object cap = diskMap.get("capacity");
                    if (cap instanceof Integer i) diskBytes += i;
                    if (cap instanceof Long l) diskBytes += l;
                }
            }
        }

        return VCenterVmResponse.builder()
                .id(null)
                .name((String) vm.get("name"))
                .cpuCores((Integer) vm.getOrDefault("cpu_count", 0))
                .memoryGb(memoryMiB / 1024)
                .diskGb((int) (diskBytes / 1024 / 1024 / 1024))
                .osName(null)
                .powerState((String) vm.get("power_state"))
                .alarmStatus("OK")
                .teamId(null)
                .teamName(null)
                .clusterName(null)
                .createdAt(null)
                .build();
    }

    /** ⭐ vCenter MAP + DB VmInstance 같이 써서 변환 (팀 정보 포함) */
    public static VCenterVmResponse fromVcenterMapWithInstance(
            Map<String, Object> vm,
            VmInstance instance
    ) {
        Integer memoryMiB = (Integer) vm.getOrDefault("memory_size_MiB", 0);

        long diskBytes = 0;
        Object disksObj = vm.get("disks");
        if (disksObj instanceof List<?> disks) {
            for (Object d : disks) {
                if (d instanceof Map<?, ?> diskMap) {
                    Object cap = diskMap.get("capacity");
                    if (cap instanceof Integer i) diskBytes += i;
                    if (cap instanceof Long l) diskBytes += l;
                }
            }
        }

        String alarmStatus = "OK";
        Long teamId = null;
        Instant createdAt = null;

        if (instance != null) {
            String lifecycle = instance.getLifecycle();
            if (lifecycle != null) {
                if ("FAILED".equalsIgnoreCase(lifecycle)) alarmStatus = "CRITICAL";
                else if ("CREATING".equalsIgnoreCase(lifecycle)) alarmStatus = "WARNING";
            }
            teamId = instance.getTeamId();
            createdAt = instance.getCreatedAt();
        }

        return VCenterVmResponse.builder()
                .id(instance != null ? instance.getId() : null)
                .name((String) vm.get("name"))
                .cpuCores((Integer) vm.getOrDefault("cpu_count", 0))
                .memoryGb(memoryMiB / 1024)
                .diskGb((int) (diskBytes / 1024 / 1024 / 1024))
                .osName(null)
                .powerState((String) vm.get("power_state"))
                .alarmStatus(alarmStatus)
                .teamId(teamId)
                .teamName(null)
                .clusterName(null)
                .createdAt(createdAt)
                .build();
    }
}
