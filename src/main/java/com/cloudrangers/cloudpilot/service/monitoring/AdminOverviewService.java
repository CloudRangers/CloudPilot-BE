package com.cloudrangers.cloudpilot.service.monitoring;

import com.cloudrangers.cloudpilot.dto.monitoring.AdminOverviewDto;

/**
 * 관리자 대시보드 운영 모니터링 요약 조회 서비스 인터페이스
 */
public interface AdminOverviewService {

    /**
     * 관리자 대시보드 상단 카드(운영 모니터링 요약)에 필요한 데이터 조회
     */
    AdminOverviewDto getOverview();
}
