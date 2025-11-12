package com.cloudrangers.cloudpilot.dto.response;

import com.cloudrangers.cloudpilot.domain.catalog.ProviderLocation;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class ProviderCatalogItem {

    private Long id;
    private String name;
    private Long providerId;
    private String providerType;

    public static ProviderCatalogItem fromEntity(ProviderLocation e) {
        return ProviderCatalogItem.builder()
                .id(e.getId())
                .name(e.getName())
                .providerId(e.getProviderId())
                .providerType(e.getProviderType())
                .build();
    }
}
