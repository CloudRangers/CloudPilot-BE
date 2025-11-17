package com.cloudrangers.cloudpilot.dto.response;

import com.cloudrangers.cloudpilot.domain.catalog.Provider;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProviderResponse {
    private Long id;
    private String name;
    private String providerType;

    public static ProviderResponse fromEntity(Provider p) {
        return ProviderResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .providerType(p.getProviderType())
                .build();
    }
}
