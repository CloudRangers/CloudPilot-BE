package com.cloudrangers.cloudpilot.controller;

import com.cloudrangers.cloudpilot.service.provision.ProvisionService;
import com.cloudrangers.cloudpilot.dto.request.ProvisionRequest;
import com.cloudrangers.cloudpilot.dto.response.ProvisionResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * VM 프로비저닝 API (단일/다중 통합)
 */
@RestController
@RequestMapping("/provision")
@RequiredArgsConstructor
@Slf4j
public class ProvisionController {

    private final ProvisionService provisionService;

    /**
     * VM 프로비저닝 요청
     * POST /provision
     */
    @PostMapping
    public ResponseEntity<ProvisionResponse> createProvisionJob(
            @Valid @RequestBody ProvisionRequest request,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-Team-Id") Long teamId
    ) {
        int vmCount = request.getVmCount() != null ? request.getVmCount() : 1;

        log.info("Received provision request: user={}, team={}, vmCount={}, vmName={}",
                userId, teamId, vmCount, request.getVmName());

        ProvisionResponse response = provisionService.createProvisionJob(request, userId, teamId);

        return ResponseEntity.accepted().body(response);
    }
}