package com.cloudrangers.cloudpilot.dto.response;

import com.cloudrangers.cloudpilot.enums.ProviderType;
import com.cloudrangers.cloudpilot.enums.ProvisionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProvisionResponse {

    private Long id;
    private String jobId;
    private Long catalogId;
    private Long userId;
    private Long teamId;
    private ProviderType providerType;
    private ProvisionStatus status;
    private String vmResourceId;
    private String errorMessage;
    private Integer retryCount;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 추가 정보
    private Long estimatedTimeSeconds;  // 예상 소요 시간
    private Integer progressPercentage;  // 진행률 (0-100)
}