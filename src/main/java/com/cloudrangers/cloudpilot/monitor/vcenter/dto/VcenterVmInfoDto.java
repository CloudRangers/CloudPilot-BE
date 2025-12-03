package com.cloudrangers.cloudpilot.monitor.vcenter.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VcenterVmInfoDto {

    private String vmId;        // vCenter VM ID (예: "vm-123")
    private String name;        // VM 이름
    private String powerState;  // POWERED_ON / POWERED_OFF / SUSPENDED ...
    private Integer cpuCount;   // vCPU 개수
    private Long memorySizeMiB; // 메모리(MiB)
    private String guestOs;     // 게스트 OS 이름
    private String ipAddress;   // IP 주소 (있을 경우)
}
