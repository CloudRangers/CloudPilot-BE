package com.cloudrangers.cloudpilot.controller;

import com.cloudrangers.cloudpilot.service.provision.ProvisionService;
import com.cloudrangers.cloudpilot.dto.request.ProvisionRequest;
import com.cloudrangers.cloudpilot.dto.response.ProvisionResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/provision")
@RequiredArgsConstructor
@Slf4j
public class ProvisionController {

    private final ProvisionService provisionService;

    /**
     * VM 프로비저닝 요청
     * POST /api/v1/provision
     */
    @PostMapping
    public ResponseEntity<ProvisionResponse> createProvisionJob(
            @Valid @RequestBody ProvisionRequest request,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-Team-Id") Long teamId
    ) {
        log.info("Received provision request from user: {}, team: {}", userId, teamId);

        ProvisionResponse response = provisionService.createProvisionJob(request, userId, teamId);

        return ResponseEntity.accepted().body(response);
    }

    /**
     * Job 상태 조회
     * GET /api/v1/provision/{jobId}
     */
    @GetMapping("/{jobId}")
    public ResponseEntity<ProvisionResponse> getJobStatus(@PathVariable String jobId) {
        log.info("Getting status for job: {}", jobId);

        ProvisionResponse response = provisionService.getJobStatus(jobId);

        return ResponseEntity.ok(response);
    }

    /**
     * 팀의 모든 프로비저닝 Job 조회
     * GET /api/v1/provision/team/{teamId}
     */
    @GetMapping("/team/{teamId}")
    public ResponseEntity<List<ProvisionResponse>> getTeamJobs(@PathVariable Long teamId) {
        log.info("Getting provision jobs for team: {}", teamId);

        List<ProvisionResponse> jobs = provisionService.getTeamJobs(teamId);

        return ResponseEntity.ok(jobs);
    }

    /**
     * Job 재시도
     * POST /api/v1/provision/{jobId}/retry
     */
    @PostMapping("/{jobId}/retry")
    public ResponseEntity<Void> retryJob(@PathVariable String jobId) {
        log.info("Retrying job: {}", jobId);

        provisionService.retryJob(jobId);

        return ResponseEntity.accepted().build();
    }

    /**
     * Job 취소 (TODO)
     * POST /api/v1/provision/{jobId}/cancel
     */
    @PostMapping("/{jobId}/cancel")
    public ResponseEntity<Void> cancelJob(@PathVariable String jobId) {
        log.info("Cancelling job: {}", jobId);

        // TODO: 구현

        return ResponseEntity.accepted().build();
    }
}
