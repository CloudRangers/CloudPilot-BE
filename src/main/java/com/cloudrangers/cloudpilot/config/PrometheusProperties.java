package com.cloudrangers.cloudpilot.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * application.properties 의
 * monitoring.prometheus.* 값을 바인딩하는 설정 클래스
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "monitoring.prometheus")
public class PrometheusProperties {

    /**
     * 예: http://172.16.5.68:30090
     */
    private String baseUrl;

    /**
     * up 쿼리 (예: up)
     */
    private String upQuery;

    /**
     * 기본 조회 범위 (분 단위)
     */
    private Integer defaultRangeMinutes;

    /**
     * step (초 단위)
     */
    private Integer defaultStepSeconds;
}
