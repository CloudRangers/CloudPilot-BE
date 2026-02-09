// src/main/java/com/cloudrangers/cloudpilot/controller/monitoring/PrometheusMonitoringController.java
package com.cloudrangers.cloudpilot.controller.monitoring;

import com.cloudrangers.cloudpilot.common.ApiResponse;
import com.cloudrangers.cloudpilot.dto.monitoring.PrometheusSummaryResponse;
import com.cloudrangers.cloudpilot.dto.DatastoreUsageDto;
import com.cloudrangers.cloudpilot.dto.VcenterSummaryResponse;
import com.cloudrangers.cloudpilot.service.monitoring.PrometheusMonitoringService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/monitoring/prometheus")   // ⬅︎ 지금 네 네트워크 캡처 URL에 맞춤
@RequiredArgsConstructor
public class PrometheusMonitoringController {

    private final PrometheusMonitoringService prometheusMonitoringService;

    /**
     * 1) up() 메트릭 요약
     *  - totalTargets / upTargets / downTargets
     *  - 실패하더라도 success=true + 0 값으로 내려보냄
     */
    @GetMapping("/summary")
    public ApiResponse<PrometheusSummaryResponse> getUpSummary() {
        try {
            PrometheusSummaryResponse summary = prometheusMonitoringService.getUpSummary();
            // service 쪽에서도 예외는 잡고 0,0,0을 리턴하도록 이미 처리해 둔 상태
            return ApiResponse.success(summary);
        } catch (Exception e) {
            log.error("getUpSummary controller error", e);
            // 🔸 여기서도 절대 Exception 밖으로 던지지 않음
            return ApiResponse.success(new PrometheusSummaryResponse(0, 0, 0));
        }
    }

    /**
     * 2) vCenter VM 요약 (상단 카드용)
     *   필요 시 /monitor/vcenter/summary 를 쓰고 있다면 이 엔드포인트는 안 써도 됨.
     *   혹시 쓰고 있다면, 마찬가지로 실패해도 success=true + 0 값으로 반환.
     */
    @GetMapping("/vcenter-summary")
    public ApiResponse<VcenterSummaryResponse> getVcenterSummary() {
        try {
            VcenterSummaryResponse summary = prometheusMonitoringService.getVcenterVmSummaryFromPrometheus();
            return ApiResponse.success(summary);
        } catch (Exception e) {
            log.error("getVcenterSummary controller error", e);
            VcenterSummaryResponse empty = VcenterSummaryResponse.builder()
                    .totalVms(0)
                    .poweredOn(0)
                    .poweredOff(0)
                    .suspended(0)
                    .unknown(0)
                    .build();
            return ApiResponse.success(empty);
        }
    }

    /**
     * 3) Datastore 사용량 요약
     *   - ?names[]=HDD1+(1)&names[]=NVME+(1) 형태로 들어온다고 가정
     *   - 실패해도 success=true + 빈 리스트
     */
    @GetMapping("/datastores")
    public ApiResponse<List<DatastoreUsageDto>> getDatastores(
            @RequestParam(name = "names") List<String> names
    ) {
        try {
            List<DatastoreUsageDto> usages = prometheusMonitoringService.getDatastoreUsage(names);
            return ApiResponse.success(usages);
        } catch (Exception e) {
            log.error("getDatastores controller error", e);
            // 🔸 실패해도 GlobalExceptionHandler까지 안 가고 여기서 마무리
            return ApiResponse.success(Collections.emptyList());
        }
    }
}
