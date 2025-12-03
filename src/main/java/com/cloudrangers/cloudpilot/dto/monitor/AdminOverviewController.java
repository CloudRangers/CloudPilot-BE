// src/main/java/com/cloudrangers/cloudpilot/controller/monitor/AdminOverviewController.java
package com.cloudrangers.cloudpilot.dto.monitor;

import com.cloudrangers.cloudpilot.common.ApiResponse;
import com.cloudrangers.cloudpilot.dto.monitor.AdminOverviewResponse;
import com.cloudrangers.cloudpilot.service.monitor.AdminOverviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/monitor/overview")
public class AdminOverviewController {

    private final AdminOverviewService adminOverviewService;

    @GetMapping
    public ApiResponse<AdminOverviewResponse> getOverview() {
        AdminOverviewResponse data = adminOverviewService.getOverview();
        return ApiResponse.success(data);
    }
}
