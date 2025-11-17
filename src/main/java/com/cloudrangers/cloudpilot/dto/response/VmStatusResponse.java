package com.cloudrangers.cloudpilot.dto.response;

import com.cloudrangers.cloudpilot.domain.vm.VmInstance;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VmStatusResponse {
    private Long id;
    private String name;
    private String providerType;
    private Long zoneId;
    private String status;
    private String powerState;
    private Instant createdAt;

    public static VmStatusResponse fromEntity(VmInstance vm) {
        if (vm == null) return null;
        return VmStatusResponse.builder()
                .id(vm.getId())
                .name(vm.getName())
                .providerType(vm.getProviderType())
                .zoneId(vm.getZoneId())
                .status(vm.getLifecycle())     // lifecycle → status 매핑
                .powerState(vm.getPowerState())
                .createdAt(vm.getCreatedAt())
                .build();
    }
}
