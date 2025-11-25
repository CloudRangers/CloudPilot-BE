package com.cloudrangers.cloudpilot.controller.ops;

import com.cloudrangers.cloudpilot.common.ApiResponse;
import com.cloudrangers.cloudpilot.dto.ops.AnomalyDetectionRequest;
import com.cloudrangers.cloudpilot.dto.ops.AnomalyDetectionResult;
import com.cloudrangers.cloudpilot.service.ops.AnomalyDetectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/ops/v1/anomaly-detection")
@RequiredArgsConstructor
public class AnomalyDetectionController {

    private final AnomalyDetectionService anomalyDetectionService;

    /**
     * 🔹 AI 이상징후 감지 엔드포인트 (AI 모델은 나중에 연동)
     *
     * POST /ops/v1/anomaly-detection
     * body: AnomalyDetectionRequest
     */
    @PostMapping
    public ApiResponse<AnomalyDetectionResult> detectAnomaly(
            @RequestBody AnomalyDetectionRequest request
    ) {
        log.info("[AnomalyDetection] 요청 수신: vmId={}", request.getVmId());
        AnomalyDetectionResult result = anomalyDetectionService.analyze(request);
        return ApiResponse.success(result);
    }
}
