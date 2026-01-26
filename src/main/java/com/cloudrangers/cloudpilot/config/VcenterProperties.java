package com.cloudrangers.cloudpilot.monitor.vcenter.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "vcenter")
@Getter
@Setter
public class VcenterProperties {

    private String baseUrl;
    private String username;
    private String password;

    // 동적으로 갱신됨 — properties 파일에는 이제 session-id 넣지 않음
    private String sessionId;
}
