package com.cloudrangers.cloudpilot.controller.monitoring;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@Slf4j
@RequiredArgsConstructor
public class PrometheusMonitoringServiceImpl implements PrometheusMonitoringService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${monitoring.prometheus.base-url}")
    private String prometheusBaseUrl;

    @Value("${monitoring.prometheus.up-query:up}")
    private String upQuery;

    @Override
    public PrometheusSummary getPrometheusSummary() {

        try {
            // 예: http://172.16.5.68:30090/api/v1/query?query=up
            String url = UriComponentsBuilder.fromHttpUrl(prometheusBaseUrl)
                    .path("/api/v1/query")
                    .queryParam("query", upQuery)
                    .toUriString();

            log.info("[Prometheus] Calling: {}", url);

            String body = restTemplate.getForObject(url, String.class);
            log.debug("[Prometheus] response body = {}", body);

            if (body == null) {
                log.warn("[Prometheus] empty body from {}", url);
                return new PrometheusSummary(0, 0, 0);
            }

            JsonNode root = objectMapper.readTree(body);
            if (!"success".equals(root.path("status").asText())) {
                log.warn("[Prometheus] status != success: {}", body);
                return new PrometheusSummary(0, 0, 0);
            }

            JsonNode result = root.path("data").path("result");

            int total = result.size();
            int up = 0;
            int down = 0;

            for (JsonNode series : result) {
                JsonNode valueNode = series.path("value");
                if (valueNode.size() >= 2) {
                    double v = valueNode.get(1).asDouble();
                    if (v == 1.0) {
                        up++;
                    } else {
                        down++;
                    }
                }
            }

            log.info("[Prometheus] summary total={}, up={}, down={}", total, up, down);
            return new PrometheusSummary(total, up, down);

        } catch (Exception e) {
            // 예외는 밖으로 안 던지고, FE는 일단 동작하게 0,0,0을 돌려줌
            log.error("[Prometheus] summary 조회 중 예외 발생", e);
            return new PrometheusSummary(0, 0, 0);
        }
    }
}