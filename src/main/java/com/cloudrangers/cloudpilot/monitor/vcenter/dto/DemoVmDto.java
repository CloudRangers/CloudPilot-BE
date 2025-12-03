package com.cloudrangers.cloudpilot.monitor.vcenter.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DemoVmDto {
    private String name;
    private Double cpuUsage;    // 0~1 값, FE에서 *100 해서 %
    private Double memoryUsage; // 0~1 값
}
