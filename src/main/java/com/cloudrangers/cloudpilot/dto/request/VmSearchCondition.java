package com.cloudrangers.cloudpilot.dto.request;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
public class VmSearchCondition {
    private String providerType;
    private Long zoneId;
    private String status;
    private String powerState;
    private String nameContains;
    private Long ownerUserId;
    private Long teamId;
    private Instant createdFrom;
    private Instant createdTo;

    // 🔹 태그 동등매칭 조건 추가 (key=value 형태)
    private Map<String, String> tagEquals;
}
