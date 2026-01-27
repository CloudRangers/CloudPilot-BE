package com.cloudrangers.cloudpilot.controller;

import com.cloudrangers.cloudpilot.common.ApiResponse;
import com.cloudrangers.cloudpilot.dto.monitoring.AdminOverviewDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.cloudrangers.cloudpilot.service.monitoring.AdminOverviewService;


@RestController
@RequestMapping("/monitor")
@RequiredArgsConstructor
public class AdminMonitorController {

    private final AdminOverviewService adminOverviewService;

    @GetMapping("/overview")
    public ApiResponse<AdminOverviewDto> getOverview() {
        AdminOverviewDto dto = adminOverviewService.getOverview();
        return ApiResponse.success(dto);
    }
}

