package com.cloudrangers.cloudpilot.dto.response;

import com.cloudrangers.cloudpilot.domain.catalog.Provider;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProviderSummaryResponse {
    private Long id;
    private String name;
    private String providerType;
    private String accountId;

    public static ProviderSummaryResponse fromEntity(Provider p) {
        return ProviderSummaryResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .providerType(p.getProviderType())
                .accountId(p.getAccountId())
                .build();
    }
}
