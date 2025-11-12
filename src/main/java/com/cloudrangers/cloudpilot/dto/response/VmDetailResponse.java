// src/main/java/com/cloudrangers/cloudpilot/dto/response/VmDetailResponse.java
package com.cloudrangers.cloudpilot.dto.response;

import com.cloudrangers.cloudpilot.domain.vm.VmInstance;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor // final/원시타입이 없도록 구성 → 컴파일 에러 방지
@AllArgsConstructor
public class VmDetailResponse {
    private Long id;
    private String name;
    private String providerType;
    private Long zoneId;
    private String status;       // lifecycle 매핑
    private String powerState;
    private Integer cpu;         // vCPU
    private Integer ramGb;       // 메모리(GB)
    private Integer diskGb;      // 루트 디스크(GB)
    private Instant createdAt;
    private Map<String, String> tags;

    public static VmDetailResponse fromEntity(VmInstance vm) {
        if (vm == null) return null;
        return VmDetailResponse.builder()
                .id(vm.getId())
                .name(vm.getName())
                .providerType(vm.getProviderType())
                .zoneId(vm.getZoneId())
                .status(vm.getLifecycle())
                .powerState(vm.getPowerState())
                .cpu(vm.getVcpu())
                .ramGb(toGb(vm.getMemoryMb()))
                .diskGb(vm.getRootDiskGb())
                .createdAt(vm.getCreatedAt())
                .tags(parseTags(vm.getTags()))
                .build();
    }

    private static Integer toGb(Integer mb) {
        if (mb == null) return null;
        // 1024로 나눠 반올림
        return (int) Math.round(mb / 1024.0);
    }

    private static Map<String, String> parseTags(String raw) {
        if (raw == null || raw.isBlank()) return Collections.emptyMap();
        Map<String, String> map = new LinkedHashMap<>();
        // 예: "k1=v1,k2=v2"
        for (String pair : raw.split(",")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                map.put(kv[0].trim(), kv[1].trim());
            }
        }
        return map;
    }
}
