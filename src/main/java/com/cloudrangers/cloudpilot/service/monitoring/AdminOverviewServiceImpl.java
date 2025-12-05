// src/main/java/com/cloudrangers/cloudpilot/service/monitoring/AdminOverviewServiceImpl.java
package com.cloudrangers.cloudpilot.service.monitoring;

import com.cloudrangers.cloudpilot.dto.monitoring.AdminOverviewDto;
import com.cloudrangers.cloudpilot.dto.monitoring.PrometheusSummaryResponse;
import com.cloudrangers.cloudpilot.domain.vm.VmInstance;
import com.cloudrangers.cloudpilot.monitor.prometheus.dto.VmMetricSummaryDto;
import com.cloudrangers.cloudpilot.monitor.prometheus.service.PrometheusMetricsService;
import com.cloudrangers.cloudpilot.repository.user.LoginHistoryRepository;
import com.cloudrangers.cloudpilot.repository.vm.VmInstanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminOverviewServiceImpl implements AdminOverviewService {

    private final PrometheusMonitoringService prometheusMonitoringService;
    private final PrometheusMetricsService prometheusMetricsService;
    private final VmInstanceRepository vmInstanceRepository;
    private final LoginHistoryRepository loginHistoryRepository;

    @Override
    public AdminOverviewDto getOverview() {

        log.info("[AdminOverview] ==== getOverview() called ====");

        // =========================
        // 1) 오늘/어제 로그인 횟수 계산 (ADMIN 제외)
        //    - 테스트 시 ADMIN도 포함하고 싶으면 아래 List에 "ADMIN" 추가해서 확인 가능
        // =========================
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime yesterdayStart = yesterday.atStartOfDay();

        // 실제 Role 코드에 맞게 사용 (예: ADMIN / HEAD / LEADER / MEMBER)
        List<String> targetRoles = List.of("HEAD", "LEADER", "MEMBER");
        // 테스트용 예시:
        // List<String> targetRoles = List.of("ADMIN", "HEAD", "LEADER", "MEMBER");

        long todayLogins = loginHistoryRepository.countByUserRoleInAndLoginAtBetween(
                targetRoles,
                todayStart,
                now
        );

        long yesterdayLogins = loginHistoryRepository.countByUserRoleInAndLoginAtBetween(
                targetRoles,
                yesterdayStart,
                todayStart
        );

        log.info("[AdminOverview] todayLogins={}, yesterdayLogins={}", todayLogins, yesterdayLogins);

        int dailyUserCount = (int) todayLogins;
        double dailyUserChange = calcChangeRate(todayLogins, yesterdayLogins);

        // =========================
        // 2) CPU 기반 시스템 부하 수준 (PrometheusMetricsService 활용)
        // =========================
        List<VmInstance> allVms = vmInstanceRepository.findAll();
        List<String> vmNames = allVms.stream()
                .map(VmInstance::getName)
                .toList();

        log.info("[AdminOverview] all VM names={}", vmNames);

        Map<String, VmMetricSummaryDto> metricsMap =
                prometheusMetricsService.getMetricsForVmNames(vmNames, null);

        log.info("[AdminOverview] metricsMap size={}, values={}", metricsMap.size(), metricsMap);

        double avgCpuUsage = metricsMap.values().stream()
                .filter(VmMetricSummaryDto::isHasMetrics)
                .map(VmMetricSummaryDto::getCpuUsage)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);

        double avgCpuPercent = avgCpuUsage * 100.0;
        String systemLoadLevel = resolveLoadLevel(avgCpuPercent);

        log.info("[AdminOverview] avgCpuUsage(raw)={}, avgCpuPercent={}, systemLoadLevel={}",
                avgCpuUsage, avgCpuPercent, systemLoadLevel);

        // =========================
        // 3) 에러 수 / 평균 응답시간: Prometheus 연동
        // =========================
        // 3-1) 기본값: up/down 요약 기반 (기존 로직)
        PrometheusSummaryResponse upSummary = prometheusMonitoringService.getUpSummary();
        int downTargets = upSummary.getDownTargets();

        log.info("[AdminOverview] upSummary total={}, up={}, down={}",
                upSummary.getTotalTargets(), upSummary.getUpTargets(), upSummary.getDownTargets());

        long errorCount24h = downTargets;   // 기본값: 다운 타겟 수
        long avgResponseMs = 0L;            // 기본값: 0 (미집계)

        // 유효성 플래그/원시값용 변수 (빌더에서 사용)
        Double avgMs = null;
        Long http5xxCount = null;

        // 3-2) HTTP 메트릭으로 override (성공하는 경우에만)
        try {
            // 최근 5분 평균 응답시간(ms)
            avgMs = prometheusMonitoringService.getAvgResponseMsLast5m();

            // 최근 24시간 HTTP 5xx 에러 수
            http5xxCount = prometheusMonitoringService.getHttp5xxCount24h();

            log.info("[AdminOverview] avgMs(from Prometheus)={}, http5xxCount={}", avgMs, http5xxCount);

            if (avgMs != null && !avgMs.isNaN() && !avgMs.isInfinite()) {
                avgResponseMs = Math.round(avgMs);
            }

            if (http5xxCount != null) {
                errorCount24h = http5xxCount;
            }

        } catch (Exception e) {
            // Prometheus HTTP 메트릭 조회가 실패해도 서비스 전체는 죽지 않도록
            log.warn("[AdminOverview] Prometheus HTTP metrics 조회 실패 - 기본값(up/down 기반)으로 대체", e);
        }

        // =========================
        // 4) DTO 빌드 (값 + 유효성 플래그 함께 내려주기)
        // =========================
        boolean avgResponseValid =
                (avgMs != null && !avgMs.isNaN() && !avgMs.isInfinite());
        boolean errorCountValid = (http5xxCount != null);

        log.info("[AdminOverview] final dto: dailyUserCount={}, dailyUserChange={}, systemLoadLevel={}, " +
                        "avgResponseMs={}, avgResponseValid={}, errorCount24h={}, errorCountValid={}",
                dailyUserCount, dailyUserChange, systemLoadLevel,
                avgResponseMs, avgResponseValid, errorCount24h, errorCountValid);

        return AdminOverviewDto.builder()
                .dailyUserCount(dailyUserCount)
                .dailyUserChange(dailyUserChange)
                .systemLoadLevel(systemLoadLevel)
                .avgResponseMs(avgResponseMs)
                .avgResponseValid(avgResponseValid)
                .errorCount24h(errorCount24h)
                .errorCountValid(errorCountValid)
                .build();
    }

    private double calcChangeRate(long today, long yesterday) {
        if (yesterday <= 0) {
            return today > 0 ? 100.0 : 0.0;
        }
        return ((double) (today - yesterday) / yesterday) * 100.0;
    }

    private String resolveLoadLevel(double cpuPercent) {
        if (cpuPercent < 40.0) {
            return "LOW";
        }
        if (cpuPercent < 75.0) {
            return "MEDIUM";
        }
        return "HIGH";
    }
}
