package com.cloudrangers.cloudpilot.dto.request;

import com.cloudrangers.cloudpilot.enums.ProviderType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.Map;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class ProvisionRequest {

    @NotNull(message = "카탈로그 ID는 필수입니다")
    private Long catalogId;

    @NotNull(message = "Provider 타입은 필수입니다")
    private ProviderType providerType;

    // DDL에 zone 개념이 있으므로 선택값으로 받음(없으면 팀 기본존 사용)
    private Short zoneId;

    @NotNull(message = "VM 수량은 필수입니다")
    @Min(value = 1, message = "최소 1개 이상의 VM을 생성해야 합니다")
    private Integer vmCount;

    @NotBlank(message = "VM 이름은 필수입니다")
    private String vmName;

    private String purpose;

    // 스펙
    private Integer cpuCores;
    private Integer memoryGb;
    private Integer diskGb;

    // 네트워크(온프레미스라면 포트그룹/네트워크명 등으로 맵핑)
    private String vpcId;
    private String subnetId;
    private String securityGroupId;

    // 태그 & 추가 설정
    private Map<String, String> tags;
    private Map<String, Object> additionalConfig;
}
