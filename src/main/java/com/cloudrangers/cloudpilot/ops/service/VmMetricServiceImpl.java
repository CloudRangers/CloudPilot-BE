package com.cloudrangers.cloudpilot.ops.service;

import com.cloudrangers.cloudpilot.ops.dto.MetricAggregation;
import com.cloudrangers.cloudpilot.ops.dto.MetricSeriesResponse;
import com.cloudrangers.cloudpilot.ops.dto.RechartsDataResponse;
import com.cloudrangers.cloudpilot.ops.dto.TimeSeriesPoint;
import com.cloudrangers.cloudpilot.ops.dto.VmMetricResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.DoubleSummaryStatistics;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class VmMetricServiceImpl implements VmMetricService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${monitoring.prometheus.base-url}")
    private String prometheusBaseUrl;

    @Value("${monitoring.prometheus.default-range-minutes:60}")
    private int defaultRangeMinutes;

    @Value("${monitoring.prometheus.default-step-seconds:60}")
    private int defaultStepSeconds;

    private static final DateTimeFormatter KST_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    .withZone(ZoneId.of("Asia/Seoul"));

    /**
     * 🔧 디버그 모드 스위치
     * true  → up{instance="..."} 만 조회 (파이프라인 검증용)
     * false → 실제 CPU/메모리 퍼센트 PromQL 사용
     */
    //private static final boolean DEBUG_SIMPLE_QUERY = true;
    private static final boolean DEBUG_SIMPLE_QUERY = false;

    @Override
    public VmMetricResponse getVmMetrics(String vmId,
                                         String metricName,
                                         int rangeMinutes,
                                         int stepSeconds) {

        int range = rangeMinutes > 0 ? rangeMinutes : defaultRangeMinutes;
        int step = stepSeconds > 0 ? stepSeconds : defaultStepSeconds;

        Instant end = Instant.now();
        Instant start = end.minusSeconds(range * 60L);

        String promQuery = buildPromQuery(metricName, vmId);
        log.debug("[VmMetricService] vmId={}, metricName={}, query={}", vmId, metricName, promQuery);

        List<TimeSeriesPoint> points = queryRange(promQuery, start, end, step);

        List<MetricSeriesResponse> series = new ArrayList<>();
        series.add(new MetricSeriesResponse(metricName, vmId, points));

        return new VmMetricResponse(vmId, series);
    }

    @Override
    public MetricAggregation getMetricAggregation(String vmId,
                                                  String metricName,
                                                  int rangeMinutes) {

        VmMetricResponse vmMetricResponse = getVmMetrics(
                vmId,
                metricName,
                rangeMinutes,
                defaultStepSeconds
        );

        List<TimeSeriesPoint> points =
                vmMetricResponse.getSeries().isEmpty()
                        ? List.of()
                        : vmMetricResponse.getSeries().get(0).getPoints();

        if (points.isEmpty()) {
            return new MetricAggregation(vmId, metricName, null, null, null, null);
        }

        DoubleSummaryStatistics stats = points.stream()
                .mapToDouble(TimeSeriesPoint::getValue)
                .summaryStatistics();

        double latest = points.get(points.size() - 1).getValue();

        return new MetricAggregation(
                vmId,
                metricName,
                stats.getAverage(),
                stats.getMax(),
                stats.getMin(),
                latest
        );
    }

    @Override
    public RechartsDataResponse getMetricsForRecharts(String vmId,
                                                      String metricName,
                                                      int rangeMinutes,
                                                      int stepSeconds) {

        VmMetricResponse vmMetricResponse = getVmMetrics(
                vmId,
                metricName,
                rangeMinutes,
                stepSeconds
        );

        List<TimeSeriesPoint> points =
                vmMetricResponse.getSeries().isEmpty()
                        ? List.of()
                        : vmMetricResponse.getSeries().get(0).getPoints();

        List<RechartsDataResponse.RechartsPoint> data = new ArrayList<>();
        for (TimeSeriesPoint p : points) {
            String ts = KST_FORMATTER.format(p.getTimestamp());
            data.add(new RechartsDataResponse.RechartsPoint(ts, p.getValue()));
        }

        return new RechartsDataResponse(vmId, metricName, data);
    }

    /**
     * metricName(논리 이름) + vmId(instance) 기준으로 PromQL 생성
     *
     * vmId 예: "172.16.5.112:9100"
     *
     * metricName 은 다음 둘을 우선 지원:
     *  - "vm_cpu_usage_percent"
     *  - "vm_memory_usage_percent"
     */
    private String buildPromQuery(String metricName, String vmId) {

        // 1️⃣ 검증 모드: up{instance="..."} 만 조회
        if (DEBUG_SIMPLE_QUERY) {
            // 이 쿼리는 이미 UI에서 정상 동작 확인됨
            return "up{instance=\"" + vmId + "\"}";
        }

        // 2️⃣ 실제 모드: CPU/메모리 퍼센트
        switch (metricName) {
            case "vm_cpu_usage_percent":
                // CPU 사용률(%) = 100 - idle%
                return "100 - ("
                        + "avg by (instance) ("
                        + "rate(node_cpu_seconds_total{instance=\"" + vmId + "\", mode=\"idle\"}[5m])"
                        + ") * 100"
                        + ")";

            case "vm_memory_usage_percent":
                // 메모리 사용률(%) = (1 - available/total) * 100
                return "("
                        + "1 - ("
                        + "node_memory_MemAvailable_bytes{instance=\"" + vmId + "\"}"
                        + " / "
                        + "node_memory_MemTotal_bytes{instance=\"" + vmId + "\"}"
                        + ")"
                        + ") * 100";

            default:
                log.warn("Unknown metricName '{}', using as raw PromQL", metricName);
                return metricName;
        }
    }

    /**
     * Prometheus /api/v1/query_range 호출해서 시계열 데이터 파싱
     */
    private List<TimeSeriesPoint> queryRange(String promQuery,
                                             Instant start,
                                             Instant end,
                                             int stepSeconds) {

        try {
            long startEpoch = start.getEpochSecond();
            long endEpoch = end.getEpochSecond();

            // ⚠️ 여기서 더 이상 URLEncoder 사용하지 않음
            String urlTemplate = prometheusBaseUrl
                    + "/api/v1/query_range"
                    + "?query={query}&start={start}&end={end}&step={step}";

            log.debug("[VmMetricService] Calling Prometheus query_range: base={}, query={}, start={}, end={}, step={}",
                    prometheusBaseUrl, promQuery, startEpoch, endEpoch, stepSeconds);

            String body = restTemplate.getForObject(
                    urlTemplate,
                    String.class,
                    promQuery,        // {query}
                    startEpoch,       // {start}
                    endEpoch,         // {end}
                    stepSeconds       // {step}
            );

            if (body == null) {
                log.warn("[VmMetricService] Prometheus returned null body");
                return List.of();
            }

            log.debug("[VmMetricService] Prometheus raw response (first 300 chars): {}",
                    body.length() > 300 ? body.substring(0, 300) + "..." : body);

            JsonNode root = objectMapper.readTree(body);
            if (!"success".equals(root.path("status").asText())) {
                log.warn("Prometheus query failed: {}", body);
                return List.of();
            }

            JsonNode resultNode = root.path("data").path("result");
            if (!resultNode.isArray() || resultNode.isEmpty()) {
                return List.of();
            }

            JsonNode firstSeries = resultNode.get(0);
            JsonNode valuesNode = firstSeries.path("values");

            List<TimeSeriesPoint> points = new ArrayList<>();
            if (valuesNode.isArray()) {
                for (JsonNode v : valuesNode) {
                    if (v.size() < 2) continue;

                    long epoch = v.get(0).asLong();
                    double value = v.get(1).asDouble();
                    points.add(new TimeSeriesPoint(Instant.ofEpochSecond(epoch), value));
                }
            }

            return points;

        } catch (Exception e) {
            log.error("Error querying Prometheus: {}", e.getMessage(), e);
            return List.of();
        }
    }
}
