package com.cloudrangers.cloudpilot.service;

import com.cloudrangers.cloudpilot.dto.DatastoreUsageDto;
import com.cloudrangers.cloudpilot.dto.VmMetricSummaryDto;
import com.cloudrangers.cloudpilot.domain.MetricTarget;
import com.cloudrangers.cloudpilot.repository.MetricTargetRepository;
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
     * 🔹 Datastore 용량/사용률 조회
     *
     * vmware_datastore_capacity_size / vmware_datastore_freespace_size 를 사용
     */
    public Map<String, DatastoreUsageDto> getDatastoreUsage(List<String> dsNames) {
        Map<String, DatastoreUsageDto> result = new HashMap<>();

        if (dsNames == null || dsNames.isEmpty()) {
            log.info("[PrometheusMetrics] dsNames empty. return default.");
            return result;
        }

        try {
            // capacity / free 각각 한 번씩 조회 (ds_name label로 필터링)
            String joined = String.join("|", dsNames); // HDD1 (1)|NVME (1)
            String capacityQuery = String.format(
                    "vmware_datastore_capacity_size{job=\"vmware_vcenter\", ds_name=~\"%s\"}",
                    joined
            );
            String freeQuery = String.format(
                    "vmware_datastore_freespace_size{job=\"vmware_vcenter\", ds_name=~\"%s\"}",
                    joined
            );

            log.info("[PrometheusMetrics] datastore capacityQuery={}", capacityQuery);
            log.info("[PrometheusMetrics] datastore freeQuery={}", freeQuery);

            String capJson = prometheusClient.query(capacityQuery);
            String freeJson = prometheusClient.query(freeQuery);

            Map<String, Double> capacityMap = parseDatastoreValueMap(capJson);
            Map<String, Double> freeMap = parseDatastoreValueMap(freeJson);

            for (String name : dsNames) {
                Double cap = capacityMap.get(name);
                Double free = freeMap.get(name);

                if (cap == null || free == null) {
                    log.info("[PrometheusMetrics] datastore {} 값 없음 (cap={}, free={})", name, cap, free);
                    continue;
                }

                double used = cap - free;
                double usedPercent = (cap > 0) ? (used * 100.0 / cap) : 0.0;

                DatastoreUsageDto dto = DatastoreUsageDto.builder()
                        .dsName(name)
                        .capacityBytes(cap)
                        .freeBytes(free)
                        .usedBytes(used)
                        .usedPercent(usedPercent)
                        .build();

                result.put(name, dto);
            }

        } catch (Exception e) {
            log.error("[PrometheusMetrics] datastore usage 조회 중 예외", e);
        }

        return result;
    }

    /**
     * Prometheus 응답 파싱 (vector)
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

    /**
     * vmware_datastore_* 메트릭 instant query 결과를
     * ds_name -> value 맵으로 파싱
     */
    private Map<String, Double> parseDatastoreValueMap(String json) {
        Map<String, Double> map = new HashMap<>();
        if (json == null) {
            return map;
        }

        try {
            JsonNode root = objectMapper.readTree(json);
            if (!"success".equals(root.path("status").asText())) {
                log.warn("[Prometheus] datastore 응답 status != success: {}", json);
                return map;
            }

            JsonNode results = root.path("data").path("result");
            if (!results.isArray()) {
                return map;
            }

            for (JsonNode series : results) {
                String dsName = series.path("metric").path("ds_name").asText(null);
                JsonNode valueNode = series.path("value");
                if (dsName == null || !valueNode.isArray() || valueNode.size() < 2) {
                    continue;
                }
                double v = valueNode.get(1).asDouble();
                map.put(dsName, v);
            }

        } catch (Exception e) {
            log.error("[Prometheus] datastore value 파싱 실패", e);
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
