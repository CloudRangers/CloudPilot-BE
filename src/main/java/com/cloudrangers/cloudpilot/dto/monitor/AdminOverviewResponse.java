// src/main/java/com/cloudrangers/cloudpilot/dto/monitor/AdminOverviewResponse.java
package com.cloudrangers.cloudpilot.dto.monitor;

import lombok.Builder;
import lombok.Getter;

/**
 * 관리자 대시보드 "운영 모니터링" 카드에 들어갈 요약 정보 DTO
 */
@Getter
@Builder
public class AdminOverviewResponse {
    private long dailyUserCount;       // 일일 사용자 수
    private double dailyUserChange;    // 전일 대비 증감률 (예: 12.5)
    private String systemLoadLevel;    // "LOW", "MEDIUM", "HIGH"
    private long avgResponseMs;        // 평균 응답시간 (ms)
    private long errorCount24h;        // 지난 24시간 에러 수
}
