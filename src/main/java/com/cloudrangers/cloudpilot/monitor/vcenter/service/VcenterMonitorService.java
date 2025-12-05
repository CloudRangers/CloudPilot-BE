package com.cloudrangers.cloudpilot.monitor.vcenter.service;

import com.cloudrangers.cloudpilot.monitor.vcenter.dto.VcenterSummaryResponse;
import com.cloudrangers.cloudpilot.monitor.vcenter.dto.VcenterVmInfoDto;

import java.util.List;
import java.util.Map;

public interface VcenterMonitorService {

    /**
     * vCenter에서 VM 목록을 가져와 요약 통계 정보로 변환
     */
    VcenterSummaryResponse getSummary();

    /**
     * vCenter의 VM 목록 상세 조회 (기존 DTO)
     */
    List<VcenterVmInfoDto> getVmList();

    /**
     * ⭐ vCenter raw VM 목록 (Map) 그대로 반환
     *   - Admin overview, metrics, live-vms 등에 사용
     */
    List<Map<String, Object>> getLiveVmList();
}
