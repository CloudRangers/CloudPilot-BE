package com.cloudrangers.cloudpilot.dto.request;

import com.cloudrangers.cloudpilot.enums.ProviderType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProvisionRequest {

    @NotNull(message = "카탈로그 ID는 필수입니다")
    private Long catalogId;

    @NotNull(message = "Provider 타입은 필수입니다")
    private ProviderType providerType;

    @NotNull(message = "VM 수량은 필수입니다")
    @Min(value = 1, message = "최소 1개 이상의 VM을 생성해야 합니다")
    private Integer vmCount;

    @NotBlank(message = "VM 이름은 필수입니다")
    private String vmName;

    // 리소스 스펙
    private Integer cpuCores;
    private Integer memoryGb;
    private Integer diskGb;

    // 네트워크
    private String vpcId;
    private String subnetId;
    private String securityGroupId;

    // 태그
    private Map<String, String> tags;

    // 추가 설정 (JSON)
    private Map<String, Object> additionalConfig;
}