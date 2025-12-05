package com.cloudrangers.cloudpilot.controller.monitoring;

import com.cloudrangers.cloudpilot.common.ApiResponse;
import com.cloudrangers.cloudpilot.dto.monitoring.AdminOverviewDto;
import com.cloudrangers.cloudpilot.service.monitoring.AdminOverviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/monitor")
@RequiredArgsConstructor
public class AdminOverviewController {

    private final AdminOverviewService adminOverviewService;

    @GetMapping("/overview")
    public ApiResponse<AdminOverviewDto> getOverview() {

        log.info("[AdminOverviewController] GET /api/monitor/overview called");

        // ⭐ 여기서 먼저 overview를 만들고
        AdminOverviewDto overview = adminOverviewService.getOverview();

        // ⭐ 그 다음에 로그로 찍어야 함
        log.info("[AdminOverviewController] overview response = {}", overview);

        return ApiResponse.success(overview);
    }
}
