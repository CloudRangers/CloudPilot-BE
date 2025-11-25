// com.cloudrangers.cloudpilot.monitor.prometheus.service.PrometheusClient

package com.cloudrangers.cloudpilot.monitor.prometheus.service;

import com.cloudrangers.cloudpilot.monitor.prometheus.config.PrometheusProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class PrometheusClient {

    private final PrometheusProperties properties;
    private final RestTemplate restTemplate = new RestTemplate();

    public String query(String promQl) {
        String url = properties.getBaseUrl() + "/api/v1/query?query=" + promQl;

        log.debug("Prometheus API 호출: {}", url);

        ResponseEntity<String> response =
                restTemplate.getForEntity(url, String.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            log.warn("Prometheus 응답 오류: status={}, body={}",
                    response.getStatusCode(), response.getBody());
            return null;
        }
        return response.getBody();
    }
}
