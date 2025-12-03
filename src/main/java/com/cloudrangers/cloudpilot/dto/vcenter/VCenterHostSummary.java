package com.cloudrangers.cloudpilot.dto.vcenter;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * vCenter /api/vcenter/host 응답의 개별 Host 정보
 */
@Getter
@Setter
@NoArgsConstructor
public class VCenterHostSummary {

    private String host;               // host ID
    private String name;               // 호스트 이름
    private String connection_state;   // CONNECTED, DISCONNECTED
    private String power_state;        // POWERED_ON 등 (버전에 따라 없을 수도 있음)

    // 필요시 필드 추가 가능
}
