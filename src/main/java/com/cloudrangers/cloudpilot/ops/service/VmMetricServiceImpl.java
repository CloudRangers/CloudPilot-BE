package com.cloudrangers.cloudpilot.ops.service;

import com.cloudrangers.cloudpilot.monitor.prometheus.service.PrometheusClient;
import com.cloudrangers.cloudpilot.ops.domain.metric.MetricTarget;
import com.cloudrangers.cloudpilot.ops.domain.metric.MetricTargetRepository;
import com.cloudrangers.cloudpilot.ops.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class VmMetricServiceImpl implements VmMetricService {

    private final PrometheusClient prometheusClient;
    private final MetricTargetRepository metricTargetRepository;

    private static final DateTimeFormatter LABEL_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.of("Asia/Seoul"));

    /**
     * vmId → Prometheus instance 문자열로 변환
     * - "172.16.5.117:9100" 형태면 그대로 사용
     * - 숫자면 DB 조회 후 endpoint(=instance 라벨 값) 반환
     */
    private String resolveInstance(String vmId) {
        // Case 1: FE에서 이미 instance 형태로 전달 (예: "172.16.5.110:9100" or "nexus-node")
        if (vmId.contains(":") || !vmId.chars().allMatch(Character::isDigit)) {
            return vmId;
        }

        // Case 2: 숫자인 경우 MetricTarget lookup
        Long numericId;
        try {
            numericId = Long.valueOf(vmId);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid vmId format: " + vmId);
        }

        MetricTarget target = metricTargetRepository
                .findByVmInstanceIdAndExporterTypeAndActiveTrue(
                        numericId,
                        "NODE_EXPORTER"
                )
                .orElseThrow(() ->
                        new IllegalArgumentException("MetricTarget not found for vmId=" + vmId));

        // endpoint 필드에 Prometheus instance 라벨 값이 들어있다고 가정
        return target.getEndpoint();  // e.g., "172.16.5.112:9100" 또는 "nexus-node"
    }

    @Override
    public VmMetricResponse getVmMetrics(String vmId, String metricName,
                                         int rangeMinutes, int stepSeconds) {

        log.info("[VmMetricService] called. vmId={}, metricName={}, rangeMinutes={}, stepSeconds={}",
                vmId, metricName, rangeMinutes, stepSeconds);

        // vmId 형태(IP:PORT or 숫자 or 이름)에 상관없이 instance 문자열로 통일
        String instance = resolveInstance(vmId);

        // PromQL 생성
        String promQl = buildPromQl(metricName, instance);

        log.info("[VmMetricService] vmId={}, metricName={}, promQl={}", vmId, metricName, promQl);

        // 시간 범위
        Instant end = Instant.now();
        Instant start = end.minusSeconds(rangeMinutes * 60L);

        // Prometheus로부터 시계열 데이터 가져오기
        List<TimeSeriesPoint> points =
                prometheusClient.queryRangeAsPoints(promQl, start, end, stepSeconds);

        return VmMetricResponse.builder()
                .vmId(vmId)
                .metricName(metricName)
                .data(points)
                .build();
    }

    @Override
    public MetricAggregation getMetricAggregation(String vmId, String metricName, int rangeMinutes) {

        VmMetricResponse ts = getVmMetrics(vmId, metricName, rangeMinutes, 60);

        if (ts.getData().isEmpty()) {
            return new MetricAggregation(vmId, metricName, null, null, null, null);
        }

        double sum = 0;
        double max = Double.NEGATIVE_INFINITY;
        double min = Double.POSITIVE_INFINITY;

        for (TimeSeriesPoint p : ts.getData()) {
            double v = p.getValue();
            sum += v;
            max = Math.max(max, v);
            min = Math.min(min, v);
        }

        double avg = sum / ts.getData().size();
        double latest = ts.getData().get(ts.getData().size() - 1).getValue();

        return new MetricAggregation(vmId, metricName, avg, max, min, latest);
    }

    @Override
    public RechartsDataResponse getMetricsForRecharts(String vmId, String metricName,
                                                      int rangeMinutes, int stepSeconds) {

        VmMetricResponse ts = getVmMetrics(vmId, metricName, rangeMinutes, stepSeconds);

        List<RechartsDataResponse.RechartsPoint> chartPoints = ts.getData()
                .stream()
                .map(p -> new RechartsDataResponse.RechartsPoint(
                        LABEL_FORMATTER.format(p.getTimestamp()),
                        p.getValue()
                ))
                .toList();

        return new RechartsDataResponse(vmId, metricName, chartPoints);
    }

    /**
     * PromQL 생성
     *  - job 라벨 필터 제거 (instance 만 맞추도록)
     */
    private String buildPromQl(String metricName, String instance) {

        // 🔹 job 필터 제거, instance 만 사용
        String selector = String.format("instance=\"%s\"", instance);

        switch (metricName) {
            case "vm_cpu_usage_percent":
                return String.format(
                        "avg by (instance) (rate(node_cpu_seconds_total{%s,mode!=\"idle\"}[5m])) * 100",
                        selector
                );

            case "vm_memory_usage_percent":
                return String.format(
                        "(1 - (node_memory_MemAvailable_bytes{%1$s} / " +
                                "      node_memory_MemTotal_bytes{%1$s})) * 100",
                        selector
                );

            default:
                // 기타 메트릭은 selector 만 붙여서 그대로 사용
                return String.format("%s{%s}", metricName, selector);
        }
    }
}
