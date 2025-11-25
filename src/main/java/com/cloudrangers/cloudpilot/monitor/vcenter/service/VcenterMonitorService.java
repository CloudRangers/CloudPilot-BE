package com.cloudrangers.cloudpilot.monitor.vcenter.service;

import com.cloudrangers.cloudpilot.monitor.vcenter.dto.VcenterSummaryResponse;
import com.cloudrangers.cloudpilot.monitor.vcenter.dto.VcenterVmInfoDto;

import java.util.List;

public interface VcenterMonitorService {

    /**
     * vCenter에서 VM 목록을 가져와 요약 통계 정보로 변환
     */
    VcenterSummaryResponse getSummary();

    /**
     * vCenter의 VM 목록 상세 조회
     */
    List<VcenterVmInfoDto> getVmList();
}
