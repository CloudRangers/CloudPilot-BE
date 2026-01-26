package com.cloudrangers.cloudpilot.ops.controller;

import com.cloudrangers.cloudpilot.common.ApiResponse;
import com.cloudrangers.cloudpilot.ops.dto.MetricAggregation;
import com.cloudrangers.cloudpilot.ops.dto.RechartsDataResponse;
import com.cloudrangers.cloudpilot.ops.dto.VmMetricResponse;
import com.cloudrangers.cloudpilot.ops.service.VmMetricService;
import com.cloudrangers.cloudpilot.security.AuthUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * VM 단위 메트릭 조회용 컨트롤러.
 *
 * vmId 는 현재 node-exporter instance 값(예: 172.16.5.112:9100) 이라고 가정.
 *
 * 지원하는 metricName (논리 이름) 예:
 *  - vm_cpu_usage_percent
 *  - vm_memory_usage_percent
 */
@RestController
@RequestMapping("/ops/v1/vms")
@RequiredArgsConstructor
public class VmMetricController {

    private final VmMetricService vmMetricService;

    /**
     * 단일 메트릭 시계열 조회 (raw TimeSeriesPoint)
     *
     * 예)
     * GET /ops/v1/vms/{vmId}/metrics
     *   ?metricName=vm_cpu_usage_percent&rangeMinutes=60&stepSeconds=60
     */
    @GetMapping("/{vmId}/metrics")
    public ApiResponse<VmMetricResponse> getVmMetrics(
            @PathVariable String vmId,
            @RequestParam String metricName,
            @RequestParam(required = false, defaultValue = "60") int rangeMinutes,
            @RequestParam(required = false, defaultValue = "60") int stepSeconds
    ) {
        checkAccess(vmId);

        VmMetricResponse result = vmMetricService.getVmMetrics(
                vmId,
                metricName,
                rangeMinutes,
                stepSeconds
        );

        return ApiResponse.success(result);
    }

    /**
     * 메트릭 집계 정보 조회 (평균, 최대, 최소, 현재값)
     *
     * 예)
     * GET /ops/v1/vms/{vmId}/metrics/aggregation
     *   ?metricName=vm_cpu_usage_percent&rangeMinutes=60
     */
    @GetMapping("/{vmId}/metrics/aggregation")
    public ApiResponse<MetricAggregation> getMetricAggregation(
            @PathVariable String vmId,
            @RequestParam String metricName,
            @RequestParam(required = false, defaultValue = "60") int rangeMinutes
    ) {
        checkAccess(vmId);

        MetricAggregation result = vmMetricService.getMetricAggregation(
                vmId,
                metricName,
                rangeMinutes
        );

        return ApiResponse.success(result);
    }

    /**
     * Recharts 용 데이터 포맷 반환
     *
     * 예)
     * GET /ops/v1/vms/{vmId}/metrics/recharts
     *   ?metricName=vm_cpu_usage_percent&rangeMinutes=60&stepSeconds=60
     */
    @GetMapping("/{vmId}/metrics/recharts")
    public ApiResponse<RechartsDataResponse> getMetricsForRecharts(
            @PathVariable String vmId,
            @RequestParam String metricName,
            @RequestParam(required = false, defaultValue = "60") int rangeMinutes,
            @RequestParam(required = false, defaultValue = "60") int stepSeconds
    ) {
        checkAccess(vmId);

        RechartsDataResponse result = vmMetricService.getMetricsForRecharts(
                vmId,
                metricName,
                rangeMinutes,
                stepSeconds
        );

        return ApiResponse.success(result);
    }

    /**
     * 🔐 권한 체크 (간단 버전)
     * - ADMIN / HEAD : 전체 VM 접근 허용
     * - LEADER       : TODO - teamId 기준으로 본인 팀 VM만 허용
     * - MEMBER       : TODO - userId 기준으로 본인 소유 VM만 허용
     */
    private void checkAccess(String vmId) {
        String role = AuthUtil.getRole();
        Long teamId = AuthUtil.getTeamId();
        Long userId = AuthUtil.getUserId();

        // 🔥 디버깅용: 권한 완전 오픈
        // 나중에 다시 롤 체크 넣으면 됨
        if (true) {
            return;
        }

        if (role == null) {
            throw new AccessDeniedException("인증되지 않은 사용자입니다.");
        }

        switch (role) {
            case "ADMIN":
            case "HEAD":
                // 전체 VM 접근 허용
                break;

            case "LEADER":
                // TODO: vmId 가 teamId 소속 팀의 VM 인지 확인
                break;

            case "MEMBER":
                // TODO: vmId 가 userId(또는 empno) 소유 VM 인지 확인
                break;

            default:
                throw new AccessDeniedException("알 수 없는 권한입니다: " + role);
        }
    }
}
