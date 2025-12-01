package com.cloudrangers.cloudpilot.monitor.prometheus.service;

import com.cloudrangers.cloudpilot.monitor.prometheus.dto.VmMetricSummaryDto;
import com.cloudrangers.cloudpilot.ops.domain.metric.MetricTarget;
import com.cloudrangers.cloudpilot.ops.domain.metric.MetricTargetRepository;
import com.cloudrangers.cloudpilot.domain.vm.VmInstance;
import com.cloudrangers.cloudpilot.repository.vm.VmInstanceRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PrometheusMetricsService {

    private final PrometheusClient prometheusClient;
    private final ObjectMapper objectMapper;

    private final VmInstanceRepository vmInstanceRepository;
    private final MetricTargetRepository metricTargetRepository;

    /**
     * teamId 필터링 포함 버전
     */
    public Map<String, VmMetricSummaryDto> getMetricsForVmNames(
            List<String> vmNames,
            Long teamId     // ⭐ 추가된 파라미터
    ) {

        // 기본 결과 초기화
        Map<String, VmMetricSummaryDto> result = new HashMap<>();
        if (vmNames != null) {
            vmNames.forEach(name ->
                    result.put(name, VmMetricSummaryDto.builder()
                            .hasMetrics(false)
                            .cpuUsage(null)
                            .memoryUsage(null)
                            .build())
            );
        }

        if (vmNames == null || vmNames.isEmpty()) {
            log.info("[PrometheusMetrics] vmNames empty. return default.");
            return result;
        }

        // ⭐ 1) teamId 필터 적용된 vm_instance 조회
        List<VmInstance> vmInstances =
                (teamId == null)
                        ? vmInstanceRepository.findByNameIn(vmNames)
                        : vmInstanceRepository.findByNameInAndTeamId(vmNames, teamId);

        if (vmInstances.isEmpty()) {
            log.info("[PrometheusMetrics] vmInstances empty. vmNames={}, teamId={}", vmNames, teamId);
            return result;
        }

        List<Long> vmInstanceIds = vmInstances.stream()
                .map(VmInstance::getId)
                .toList();

        log.info("[PrometheusMetrics] vmInstances size={}, ids={}", vmInstances.size(), vmInstanceIds);

        // ⭐ 2) metric_target 조회 (active + NODE_EXPORTER)
        List<MetricTarget> targets =
                metricTargetRepository.findByVmInstanceIdInAndActiveTrue(vmInstanceIds);

        Map<Long, MetricTarget> metricTargets = targets.stream()
                .filter(mt -> "NODE_EXPORTER".equalsIgnoreCase(mt.getExporterType()))
                .collect(Collectors.toMap(
                        MetricTarget::getVmInstanceId,
                        Function.identity(),
                        (a, b) -> a
                ));

        if (metricTargets.isEmpty()) {
            log.info("[PrometheusMetrics] no MetricTarget found for teamId={}", teamId);
        }

        // ⭐ 3) instance → vmName 매핑
        Map<String, String> instanceToVmName = new HashMap<>();

        for (VmInstance v : vmInstances) {
            MetricTarget mt = metricTargets.get(v.getId());
            if (mt != null && mt.getEndpoint() != null) {
                instanceToVmName.put(mt.getEndpoint(), v.getName());
            }
        }

        log.info("[PrometheusMetrics] instanceToVmName = {}", instanceToVmName);

        if (instanceToVmName.isEmpty()) {
            return result;
        }

        // ⭐ 4) Prometheus 쿼리
        String cpuQuery =
                "avg(rate(node_cpu_seconds_total{job=\"node-exporter\",mode!=\"idle\"}[5m])) by (instance)";
        String memQuery =
                "1 - (node_memory_MemAvailable_bytes{job=\"node-exporter\"} / node_memory_MemTotal_bytes{job=\"node-exporter\"})";

        try {
            // CPU
            String cpuJson = prometheusClient.query(cpuQuery);
            if (cpuJson != null) {
                Map<String, Double> cpuByInstance = parseVector(cpuJson);
                mergeCpu(result, cpuByInstance, instanceToVmName);
            }

            // Memory
            String memJson = prometheusClient.query(memQuery);
            if (memJson != null) {
                Map<String, Double> memByInstance = parseVector(memJson);
                mergeMem(result, memByInstance, instanceToVmName);
            }

        } catch (Exception e) {
            log.warn("[PrometheusMetrics] Prometheus 조회 중 오류", e);
        }

        return result;
    }


    /**
     * Prometheus 응답 파싱
     */
    private Map<String, Double> parseVector(String json) throws IOException {
        Map<String, Double> map = new HashMap<>();

        JsonNode root = objectMapper.readTree(json);
        JsonNode result = root.path("data").path("result");

        if (!result.isArray()) {
            return map;
        }

        for (JsonNode e : result) {
            String instance = e.path("metric").path("instance").asText(null);
            JsonNode valueArr = e.path("value");

            if (instance == null || !valueArr.isArray() || valueArr.size() < 2) {
                continue;
            }

            double value = valueArr.get(1).asDouble();
            map.put(instance, value);
        }

        return map;
    }

    private void mergeCpu(Map<String, VmMetricSummaryDto> base,
                          Map<String, Double> cpu,
                          Map<String, String> instanceToVmName) {

        cpu.forEach((instance, value) -> {
            String vmName = instanceToVmName.get(instance);
            if (vmName == null) return;

            VmMetricSummaryDto dto = base.get(vmName);
            if (dto != null) {
                dto.setHasMetrics(true);
                dto.setCpuUsage(value);
            }
        });
    }

    private void mergeMem(Map<String, VmMetricSummaryDto> base,
                          Map<String, Double> mem,
                          Map<String, String> instanceToVmName) {

        mem.forEach((instance, value) -> {
            String vmName = instanceToVmName.get(instance);
            if (vmName == null) return;

            VmMetricSummaryDto dto = base.get(vmName);
            if (dto != null) {
                dto.setHasMetrics(true);
                dto.setMemoryUsage(value);
            }
        });
    }
}
