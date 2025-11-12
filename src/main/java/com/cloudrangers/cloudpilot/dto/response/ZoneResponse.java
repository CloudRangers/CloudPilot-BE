package com.cloudrangers.cloudpilot.dto.response;

import com.cloudrangers.cloudpilot.domain.catalog.Zone;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ZoneResponse {
    private Long id;
    private String name;
    private Long providerId;
    private String providerType;

    public static ZoneResponse fromEntity(Zone z) {
        return ZoneResponse.builder()
                .id(z.getId())
                .name(z.getName())
                .providerId(z.getProviderId())
                .providerType(z.getProviderType())
                .build();
    }
}
