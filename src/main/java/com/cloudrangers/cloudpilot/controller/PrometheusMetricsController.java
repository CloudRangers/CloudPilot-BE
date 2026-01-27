// src/main/java/com/cloudrangers/cloudpilot/monitor/prometheus/web/PrometheusMetricsController.java
package com.cloudrangers.cloudpilot.controller;

import com.cloudrangers.cloudpilot.common.ApiResponse;
import com.cloudrangers.cloudpilot.dto.DatastoreUsageDto;
import com.cloudrangers.cloudpilot.service.PrometheusMetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/monitor/prometheus")
public class PrometheusMetricsController {

    private final PrometheusMetricsService prometheusMetricsService;

    /**
     * 🔹 Datastore 용량/사용률 조회 API
     *  - 프론트: GET /monitor/prometheus/datastores?names=HDD1 (1)&names=NVME (1)
     */
    @GetMapping("/datastores")
    public ApiResponse<Map<String, DatastoreUsageDto>> getDatastores(
            @RequestParam("names") List<String> names
    ) {
        log.info("[PrometheusMetricsController] getDatastores names={}", names);

        Map<String, DatastoreUsageDto> data = prometheusMetricsService.getDatastoreUsage(names);

        // 프로젝트에서 쓰는 ApiResponse 형태에 맞게 수정해서 사용하면 돼
        return ApiResponse.success(data);
    }
}
