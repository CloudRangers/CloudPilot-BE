package com.cloudrangers.cloudpilot.service;

import com.cloudrangers.cloudpilot.config.PrometheusProperties;
import com.cloudrangers.cloudpilot.enums.TimeSeriesPoint;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PrometheusClient {

    private final PrometheusProperties properties;

    /**
     * 기존 구조 유지: 내부에서 RestTemplate 생성
     */
    private final RestTemplate restTemplate = new RestTemplate();

    // JSON 파싱용 ObjectMapper
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 🔹 Instant query (단일 시점)
     *  - UriComponentsBuilder 사용
     */
    public String query(String promQl) {
        // ✅ instant query도 URL 템플릿 + 변수 방식으로 통일
        String urlTemplate = properties.getBaseUrl()
                + "/api/v1/query?query={query}";

        log.info("[Prometheus][instant] promQl={}", promQl);

        String body = restTemplate.getForObject(
                urlTemplate,
                String.class,
                promQl   // {query} 자리에 들어갈 값
        );

        if (body == null) {
            log.warn("[Prometheus][instant] 응답 body 가 null 입니다. query={}", promQl);
        }
        return body;
    }

    /**
     * 🔹 Range query raw JSON
     *
     * @param promQl  PromQL 쿼리
     * @param start   시작 시각 (UTC)
     * @param end     종료 시각 (UTC)
     * @param stepSec step(초)
     */
    public String queryRangeRaw(String promQl, Instant start, Instant end, int stepSec) {

        long startSec = start.getEpochSecond();
        long endSec   = end.getEpochSecond();

        // ✅ URL 템플릿 + path 변수 방식 사용 (RestTemplate가 알아서 인코딩)
        String urlTemplate = properties.getBaseUrl()
                + "/api/v1/query_range"
                + "?query={query}&start={start}&end={end}&step={step}";

        log.info("[Prometheus][range] promQl={}", promQl);

        String urlForLog = urlTemplate
                .replace("{query}", promQl)
                .replace("{start}", String.valueOf(startSec))
                .replace("{end}", String.valueOf(endSec))
                .replace("{step}", String.valueOf(stepSec));
        log.info("[Prometheus][range] urlTemplate(before encode)={}", urlForLog);

        String body = restTemplate.getForObject(
                urlTemplate,
                String.class,
                promQl,          // {query}
                startSec,        // {start}
                endSec,          // {end}
                stepSec          // {step}
        );

        if (body == null) {
            log.warn("[Prometheus][range] 응답 body 가 null 입니다. query={}", promQl);
        }
        return body;
    }

    /**
     * 🔹 Range 결과 → TimeSeriesPoint 리스트로 변환
     */
    public List<TimeSeriesPoint> queryRangeAsPoints(String promQl, Instant start, Instant end, int stepSec) {
        String json = queryRangeRaw(promQl, start, end, stepSec);
        if (json == null) {
            return List.of();
        }

        List<TimeSeriesPoint> result = new ArrayList<>();

        try {
            JsonNode root = objectMapper.readTree(json);
            if (!"success".equals(root.path("status").asText())) {
                log.warn("[Prometheus][range] 응답 status != success: {}, query={}",
                        root.path("status").asText(), promQl);
                return List.of();
            }

            JsonNode data = root.path("data");
            JsonNode results = data.path("result");
            if (!results.isArray() || results.isEmpty()) {
                // 데이터 없음
                log.info("[Prometheus][range] result 가 비어있습니다. query={}", promQl);
                return List.of();
            }

            // 일단 첫 번째 시계열만 사용
            JsonNode firstSeries = results.get(0);
            JsonNode values = firstSeries.path("values");

            if (values.isArray()) {
                for (JsonNode v : values) {
                    if (!v.isArray() || v.size() < 2) {
                        continue;
                    }
                    long ts = v.get(0).asLong();
                    String valStr = v.get(1).asText();
                    double val;
                    try {
                        val = Double.parseDouble(valStr);
                    } catch (NumberFormatException e) {
                        // NaN, Inf 등은 스킵
                        continue;
                    }
                    result.add(new TimeSeriesPoint(
                            Instant.ofEpochSecond(ts),
                            val
                    ));
                }
            }

        } catch (Exception e) {
            log.error("[Prometheus][range] 응답 파싱 실패. query={}", promQl, e);
            return List.of();
        }

        return result;
    }
}
