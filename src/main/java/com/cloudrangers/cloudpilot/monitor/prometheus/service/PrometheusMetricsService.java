package com.cloudrangers.cloudpilot.monitor.prometheus.service;

import com.cloudrangers.cloudpilot.monitor.prometheus.dto.VmMetricSummaryDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Prometheus에서 VM 단위 메트릭 요약을 조회하는 서비스
 *
 * 지금은 예시로 node_exporter 기반 메트릭(node_cpu_seconds_total, node_memory_*)을 사용하고,
 * Prometheus의 "instance" 라벨을 vCenter VM 이름과 매핑한다고 가정.
 * (⚠️ instance ↔ VM name 매핑 규칙은 인프라 쪽이랑 협의 후 수정 필요)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PrometheusMetricsService {

    private final PrometheusClient prometheusClient;
    private final ObjectMapper objectMapper;

    /**
     * vmNames 기준으로 Prometheus에서 CPU/메모리 사용률을 조회하여
     * Map<vmName, VmMetricSummaryDto> 형태로 반환
     */
    public Map<String, VmMetricSummaryDto> getMetricsForVmNames(List<String> vmNames) {

        // 기본값: 모두 hasMetrics = false
        Map<String, VmMetricSummaryDto> result = new HashMap<>();
        vmNames.forEach(name -> result.put(
                name,
                VmMetricSummaryDto.builder()
                        .hasMetrics(false)
                        .cpuUsage(null)
                        .memoryUsage(null)
                        .build()
        ));

        // ✅ 1) CPU 사용률 (node_exporter 기준 예시)
        //    avg(rate(node_cpu_seconds_total{mode!="idle"}[5m])) by (instance)
        String cpuQuery = "avg(rate(node_cpu_seconds_total{mode!=\"idle\"}[5m])) by (instance)";

        // ✅ 2) 메모리 사용률 (1 - MemAvailable/MemTotal)
        String memQuery = "1 - (node_memory_MemAvailable_bytes / node_memory_MemTotal_bytes)";

        try {
            // ----- CPU -----
            String cpuJson = prometheusClient.query(cpuQuery);
            if (cpuJson != null) {
                Map<String, Double> cpuByInstance = parseVectorResult(cpuJson, "instance");
                mergeCpuUsage(result, cpuByInstance);
            }

            // ----- 메모리 -----
            String memJson = prometheusClient.query(memQuery);
            if (memJson != null) {
                Map<String, Double> memByInstance = parseVectorResult(memJson, "instance");
                mergeMemoryUsage(result, memByInstance);
            }

        } catch (Exception e) {
            log.warn("Prometheus 메트릭 파싱 중 오류 발생", e);
        }

        return result;
    }

    /**
     * Prometheus HTTP API (query) 응답(JSON)에서
     * data.result[*].metric[labelKey], data.result[*].value[1] 을 파싱하여
     * Map<labelValue, value> 로 반환.
     *
     * 예:
     *  - labelKey = "instance"
     */
    private Map<String, Double> parseVectorResult(String json, String labelKey) throws IOException {
        Map<String, Double> out = new HashMap<>();

        JsonNode root = objectMapper.readTree(json);
        JsonNode resultArray = root.path("data").path("result");

        if (!resultArray.isArray()) {
            return out;
        }

        for (JsonNode node : resultArray) {
            String labelValue = node.path("metric").path(labelKey).asText(null);
            JsonNode valueNode = node.path("value");

            if (labelValue == null || !valueNode.isArray() || valueNode.size() < 2) {
                continue;
            }

            double value = valueNode.get(1).asDouble();
            out.put(labelValue, value);
        }

        return out;
    }

    /**
     * CPU 메트릭을 VmMetricSummaryDto에 머지
     * 현재는 Prometheus "instance" 라벨 값이 vmName 과 같다고 가정.
     *
     * ⚠️ instance 값이 "vm1:9100" 처럼 되어 있으면,
     *    vCenter VM name과의 매핑 로직을 여기에 커스터마이징해야 함.
     */
    private void mergeCpuUsage(Map<String, VmMetricSummaryDto> base,
                               Map<String, Double> cpuByInstance) {

        cpuByInstance.forEach((instance, cpu) -> {
            VmMetricSummaryDto dto = base.get(instance);
            if (dto != null) {
                dto.setHasMetrics(true);
                dto.setCpuUsage(cpu);
            }
        });
    }

    /**
     * 메모리 메트릭을 VmMetricSummaryDto에 머지
     */
    private void mergeMemoryUsage(Map<String, VmMetricSummaryDto> base,
                                  Map<String, Double> memByInstance) {

        memByInstance.forEach((instance, mem) -> {
            VmMetricSummaryDto dto = base.get(instance);
            if (dto != null) {
                dto.setHasMetrics(true);
                dto.setMemoryUsage(mem);
            }
        });
    }
}
