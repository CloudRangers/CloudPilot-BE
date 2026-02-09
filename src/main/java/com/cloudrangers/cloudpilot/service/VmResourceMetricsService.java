package com.cloudrangers.cloudpilot.service;

import com.cloudrangers.cloudpilot.config.PrometheusProperties;
import com.cloudrangers.cloudpilot.dto.VmResourceChartResponse;
import com.cloudrangers.cloudpilot.enums.TimeSeriesPoint;
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
 * vCenter VM CPU/메모리 사용률 시계열 조회 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VmResourceMetricsService {

    private final PrometheusClient prometheusClient;
    private final PrometheusProperties prometheusProperties;

    private static final DateTimeFormatter LABEL_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.of("Asia/Seoul"));

    public VmResourceChartResponse getVmResourceSeries(
            String vmName,
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

        // 🔹 vmware_exporter 메트릭 기준 PromQL (이름은 환경에 맞게 조금 수정 가능)
        String cpuQuery = String.format(
                "100 * vmware_vm_cpu_usage_average{job=\"vmware_vcenter\",vm_name=\"%s\"}"
                        + " / vmware_vm_max_cpu_usage{job=\"vmware_vcenter\",vm_name=\"%s\"}",
                vmName, vmName
        );

        String memQuery = String.format(
                "100 * vmware_vm_mem_usage_average{job=\"vmware_vcenter\",vm_name=\"%s\"} " +
                        "/ vmware_vm_memory_max{job=\"vmware_vcenter\",vm_name=\"%s\"}",
                vmName, vmName
        );

        log.info("[VmResourceMetrics] vmName={}, cpuQuery={}", vmName, cpuQuery);
        log.info("[VmResourceMetrics] vmName={}, memQuery={}", vmName, memQuery);

        List<TimeSeriesPoint> cpuPoints =
                prometheusClient.queryRangeAsPoints(cpuQuery, start, end, step);
        List<TimeSeriesPoint> memPoints =
                prometheusClient.queryRangeAsPoints(memQuery, start, end, step);

        List<VmResourceChartResponse.ChartPoint> cpuSeries = cpuPoints.stream()
                .map(p -> new VmResourceChartResponse.ChartPoint(
                        LABEL_FORMATTER.format(p.getTimestamp()),
                        p.getValue()
                ))
                .collect(Collectors.toList());

        List<VmResourceChartResponse.ChartPoint> memorySeries = memPoints.stream()
                .map(p -> new VmResourceChartResponse.ChartPoint(
                        LABEL_FORMATTER.format(p.getTimestamp()),
                        p.getValue()
                ))
                .collect(Collectors.toList());

        return new VmResourceChartResponse(vmName, cpuSeries, memorySeries);
    }
}
