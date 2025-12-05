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
                .teamName(null)   // 필요하면 Team 테이블 조인으로 대체
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

    /** ⭐ vCenter MAP + DB VmInstance를 함께 써서 풍부한 정보로 변환 */
    public static VCenterVmResponse fromVcenterAndDb(
            Map<String, Object> vm,
            VmInstance dbVm
    ) {
        // ── 1) vCenter 쪽 리소스 정보 ─────────────────────────────
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

        // 클러스터 이름 (vCenter 응답 키에 맞게 cluster_name / cluster 둘 다 시도)
        String clusterName = null;
        Object clusterObj = vm.get("cluster_name");
        if (clusterObj == null) {
            clusterObj = vm.get("cluster");
        }
        if (clusterObj != null) {
            clusterName = clusterObj.toString();
        }

        // ── 2) DB 쪽 팀 / 라이프사이클 정보 ────────────────────────
        Long teamId = null;
        String teamName = null;
        Instant createdAt = null;
        String alarmStatus = "OK";

        if (dbVm != null) {
            teamId = dbVm.getTeamId();
            createdAt = dbVm.getCreatedAt();

            String lifecycle = dbVm.getLifecycle();
            if (lifecycle != null) {
                if ("FAILED".equalsIgnoreCase(lifecycle)) alarmStatus = "CRITICAL";
                else if ("CREATING".equalsIgnoreCase(lifecycle)) alarmStatus = "WARNING";
            }

            // ⚠️ 임시 매핑: 나중에 Team 엔티티/Repository 있으면 여기만 교체
            if (teamId != null) {
                if (teamId == 1L) teamName = "develop"; // 지금 team 테이블에 있는 값
                else teamName = "TEAM-" + teamId;
            }
        }

        return VCenterVmResponse.builder()
                .id(dbVm != null ? dbVm.getId() : null)
                .name((String) vm.get("name"))
                .cpuCores((Integer) vm.getOrDefault("cpu_count", 0))
                .memoryGb(memoryMiB / 1024)
                .diskGb((int) (diskBytes / 1024 / 1024 / 1024))
                .osName(null)
                .powerState((String) vm.get("power_state"))
                .alarmStatus(alarmStatus)
                .teamId(teamId)
                .teamName(teamName)
                .clusterName(clusterName)
                .createdAt(createdAt)
                .build();
    }

    /** ✅ 기존에 쓰던 fromVcenterMapWithInstance가 있다면, 새 메서드에 위임 */
    public static VCenterVmResponse fromVcenterMapWithInstance(
            Map<String, Object> vm,
            VmInstance instance
    ) {
        return fromVcenterAndDb(vm, instance);
    }
}
