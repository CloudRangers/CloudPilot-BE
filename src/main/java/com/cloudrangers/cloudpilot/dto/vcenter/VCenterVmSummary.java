package com.cloudrangers.cloudpilot.dto.vcenter;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * vCenter /api/vcenter/vm 응답의 개별 VM 요약 정보
 */
@Getter
@Setter
@NoArgsConstructor
public class VCenterVmSummary {

    private String vm;              // VM ID (예: "vm-123")
    private String name;            // VM 이름
    private String power_state;     // POWERED_ON, POWERED_OFF 등
    private Integer cpu_count;      // vCPU 개수
    private Long memory_size_MiB;   // 메모리(MiB)

    // 필요하면 나중에 필드 더 추가 가능 (예: folder, resource_pool 등)
}
