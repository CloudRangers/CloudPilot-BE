// src/main/java/com/cloudrangers/cloudpilot/service/monitor/AdminOverviewService.java
package com.cloudrangers.cloudpilot.service.monitor;

import com.cloudrangers.cloudpilot.dto.monitor.AdminOverviewResponse;
import com.cloudrangers.cloudpilot.dto.monitoring.PrometheusSummaryResponse;
import com.cloudrangers.cloudpilot.service.monitoring.PrometheusMonitoringService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 관리자 대시보드 상단 "운영 모니터링" 카드용 서비스
 *
 * - PrometheusMonitoringService.getUpSummary() 호출해서
 *   up/down 타깃 기준으로 systemLoadLevel 계산
 * - Prometheus 장애 시에는 LOW 로 떨어지는 안전한 fallback 포함
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminOverviewService {

    private final PrometheusMonitoringService prometheusMonitoringService;

    public AdminOverviewResponse getOverview() {

        // 1) Prometheus 요약 정보 조회 (예외 시 fallback)
        PrometheusSummaryResponse summary;
        try {
            summary = prometheusMonitoringService.getUpSummary();
        } catch (Exception e) {
            log.warn("[AdminOverview] Prometheus 요약 조회 실패, 기본값으로 대체합니다.", e);
            summary = new PrometheusSummaryResponse(0, 0, 0);
        }

        int total = summary.getTotalTargets();
        int down = summary.getDownTargets();

        // 2) up/down 비율 기반 systemLoadLevel 계산
        String systemLoadLevel = calculateSystemLoadLevel(total, down);

        // 3) 나머지 값은 일단 하드코딩 (TODO로 남겨두기)
        long dailyUserCount = 1247L;    // TODO: 실제 로그인/사용 로그 기준으로 계산
        double dailyUserChange = 12.5;  // TODO: 어제 vs 오늘 비교
        long avgResponseMs = 245L;      // TODO: Prometheus latency 메트릭에서 평균값
        long errorCount24h = 3L;        // TODO: 24시간 5xx 카운트

        return AdminOverviewResponse.builder()
                .dailyUserCount(dailyUserCount)
                .dailyUserChange(dailyUserChange)
                .systemLoadLevel(systemLoadLevel)
                .avgResponseMs(avgResponseMs)
                .errorCount24h(errorCount24h)
                .build();
    }

    /**
     * down 비율에 따라 시스템 부하 레벨 결정
     *  - total <= 0            → LOW (예외상황)
     *  - down == 0             → LOW
     *  - down <= 20% of total  → MEDIUM
     *  - 그 외                 → HIGH
     */
    private String calculateSystemLoadLevel(int totalTargets, int downTargets) {
        if (totalTargets <= 0) {
            return "LOW";
        }

        if (downTargets == 0) {
            return "LOW";
        } else if (downTargets <= totalTargets * 0.2) {
            return "MEDIUM";
        } else {
            return "HIGH";
        }
    }
}
