package com.cloudrangers.cloudpilot.dto.monitoring;

import lombok.Builder;
import lombok.Getter;

/**
 * 관리자 대시보드 운영 모니터링 요약 DTO
 *
 * FE 타입:
 *  - dailyUserCount: number;
 *  - dailyUserChange: number;
 *  - systemLoadLevel: string;
 *  - avgResponseMs: number;
 *  - avgResponseValid: boolean;
 *  - errorCount24h: number;
 *  - errorCountValid: boolean;
 */
@Getter
@Builder
public class AdminOverviewDto {

    /** 오늘 관리자/팀장/사용자들의 총 로그인 횟수 */
    private final int dailyUserCount;

    /** 어제 대비 로그인 횟수 증감률 (%) */
    private final double dailyUserChange;

    /** 시스템 부하 수준 (LOW / MEDIUM / HIGH) */
    private final String systemLoadLevel;

    /** 평균 응답 시간(ms) - Prometheus HTTP Metrics 연동 */
    private final long avgResponseMs;

    /** avgResponseMs 값이 유효하게 조회되었는지 여부 */
    private final boolean avgResponseValid;

    /** 최근 24시간 에러 수 (HTTP 5xx 등) */
    private final long errorCount24h;

    /** errorCount24h 값이 유효하게 조회되었는지 여부 */
    private final boolean errorCountValid;
}
