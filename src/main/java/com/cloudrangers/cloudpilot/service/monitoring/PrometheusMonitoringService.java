package com.cloudrangers.cloudpilot.service.monitoring;

import com.cloudrangers.cloudpilot.dto.monitoring.PrometheusQueryResponse;
import com.cloudrangers.cloudpilot.dto.monitoring.PrometheusSummaryResponse;
import com.cloudrangers.cloudpilot.dto.DatastoreUsageDto;
import com.cloudrangers.cloudpilot.dto.VcenterSummaryResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class PrometheusMonitoringService {

    @Value("${monitoring.prometheus.base-url}")
    private String prometheusBaseUrl;

    @Value("${monitoring.prometheus.up-query:up}")
    private String upQuery;

    /**
     * 최근 5분 평균 응답시간(ms)을 가져오기 위한 PromQL
     * 필요하면 yml에서 overriding 가능
     */
    @Value("${monitoring.prometheus.http-avg-latency-query:" +
            "rate(http_server_requests_seconds_sum[5m]) / " +
            "rate(http_server_requests_seconds_count[5m]) * 1000}")
    private String httpAvgLatencyQuery;

    /**
     * 최근 24시간 HTTP 5xx 카운트를 가져오기 위한 PromQL
     */
    @Value("${monitoring.prometheus.http-5xx-count-query:" +
            "sum(increase(http_server_requests_seconds_count{status=~\"5..\"}[24h]))}")
    private String http5xxCountQuery;

    // =========================================================
    // 🔹 0) up() 메트릭 요약
    // =========================================================

    /**
     * Prometheus up 메트릭을 조회해서
     * - 전체 타겟 수
     * - up(1)인 타겟 수
     * - down(0 또는 그 외) 타겟 수
     * 를 계산해서 반환
     */
    public PrometheusSummaryResponse getUpSummary() {
        try {
            String url = prometheusBaseUrl + "/api/v1/query?query=" + upQuery;
            log.info("Calling Prometheus (upSummary): {}", url);

            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<PrometheusQueryResponse> response =
                    restTemplate.getForEntity(url, PrometheusQueryResponse.class);

            PrometheusQueryResponse body = response.getBody();
            if (body == null || body.getData() == null || body.getData().getResult() == null) {
                log.warn("Prometheus response is null or invalid (upSummary)");
                return new PrometheusSummaryResponse(0, 0, 0);
            }

            List<PrometheusQueryResponse.Result> results = body.getData().getResult();

            int total = results.size();
            int up = 0;

            for (PrometheusQueryResponse.Result r : results) {
                List<Object> value = r.getValue();
                if (value != null && value.size() >= 2) {
                    Object v = value.get(1);
                    if ("1".equals(v.toString())) up++;
                }
            }

            int down = total - up;
            return new PrometheusSummaryResponse(total, up, down);

        } catch (Exception e) {
            log.error("getUpSummary() error → return zero", e);
            return new PrometheusSummaryResponse(0, 0, 0);
        }
    }

    // =========================================================
    // 🔹 1) 평균 응답시간 & 5xx 카운트용 헬퍼
    // =========================================================

    /**
     * 최근 5분 평균 응답시간(ms) 조회
     */
    public Double getAvgResponseMsLast5m() {
        Double value = querySingleValue(httpAvgLatencyQuery);
        log.debug("Prometheus avg response ms (5m): {}", value);
        return value;
    }

    /**
     * 최근 24시간 HTTP 5xx 응답 개수 조회
     */
    public Long getHttp5xxCount24h() {
        Double value = querySingleValue(http5xxCountQuery);
        Long result = (value != null) ? value.longValue() : 0L;
        log.debug("Prometheus http 5xx count (24h): {}", result);
        return result;
    }

    /**
     * 공통: PromQL 한 개를 날려서 single value(Double)로 받아오는 헬퍼
     *  - 실패 / 빈 결과 / 포맷 이상 → null
     *  - 실제 디폴트 처리는 상위 레벨(queryIntValueOrDefault 등)에서 함
     */
    private Double querySingleValue(String promQl) {
        try {
            String url = prometheusBaseUrl + "/api/v1/query?query={q}";
            log.info("Calling Prometheus(single): baseUrl={}, query={}", prometheusBaseUrl, promQl);

            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<PrometheusQueryResponse> response =
                    restTemplate.getForEntity(url, PrometheusQueryResponse.class, promQl);

            PrometheusQueryResponse body = response.getBody();
            if (body == null || body.getData() == null || body.getData().getResult() == null) {
                log.warn("Prometheus single-value response is null or invalid. query={}", promQl);
                return null;
            }

            List<PrometheusQueryResponse.Result> results = body.getData().getResult();
            if (results.isEmpty()) {
                log.warn("Prometheus single-value result is empty for query: {}", promQl);
                return null;
            }

            // 첫 번째 결과만 사용
            PrometheusQueryResponse.Result r = results.get(0);
            List<Object> value = r.getValue();
            if (value == null || value.size() < 2 || value.get(1) == null) {
                log.warn("Prometheus single-value format invalid for query: {}", promQl);
                return null;
            }

            String vStr = value.get(1).toString();
            return Double.parseDouble(vStr);
        } catch (Exception e) {
            log.warn("Failed to query Prometheus single value. query={}", promQl, e);
            return null;
        }
    }

    // =========================================================
    // 🔹 2) VM 개수용 Int 헬퍼 (기본값 허용)
    // =========================================================

    /**
     * PromQL을 실행해서 int 값으로 반환.
     * - Prometheus 응답이 null/빈 결과/포맷 이상 → defaultValue 사용
     * - 음수 값 → 0으로 클램핑
     */
    public int queryIntValueOrDefault(String promQl, int defaultValue) {
        Double v = querySingleValue(promQl);
        if (v == null) {
            log.warn("Prometheus int-value is null. query={} → use default={}", promQl, defaultValue);
            return defaultValue;
        }

        int result = v.intValue();
        if (result < 0) {
            log.warn("Prometheus int-value is negative. query={}, value={} → clamp to 0", promQl, result);
            return 0;
        }

        return result;
    }

    // =========================================================
    // 🔹 3) vCenter VM 요약 (SummaryCards용)
    //    → Prometheus에 등록된 VM 기준으로 Summary 생성
    // =========================================================
    public VcenterSummaryResponse getVcenterVmSummaryFromPrometheus() {

        String totalQuery      = "count(vmware_vm_power_state{job=\"vmware_vcenter\"})";
        String poweredOnQuery  = "count(vmware_vm_power_state{job=\"vmware_vcenter\", power_state=\"poweredOn\"})";
        String poweredOffQuery = "count(vmware_vm_power_state{job=\"vmware_vcenter\", power_state=\"poweredOff\"})";
        String suspendedQuery  = "count(vmware_vm_power_state{job=\"vmware_vcenter\", power_state=\"suspended\"})";

        // ❗ Prometheus 값이 비어 있거나 잘못된 경우 → 0으로 처리
        int total      = queryIntValueOrDefault(totalQuery, 0);
        int poweredOn  = queryIntValueOrDefault(poweredOnQuery, 0);
        int poweredOff = queryIntValueOrDefault(poweredOffQuery, 0);
        int suspended  = queryIntValueOrDefault(suspendedQuery, 0);

        int unknown = total - poweredOn - poweredOff - suspended;
        if (unknown < 0) {
            log.warn("Calculated unknown VM count is negative. total={}, on={}, off={}, susp={} → unknown={}",
                    total, poweredOn, poweredOff, suspended, unknown);
            unknown = 0;
        }

        log.debug("vCenter summary from Prometheus → total={}, on={}, off={}, susp={}, unknown={}",
                total, poweredOn, poweredOff, suspended, unknown);

        return VcenterSummaryResponse.builder()
                .totalVms(total)
                .poweredOn(poweredOn)
                .poweredOff(poweredOff)
                .suspended(suspended)
                .unknown(unknown)
                .build();
    }

    // =========================================================
    // 🔹 4) Datastore / 기타 vector용 헬퍼
    // =========================================================
    public List<PrometheusQueryResponse.Result> queryVector(String promQl) {
        try {
            String url = prometheusBaseUrl + "/api/v1/query?query={q}";
            log.info("Calling Prometheus(vector): {}", promQl);

            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<PrometheusQueryResponse> response =
                    restTemplate.getForEntity(url, PrometheusQueryResponse.class, promQl);

            PrometheusQueryResponse body = response.getBody();
            if (body == null || body.getData() == null || body.getData().getResult() == null) {
                log.warn("Prometheus vector response is null or invalid. query={}", promQl);
                return List.of();
            }

            return body.getData().getResult();
        } catch (Exception e) {
            log.warn("Failed to query Prometheus vector. query={}", promQl, e);
            return List.of();
        }
    }

// =========================================================
// 🔹 5) Datastore 사용량 조회 (FE: /monitoring/prometheus/datastores)
// =========================================================
    /**
     * names 리스트(HDD1 (1), NVME (1) 등)에 대해
     * - capacityBytes
     * - freeBytes
     * - usedBytes
     * - usedPercent
     * 를 채운 DatastoreUsageDto 목록을 반환.
     *
     * ❗ metric 이름/라벨은 현재 Prometheus 화면 기준:
     *   - vmware_datastore_capacity_size{job="vmware_vcenter", ds_name="HDD1 (1)"}
     *   - vmware_datastore_freespace_size{job="vmware_vcenter", ds_name="HDD1 (1)"}
     */
    public List<DatastoreUsageDto> getDatastoreUsage(List<String> names) {
        if (names == null || names.isEmpty()) {
            return List.of();
        }

        List<DatastoreUsageDto> result = new ArrayList<>();

        for (String rawName : names) {
            if (rawName == null) continue;

            String dsName = rawName.trim();
            if (dsName.isEmpty()) continue;

            try {
                // ⚠️ 여기서 Prometheus 메트릭 이름을
                //     _capacity_size / _freespace_size 로 맞춰준 것에 주목!
                String capacityQuery = String.format(
                        "vmware_datastore_capacity_size{job=\"vmware_vcenter\", ds_name=\"%s\"}",
                        dsName
                );
                String freeQuery = String.format(
                        "vmware_datastore_freespace_size{job=\"vmware_vcenter\", ds_name=\"%s\"}",
                        dsName
                );

                Double capacityVal = querySingleValue(capacityQuery);
                Double freeVal = querySingleValue(freeQuery);

                double capacityBytes = capacityVal != null ? capacityVal : 0d;
                double freeBytes = freeVal != null ? freeVal : 0d;

                // 🔹 사용량(바이트) = capacity - free (음수면 0으로 보정)
                double usedBytes = Math.max(0d, capacityBytes - freeBytes);

                // 🔹 사용률(%) - 팀원이 준 식과 동일한 의미:
                //   100 - 100 * free / capacity
                double usedPercent = (capacityBytes > 0d)
                        ? (100.0 - 100.0 * freeBytes / capacityBytes)
                        : 0d;

                DatastoreUsageDto dto = DatastoreUsageDto.builder()
                        .dsName(dsName)
                        .capacityBytes(capacityBytes)
                        .freeBytes(freeBytes)
                        .usedBytes(usedBytes)
                        .usedPercent(usedPercent)
                        .build();

                result.add(dto);

            } catch (Exception e) {
                // 개별 ds 실패는 로그만 남기고 다음으로 진행
                log.warn("Failed to build DatastoreUsageDto for dsName={}", dsName, e);
            }
        }

        return result;
    }

}
