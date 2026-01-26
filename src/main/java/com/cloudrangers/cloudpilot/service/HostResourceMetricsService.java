package com.cloudrangers.cloudpilot.monitor.prometheus.service;

import com.cloudrangers.cloudpilot.monitor.prometheus.config.PrometheusProperties;
import com.cloudrangers.cloudpilot.monitor.prometheus.dto.HostResourceChartResponse;
import com.cloudrangers.cloudpilot.ops.dto.TimeSeriesPoint;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * vSphere 호스트 CPU/메모리 사용률을 Prometheus에서 조회해서
 * Recharts용 DTO로 변환하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HostResourceMetricsService {

    private final PrometheusClient prometheusClient;
    private final PrometheusProperties prometheusProperties;

    private static final DateTimeFormatter LABEL_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.of("Asia/Seoul"));

    /**
     * hostName 기준으로 CPU/메모리 사용률(%) 시계열 데이터 조회
     *
     * @param hostName     vmware_exporter 메트릭의 host_name 라벨 값
     * @param rangeMinutes 조회 범위 (분)
     * @param stepSeconds  step (초)
     */
    public HostResourceChartResponse getHostResourceSeries(
            String hostName,
            Integer rangeMinutes,
            Integer stepSeconds
    ) {
        int range = Optional.ofNullable(rangeMinutes)
                .orElse(Optional.ofNullable(prometheusProperties.getDefaultRangeMinutes())
                        .orElse(60));
        int step = Optional.ofNullable(stepSeconds)
                .orElse(Optional.ofNullable(prometheusProperties.getDefaultStepSeconds())
                        .orElse(60));

        Instant end = Instant.now();
        Instant start = end.minusSeconds(range * 60L);

        // 🔹 vmware_exporter 메트릭 기준 PromQL
        //   - CPU: vmware_host_cpu_usage / vmware_host_cpu_max * 100
        //   - MEM: vmware_host_memory_usage / vmware_host_memory_max * 100
        //   - job 라벨은 Prometheus 설정에 맞게 수정 (여기선 "vmware_vcenter" 가정)
        String cpuQuery = String.format(
                "100 * vmware_host_cpu_usage{job=\"vmware_vcenter\",host_name=\"%s\"}"
                        + " / vmware_host_cpu_max{job=\"vmware_vcenter\",host_name=\"%s\"}",
                hostName, hostName
        );

        String memQuery = String.format(
                "100 * vmware_host_memory_usage{job=\"vmware_vcenter\",host_name=\"%s\"}"
                        + " / vmware_host_memory_max{job=\"vmware_vcenter\",host_name=\"%s\"}",
                hostName, hostName
        );

        log.info("[HostResourceMetrics] hostName={}, cpuQuery={}", hostName, cpuQuery);
        log.info("[HostResourceMetrics] hostName={}, memQuery={}", hostName, memQuery);

        List<TimeSeriesPoint> cpuPoints =
                prometheusClient.queryRangeAsPoints(cpuQuery, start, end, step);

        List<TimeSeriesPoint> memPoints =
                prometheusClient.queryRangeAsPoints(memQuery, start, end, step);

        List<HostResourceChartResponse.ChartPoint> cpuSeries = cpuPoints.stream()
                .map(p -> new HostResourceChartResponse.ChartPoint(
                        LABEL_FORMATTER.format(p.getTimestamp()),
                        p.getValue()
                ))
                .collect(Collectors.toList());

        List<HostResourceChartResponse.ChartPoint> memorySeries = memPoints.stream()
                .map(p -> new HostResourceChartResponse.ChartPoint(
                        LABEL_FORMATTER.format(p.getTimestamp()),
                        p.getValue()
                ))
                .collect(Collectors.toList());

        return new HostResourceChartResponse(hostName, cpuSeries, memorySeries);
    }
}
