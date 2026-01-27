// DatastoreUsageDto.java
package com.cloudrangers.cloudpilot.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DatastoreUsageDto {

    private final String dsName;

    // Prometheus에서 오는 raw 값 (대부분 byte 단위일 가능성 높음)
    private final double capacityBytes;
    private final double freeBytes;

    private final double usedBytes;
    private final double usedPercent; // 0~100
}
