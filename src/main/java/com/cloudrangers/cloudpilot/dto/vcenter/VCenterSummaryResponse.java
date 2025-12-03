package com.cloudrangers.cloudpilot.dto.vcenter;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class VCenterSummaryResponse {

    // VM 요약
    private int totalVms;
    private long poweredOnVms;
    private long poweredOffVms;

    // Host 요약
    private int totalHosts;
    private long connectedHosts;
    private long disconnectedHosts;
}
