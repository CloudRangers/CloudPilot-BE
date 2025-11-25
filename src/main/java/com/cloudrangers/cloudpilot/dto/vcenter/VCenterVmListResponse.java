package com.cloudrangers.cloudpilot.dto.vcenter;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * vCenter /api/vcenter/vm 전체 응답 래퍼 (value 배열)
 */
@Getter
@Setter
@NoArgsConstructor
public class VCenterVmListResponse {
    private List<VCenterVmSummary> value;
}
