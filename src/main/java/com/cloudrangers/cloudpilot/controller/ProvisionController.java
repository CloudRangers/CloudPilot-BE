package com.cloudrangers.cloudpilot.controller;

import com.cloudrangers.cloudpilot.dto.request.ProvisionRequest;
import com.cloudrangers.cloudpilot.dto.response.ProvisionResponse;
import com.cloudrangers.cloudpilot.security.AuthUtil;
import com.cloudrangers.cloudpilot.service.provision.ProvisionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

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
            @Valid @RequestBody ProvisionRequest request
    ) {
        Long userId = AuthUtil.getUserId();
        Long callerTeamId = AuthUtil.getTeamId();

        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증이 필요합니다.");
        }

        int vmCount = request.getVmCountOrDefault();

        log.info("Received provision request: user={}, callerTeam={}, requestTeam={}, vmCount={}, vmName={}",
                userId, callerTeamId, request.getTeamId(), vmCount, request.getVmName());

        ProvisionResponse response =
                provisionService.createProvisionJob(request, userId, callerTeamId);

        return ResponseEntity.accepted().body(response);
    }

}
