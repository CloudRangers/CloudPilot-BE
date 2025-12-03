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
@NoArgsConstructor
@AllArgsConstructor
public class VmDetailResponse {

    private Long id;
    private String name;

    // 🔄 providerType 제거됨 (DDL/엔티티에 없음)
    // private String providerType;

    private Integer vcpu;
    private Integer memoryMb;
    private Integer rootDiskGb;

    private String lifecycle;
    private String powerState;

    private Long teamId;
    private Long createdBy;

    private Instant createdAt;
    private Instant updatedAt;

    private String providerInstanceId; // AWS or vSphere instance identifier
    private Map<String, String> tags;  // JSON tags 파싱한 결과

    /**
     * VM → 상세 DTO 변환
     */
    public static VmDetailResponse fromEntity(VmInstance vm) {
        return VmDetailResponse.builder()
                .id(vm.getId())
                .name(vm.getName())
                .vcpu(vm.getVcpu())
                .memoryMb(vm.getMemoryMb())
                .rootDiskGb(vm.getRootDiskGb())
                .lifecycle(vm.getLifecycle())
                .powerState(vm.getPowerState())
                .teamId(vm.getTeamId())
                .createdBy(vm.getCreatedBy())
                .createdAt(vm.getCreatedAt())
                .updatedAt(vm.getUpdatedAt())
                .providerInstanceId(vm.getProviderInstanceId())
                .tags(parseTags(vm.getTags()))
                .build();
    }

    /**
     * "k=v,k2=v2" 형태 or JSON string일 경우 파싱
     */
    private static Map<String, String> parseTags(String raw) {
        if (raw == null || raw.isBlank()) return Collections.emptyMap();

        Map<String, String> map = new LinkedHashMap<>();

        // 기본 태그 파싱 (k=v,k2=v2)
        if (raw.contains("=") && raw.contains(",")) {
            for (String pair : raw.split(",")) {
                String[] kv = pair.split("=", 2);
                if (kv.length == 2) {
                    map.put(kv[0].trim(), kv[1].trim());
                }
            }
            return map;
        }

        // TODO: 만약 실제 JSON 형태라면 JSON 파싱 처리 필요 (Gson/Jackson)
        return Collections.singletonMap("raw", raw);
    }
}
