package com.cloudrangers.cloudpilot.dto.message;

import com.cloudrangers.cloudpilot.dto.request.ProvisionRequest;
import lombok.*;
import com.cloudrangers.cloudpilot.enums.ProviderType;

import java.util.Map;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class ProvisionJobMessage {

    // 스키마 버전(향후 호환성)
    @Builder.Default
    private String schemaVersion = "v1";

    // 상관관계 및 기본정보
    private String jobId;
    private Long userId;
    private Long teamId;
    private Short zoneId;            // null 가능
    private ProviderType providerType;

    // 인증정보(임시: ENV → 차후 CredentialService 연동)
    private Map<String, String> credentials;

    // 실행 파라미터(원본 요청 스냅샷)
    private ProvisionRequest request;

    // 실행 동작 (기본 apply) – 추후 destroy 등 확장
    @Builder.Default
    private String action = "apply";
}
