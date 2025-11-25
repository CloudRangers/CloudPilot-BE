package com.cloudrangers.cloudpilot.controller.monitoring;

/**
 * Prometheus 요약 정보 조회용 서비스 인터페이스
 */
public interface PrometheusMonitoringService {

    /**
     * Prometheus up 메트릭을 기반으로
     * 전체 타겟 수 / up / down 개수를 계산해서 돌려준다.
     */
    PrometheusSummary getPrometheusSummary();
}
