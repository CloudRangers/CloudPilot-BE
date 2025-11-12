package com.cloudrangers.cloudpilot.dto.request;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class ProviderCatalogSearchCondition {
    private String providerType;  // AWS, VSPHERE
    private String nameContains;  // 부분검색
    private Long providerId;      // 특정 상위 프로바이더(계정)로 한정
}
