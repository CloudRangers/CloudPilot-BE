package com.cloudrangers.cloudpilot.dto.response;

import com.cloudrangers.cloudpilot.domain.catalog.InstanceTypeCatalog;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InstanceTypeResponse {
    private String id;
    private String name;
    private Integer vcpu;
    private Integer memoryGiB;
    private Long providerId;
    private Long zoneId;
    private Boolean burstable;

    public static InstanceTypeResponse fromEntity(InstanceTypeCatalog e) {
        return InstanceTypeResponse.builder()
                .id(e.getId())
                .name(e.getName())
                .vcpu(e.getVcpu())
                .memoryGiB(e.getMemoryGiB())
                .providerId(e.getProviderId())
                .zoneId(e.getZoneId())
                .burstable(e.getBurstable())
                .build();
    }
}
