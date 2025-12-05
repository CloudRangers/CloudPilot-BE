package com.cloudrangers.cloudpilot.service.provision;

import com.cloudrangers.cloudpilot.domain.pipeline.TfRun;
import com.cloudrangers.cloudpilot.domain.provision.VmProvisionItem;
import com.cloudrangers.cloudpilot.domain.provision.VmProvisionJob;
import com.cloudrangers.cloudpilot.dto.message.ProvisionResultMessage;
import com.cloudrangers.cloudpilot.dto.message.ProvisionProgressMessage;
import com.cloudrangers.cloudpilot.enums.TfRunAction;
import com.cloudrangers.cloudpilot.enums.TfRunStatus;
import com.cloudrangers.cloudpilot.enums.VmProvisionStatus;
import com.cloudrangers.cloudpilot.repository.pipeline.TfRunRepository;
import com.cloudrangers.cloudpilot.repository.provision.ProvisionJobRepository;
import com.cloudrangers.cloudpilot.repository.provision.VmProvisionItemRepository;
import com.cloudrangers.cloudpilot.service.vm.VmDeleteService;
import com.cloudrangers.cloudpilot.service.vm.VmProvisionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobResultConsumer {

    private final ProvisionJobRepository provisionJobRepository;
    private final VmProvisionService vmProvisionService;
    private final VmDeleteService vmDeleteService;
    private final TfRunRepository tfRunRepository;
    private final VmProvisionItemRepository vmProvisionItemRepository;
    private final ProvisionSseService sseService;

    @RabbitListener(queues = "${rabbitmq.queue.result.name:provision-results}")
    @Transactional
    public void consumeResult(
            @Payload ProvisionResultMessage result,
            Message amqpMessage,
            @Headers Map<String, Object> headers
    ) {
        log.info("========================================");
        log.info("[RESULT DEBUG] Raw Message Received");
        log.info("  - payload.jobId: {}", result.getJobId());
        log.info("  - payload.tfRunId: {}", result.getTfRunId());
        log.info("  - payload.stateUri: {}", result.getStateUri());
        log.info("  - payload.step: {}", result.getStep());
        log.info("  - payload.status: {}", result.getStatus());
        log.info("  - payload.eventType: {}", result.getEventType());
        log.info("========================================");

        final String corr = extractCorrelationId(amqpMessage, headers);

        final String jobIdStr = firstNonBlank(
                result.getJobId(),
                asString(headers.get("jobId")),
                corr
        ).orElse(null);

        if (jobIdStr == null) {
            log.error("Result dropped: jobId/correlationId 없음.");
            return;
        }

        Long jobId;
        try {
            jobId = Long.parseLong(jobIdStr);
        } catch (NumberFormatException nfe) {
            log.error("Invalid jobId format: {}", jobIdStr);
            return;
        }

        Long tfRunId = extractTfRunId(result, headers);
        if (tfRunId == null) {
            tfRunId = lookupTfRunIdFromDatabase(jobId);
            if (tfRunId != null) {
                log.info("✓ tfRunId resolved from database: jobId={}, tfRunId={}", jobId, tfRunId);
                result.setTfRunId(tfRunId);
            }
        }

        if (tfRunId != null && result.getTfRunId() == null) {
            result.setTfRunId(tfRunId);
        }

        ProvisionResultMessage.EventType eventType = resolveEventType(result);

        log.info("[Result] jobId={}, tfRunId={}, corr={}, eventType={}, status={}, step={}, msg={}, stateUri={}",
                jobId, tfRunId, corr, eventType, result.getStatus(), result.getStep(),
                truncate(result.getMessage(), 200), result.getStateUri());

        updateTfRunFromResult(result, tfRunId, eventType);

        try {
            VmProvisionJob job = provisionJobRepository.findById(jobId).orElse(null);
            if (job == null) {
                log.warn("Job not found: jobId={}", jobId);
                return;
            }

            if (job.getStatus() == VmProvisionStatus.queued) {
                job.setStatus(VmProvisionStatus.running);
                if (job.getStartedAt() == null) {
                    job.setStartedAt(result.getTimestamp() != null
                            ? result.getTimestamp().toInstant()
                            : Instant.now());
                }
            }

            switch (eventType) {
                case LOG -> handleLogEvent(job, result);
                case SUCCESS -> handleSuccessEvent(job, result, tfRunId);
                case ERROR -> handleErrorEvent(job, result, tfRunId);
            }

            provisionJobRepository.save(job);
        } catch (Exception e) {
            log.error("Failed to process result for job: {}", jobIdStr, e);
            throw new RuntimeException("Failed to process result", e);
        }
    }

    @RabbitListener(queues = "${rabbitmq.queue.dlq.name:provision-jobs.dlq}")
    @Transactional
    public void consumeDeadLetter(
            @Payload byte[] body,
            Message amqpMessage,
            @Headers Map<String, Object> headers
    ) {
        String payload = new String(body, StandardCharsets.UTF_8);
        final String jobIdStr = firstNonBlank(
                asString(headers.get("jobId")),
                extractCorrelationId(amqpMessage, headers)
        ).orElse(null);

        log.error("[DLQ] Dead-lettered job message 수신. jobId={}, headers={}, payload={}",
                jobIdStr, safeHeaderPreview(headers), truncate(payload, 500));

        if (jobIdStr == null) {
            return;
        }

        Long jobId;
        try {
            jobId = Long.parseLong(jobIdStr);
        } catch (NumberFormatException e) {
            log.error("[DLQ] Invalid jobId format (expected Long): {}", jobIdStr);
            return;
        }

        VmProvisionJob job = provisionJobRepository.findById(jobId).orElse(null);
        if (job == null) {
            log.warn("[DLQ] Job not found. jobId={}", jobId);
            return;
        }

        if (job.getStatus() != VmProvisionStatus.succeeded) {
            job.setStatus(VmProvisionStatus.failed);
            if (job.getErrorMessage() == null || job.getErrorMessage().isBlank()) {
                job.setErrorMessage("Job message moved to DLQ. payload=" + truncate(payload, 300));
            }
            if (job.getFinishedAt() == null) {
                job.setFinishedAt(Instant.now());
            }
            provisionJobRepository.save(job);

            sseService.sendError(String.valueOf(jobId),
                    "작업이 실패했습니다. " + truncate(payload, 200));
        }
    }

    // ====== 내부 핸들러 ======

    private ProvisionResultMessage.EventType resolveEventType(ProvisionResultMessage result) {
        if (result.getEventType() != null) {
            return result.getEventType();
        }
        String status = result.getStatus();
        if (status == null) {
            return ProvisionResultMessage.EventType.LOG;
        }
        String up = status.toUpperCase(Locale.ROOT);
        return switch (up) {
            case "RUNNING", "LOG" -> ProvisionResultMessage.EventType.LOG;
            case "SUCCEEDED", "SUCCESS" -> ProvisionResultMessage.EventType.SUCCESS;
            case "FAILED", "ERROR" -> ProvisionResultMessage.EventType.ERROR;
            default -> ProvisionResultMessage.EventType.LOG;
        };
    }

    private void handleLogEvent(VmProvisionJob job, ProvisionResultMessage result) {
        String step = result.getStep() != null ? result.getStep() : "unknown";
        String line = result.getMessage();
        log.info("[Job:{}][TF-{}] {}", job.getId(), step, line);

        // ⭐ Ansible 최종 완료 LOG 를 "진짜 완료"로 처리
        if (isFinalAnsibleCompletionLog(line)) {
            log.info("[Job:{}] Detected final Ansible completion log. Marking job as SUCCEEDED.", job.getId());

            job.setStatus(VmProvisionStatus.succeeded);
            if (result.getTimestamp() != null) {
                job.setFinishedAt(result.getTimestamp().toInstant());
            } else if (job.getFinishedAt() == null) {
                job.setFinishedAt(Instant.now());
            }

            // 최종 COMPLETE SSE 전송
            sendCompleteToSSE(job, result);
            return; // 더 이상 progress 이벤트는 보내지 않음
        }

        // 일반 LOG 는 계속 progress 로 전송
        sendProgressToSSE(job, result, "RUNNING");
    }

    // ⭐ Ansible 쪽에서 보내는 최종 완료 메시지 패턴 감지
    private boolean isFinalAnsibleCompletionLog(String line) {
        if (line == null) return false;
        String lower = line.toLowerCase(Locale.ROOT);

        // 예시:
        // "Ansible provisioning completed for IP: 172.16.5.108"
        // "✅ [Ansible] Provisioning Completed Successfully for IP: 172.16.5.108"
        // "[Worker:worker-01] ✓ Job completed. jobId=216, IP=172.16.5.108"
        return lower.contains("ansible provisioning completed")
                || lower.contains("provisioning completed successfully for ip")
                || lower.contains("✓ job completed".toLowerCase(Locale.ROOT));
    }

    private void handleSuccessEvent(VmProvisionJob job,
                                    ProvisionResultMessage result,
                                    Long tfRunId) {

        // ⭐ step 확인
        String step = result.getStep() != null ? result.getStep().toLowerCase() : "";

        log.info("=== SUCCESS Event Received ===");
        log.info("  - jobId: {}", job.getId());
        log.info("  - step: {}", step);
        log.info("  - message: {}", result.getMessage());

        // Destroy 이벤트 처리
        boolean isDestroy = isDestroyEvent(result, tfRunId);
        if (isDestroy) {
            job.setStatus(VmProvisionStatus.succeeded);
            if (result.getTimestamp() != null) {
                job.setFinishedAt(result.getTimestamp().toInstant());
            } else if (job.getFinishedAt() == null) {
                job.setFinishedAt(Instant.now());
            }
            handleDestroySuccess(job, result);
            return;
        }


        if (step.contains("terraform") || step.contains("apply")) {
            // Terraform만 완료 → Ansible 대기
            log.info("Terraform apply completed, waiting for Ansible...");

            // status는 running 유지 (아직 완료 아님)
            job.setStatus(VmProvisionStatus.running);

            // VM 정보는 저장 (handleProvisionSuccess에서 VM 레코드 생성)
            vmProvisionService.handleProvisionSuccess(job, result);

            int count = result.getInstances() != null ? result.getInstances().size() : 0;
            log.info("VM creation succeeded: jobId={}, tfRunId={}, instances={}",
                    job.getId(), tfRunId, count);

            // SSE로 "Terraform 완료, Ansible 대기" 메시지 전송
            ProvisionProgressMessage progressMsg = ProvisionProgressMessage.builder()
                    .jobId(String.valueOf(job.getId()))
                    .stage("TERRAFORM_COMPLETE")
                    .description("VM 생성 완료 - 패키지 설치 준비 중...")
                    .progress(80)
                    .vmIpAddress(extractVmIpAddress(result))
                    .status("RUNNING")
                    .logLine("Terraform 작업 완료, Ansible 실행 예정")
                    .build();

            sseService.sendProgress(progressMsg);

        } else {
            // (이 경우는 혹시 Worker 쪽에서 SUCCESS 로 Ansible 완료를 보내는 경우 대비)
            log.info("All provisioning completed (final step: {})", step);

            job.setStatus(VmProvisionStatus.succeeded);
            if (result.getTimestamp() != null) {
                job.setFinishedAt(result.getTimestamp().toInstant());
            } else if (job.getFinishedAt() == null) {
                job.setFinishedAt(Instant.now());
            }

            if (result.getInstances() != null && !result.getInstances().isEmpty()) {
                vmProvisionService.handleProvisionSuccess(job, result);
            }

            sendCompleteToSSE(job, result);
        }
    }

    private void handleErrorEvent(VmProvisionJob job,
                                  ProvisionResultMessage result,
                                  Long tfRunId) {
        final String err = (result.getMessage() == null || result.getMessage().isBlank())
                ? "Worker reported failure (no message)"
                : result.getMessage();

        job.setStatus(VmProvisionStatus.failed);
        job.setErrorMessage(err);
        if (job.getFinishedAt() == null) {
            job.setFinishedAt(Instant.now());
        }

        boolean isDestroy = isDestroyEvent(result, tfRunId);

        if (isDestroy) {
            handleDestroyError(job, result, err);
        } else {
            log.error("VM creation failed: jobId={}, tfRunId={}, error={}",
                    job.getId(), tfRunId, err);

            sseService.sendError(String.valueOf(job.getId()), err);
        }
    }

    // ====== SSE 전송 헬퍼 메서드 ======

    private void sendProgressToSSE(VmProvisionJob job,
                                   ProvisionResultMessage result,
                                   String status) {
        try {
            ProgressInfo progressInfo = parseProgress(result, job);

            ProvisionProgressMessage message = ProvisionProgressMessage.builder()
                    .jobId(String.valueOf(job.getId()))
                    .stage(progressInfo.stage)
                    .description(progressInfo.description)
                    .progress(progressInfo.progress)
                    .elapsedSeconds(progressInfo.elapsedSeconds)
                    .vmIpAddress(extractVmIpAddress(result))
                    .status(status)
                    .logLine(result.getMessage())
                    .build();

            sseService.sendProgress(message);

        } catch (Exception e) {
            log.error("Failed to send SSE progress: jobId={}", job.getId(), e);
        }
    }

    private void sendCompleteToSSE(VmProvisionJob job, ProvisionResultMessage result) {
        try {
            String vmIp = extractVmIpAddress(result);

            ProvisionProgressMessage message = ProvisionProgressMessage.builder()
                    .jobId(String.valueOf(job.getId()))
                    .stage("COMPLETE")
                    .description("VM 생성 및 설정 완료!")
                    .progress(100)
                    .vmIpAddress(vmIp)
                    .status("SUCCEEDED")
                    .build();

            sseService.sendComplete(message);

        } catch (Exception e) {
            log.error("Failed to send SSE complete: jobId={}", job.getId(), e);
        }
    }

    // ====== 로그 파싱 ======

    private ProgressInfo parseProgress(ProvisionResultMessage result, VmProvisionJob job) {
        String step = result.getStep() != null ? result.getStep().toLowerCase() : "";
        String message = result.getMessage() != null ? result.getMessage() : "";

        ProgressInfo info = new ProgressInfo();

        if (step.contains("init") || message.contains("Initializing")) {
            info.stage = "TERRAFORM_INIT";
            info.description = "Terraform 초기화 중";
            info.progress = 5;

            if (message.contains("successfully initialized")) {
                info.progress = 10;
                info.description = "초기화 완료";
            }
        }
        else if (step.contains("validate") || message.contains("valid")) {
            info.stage = "TERRAFORM_VALIDATE";
            info.description = "구성 검증 중";
            info.progress = 15;
        }
        else if (step.contains("plan") || message.contains("Plan:")) {
            info.stage = "TERRAFORM_PLAN";
            info.description = "실행 계획 수립 중";
            info.progress = 20;

            if (message.contains("Plan:")) {
                info.progress = 25;
                info.description = "계획 수립 완료";
            }
        }
        else if (step.contains("apply") || message.contains("Creating") || message.contains("Still creating")) {
            info.stage = "TERRAFORM_APPLY";

            if (message.contains("Creating...") || message.contains("Creating (")) {
                info.progress = 30;
                info.description = "VM 생성 시작";
            }
            else if (message.contains("Still creating")) {
                int elapsed = parseElapsedSeconds(message);
                info.elapsedSeconds = elapsed;

                if (elapsed < 60) {
                    info.progress = 35;
                } else if (elapsed < 120) {
                    info.progress = 45;
                } else if (elapsed < 180) {
                    info.progress = 55;
                } else {
                    info.progress = 65;
                }

                int mins = elapsed / 60;
                int secs = elapsed % 60;
                info.description = String.format("⏳ VM 생성 중 (%d분 %d초 경과)", mins, secs);
            }
            else if (message.contains("Creation complete") || message.contains("VM Created")) {
                info.progress = 75;
                info.description = "VM 생성 완료";
            }
            else if (message.contains("Apply complete")) {
                info.progress = 80;
                info.description = "Terraform 작업 완료";
            }
        }
        else if (message.contains("PLAY [")) {
            info.stage = "ANSIBLE_START";
            info.description = "⚙설정 시작";
            info.progress = 82;
        }
        else if (message.contains("TASK [Wait for SSH]")) {
            info.stage = "ANSIBLE_SSH";
            info.description = "SSH 연결 대기 중";
            info.progress = 85;
        }
        else if (message.contains("TASK [common :")) {
            info.stage = "ANSIBLE_COMMON";
            info.description = "기본 패키지 설치 중";
            info.progress = 88;
        }
        else if (message.contains("TASK [node_exporter :")) {
            info.stage = "ANSIBLE_NODE_EXPORTER";
            info.description = "모니터링 설치 중";
            info.progress = 92;
        }
        else if (message.contains("TASK [monitoring_server :")) {
            info.stage = "ANSIBLE_MONITORING";
            info.description = "모니터링 등록 중";
            info.progress = 97;
        }
        else if (message.contains("PLAY RECAP")) {
            info.stage = "ANSIBLE_COMPLETE";
            info.description = "Ansible 설정 완료";
            info.progress = 99;
        }

        return info;
    }

    /**
     * "Still creating (02m30s)" 또는 "[02m30s elapsed]" 파싱
     */
    private int parseElapsedSeconds(String message) {
        // 패턴 1: "(02m30s)"
        Pattern pattern1 = Pattern.compile("\\((\\d+)m(\\d+)s\\)");
        Matcher matcher1 = pattern1.matcher(message);

        if (matcher1.find()) {
            int minutes = Integer.parseInt(matcher1.group(1));
            int seconds = Integer.parseInt(matcher1.group(2));
            int total = minutes * 60 + seconds;
            log.debug("Parsed elapsed time: {}m {}s = {}s total", minutes, seconds, total);
            return total;
        }

        // 패턴 2: "[02m30s elapsed]"
        Pattern pattern2 = Pattern.compile("\\[(\\d+)m(\\d+)s elapsed\\]");
        Matcher matcher2 = pattern2.matcher(message);

        if (matcher2.find()) {
            int minutes = Integer.parseInt(matcher2.group(1));
            int seconds = Integer.parseInt(matcher2.group(2));
            int total = minutes * 60 + seconds;
            log.debug("Parsed elapsed time: {}m {}s = {}s total", minutes, seconds, total);
            return total;
        }

        log.debug("Could not parse elapsed time from: {}", message);
        return 0;
    }

    private String extractVmIpAddress(ProvisionResultMessage result) {
        if (result.getInstances() != null && !result.getInstances().isEmpty()) {
            ProvisionResultMessage.InstanceInfo first = result.getInstances().get(0);
            if (first.getIpAddress() != null && !first.getIpAddress().isBlank()) {
                return first.getIpAddress();
            }
        }

        String message = result.getMessage();
        if (message != null) {
            // 1) terraform output: ip_address = "172.16.5.108"
            Pattern pattern1 = Pattern.compile("ip_address\\s*=\\s*\"([0-9.]+)\"");
            Matcher matcher1 = pattern1.matcher(message);
            if (matcher1.find()) {
                return matcher1.group(1);
            }

            Pattern pattern2 = Pattern.compile("IP[:=]\\s*([0-9.]+)");
            Matcher matcher2 = pattern2.matcher(message);
            if (matcher2.find()) {
                return matcher2.group(1);
            }
        }

        return null;
    }

    private static class ProgressInfo {
        String stage = "RUNNING";
        String description = "처리 중...";
        Integer progress = null;
        Integer elapsedSeconds = 0;
    }

    // ====== TfRun 업데이트 ======

    private void updateTfRunFromResult(ProvisionResultMessage result,
                                       Long tfRunId,
                                       ProvisionResultMessage.EventType eventType) {
        log.info("========================================");
        log.info("[UPDATE TF_RUN] Method Called");
        log.info("  - tfRunId: {}", tfRunId);
        log.info("  - eventType: {}", eventType);
        log.info("  - result.stateUri: {}", result.getStateUri());
        log.info("  - result.step: {}", result.getStep());
        log.info("========================================");

        if (tfRunId == null) {
            log.warn("tfRunId is NULL - SKIPPING tf_run update");
            return;
        }

        Optional<TfRun> optional = tfRunRepository.findById(tfRunId);
        if (optional.isEmpty()) {
            log.warn("tf_run NOT FOUND for tfRunId={}", tfRunId);
            return;
        }

        TfRun tfRun = optional.get();
        log.info("✓ Found TfRun: id={}, currentStateUri={}, status={}",
                tfRun.getId(), tfRun.getStateUri(), tfRun.getStatus());

        tfRun.setUpdatedAt(Instant.now());
        boolean changed = false;

        String step = result.getStep() != null ? result.getStep() : "";

        switch (eventType) {
            case SUCCESS -> {
                log.info("→ Processing SUCCESS event");

                if (result.getStateUri() != null && !result.getStateUri().isBlank()) {
                    log.info("  ✓ Setting stateUri: {}", result.getStateUri());
                    tfRun.setStateUri(result.getStateUri());
                    changed = true;
                } else {
                    log.warn("stateUri is NULL or BLANK in result");
                }

                if (step.toLowerCase(Locale.ROOT).contains("apply")
                        || step.toLowerCase(Locale.ROOT).contains("destroy")) {
                    log.info("Setting status to SUCCEEDED");
                    tfRun.setStatus(TfRunStatus.succeeded);
                    tfRun.setFinishedAt(result.getTimestamp() != null
                            ? result.getTimestamp().toInstant()
                            : Instant.now());
                    changed = true;
                }
            }
            case ERROR -> {
                log.info("→ Processing ERROR event");
                tfRun.setStatus(TfRunStatus.failed);
                tfRun.setFinishedAt(Instant.now());
                changed = true;
                log.error("✗ TfRun marked as failed: tfRunId={}, error={}",
                        tfRunId, truncate(result.getMessage(), 150));
            }
            case LOG -> {
                log.info("→ Processing LOG event");
                if (result.getStateUri() != null && !result.getStateUri().isBlank()
                        && tfRun.getStateUri() == null) {
                    log.info("  ✓ Setting stateUri from LOG event: {}", result.getStateUri());
                    tfRun.setStateUri(result.getStateUri());
                    changed = true;
                }
            }
        }

        if (changed) {
            log.info("Saving tf_run: id={}, newStateUri={}, status={}",
                    tfRun.getId(), tfRun.getStateUri(), tfRun.getStatus());
            tfRunRepository.save(tfRun);
            log.info("tf_run saved successfully");
        } else {
            log.warn("No changes to save for tf_run: {}", tfRun.getId());
        }
    }

    // ====== DB 조회 ======

    private Long lookupTfRunIdFromDatabase(Long jobId) {
        try {
            List<VmProvisionItem> items = vmProvisionItemRepository.findAll();
            for (VmProvisionItem item : items) {
                if (item.getTfRunId() != null) {
                    log.info("Found tfRunId via VmProvisionItem: itemId={}, tfRunId={}",
                            item.getId(), item.getTfRunId());
                    return item.getTfRunId();
                }
            }

            List<TfRun> recentRuns = tfRunRepository.findTop5ByOrderByIdDesc();
            for (TfRun run : recentRuns) {
                if (run.getAction() == TfRunAction.apply
                        && run.getStateUri() == null
                        && run.getStartedAt() != null
                        && run.getStartedAt().isAfter(Instant.now().minusSeconds(300))) {
                    log.info("✓ Found recent TfRun (fallback): tfRunId={}", run.getId());
                    return run.getId();
                }
            }

            log.warn("Could not lookup tfRunId from database for jobId={}", jobId);
            return null;

        } catch (Exception e) {
            log.error("Failed to lookup tfRunId from database: jobId={}", jobId, e);
            return null;
        }
    }

    // ====== Destroy 처리 ======

    private boolean isDestroyEvent(ProvisionResultMessage result, Long tfRunId) {
        if (tfRunId != null) {
            TfRun tfRun = tfRunRepository.findById(tfRunId).orElse(null);
            if (tfRun != null && tfRun.getAction() != null) {
                if (tfRun.getAction() == TfRunAction.destroy) {
                    return true;
                } else if (tfRun.getAction() == TfRunAction.apply) {
                    return false;
                }
            }
        }

        String step = result.getStep();
        if (step != null && step.toLowerCase(Locale.ROOT).contains("destroy")) {
            return true;
        }

        return false;
    }

    private void handleDestroySuccess(VmProvisionJob job, ProvisionResultMessage result) {
        try {
            Long vmId = extractVmId(result);

            if (vmId != null) {
                vmDeleteService.markAsDeleted(vmId);
                log.info("VM deletion succeeded: jobId={}, vmId={}", job.getId(), vmId);
            } else {
                log.warn("Cannot extract vmId from destroy success: jobId={}", job.getId());
            }
        } catch (Exception e) {
            log.error("Failed to mark VM as deleted: jobId={}", job.getId(), e);
        }
    }

    private void handleDestroyError(VmProvisionJob job,
                                    ProvisionResultMessage result,
                                    String errorMessage) {
        try {
            Long vmId = extractVmId(result);

            if (vmId != null) {
                vmDeleteService.markDeletionFailed(vmId, errorMessage);
                log.error("VM deletion failed: jobId={}, vmId={}, error={}",
                        job.getId(), vmId, errorMessage);
            } else {
                log.warn("Cannot extract vmId from destroy error: jobId={}", job.getId());
            }
        } catch (Exception e) {
            log.error("Failed to handle deletion error: jobId={}", job.getId(), e);
        }
    }

    private Long extractVmId(ProvisionResultMessage result) {
        String vmIdStr = result.getVmId();
        if (vmIdStr != null && !vmIdStr.isBlank()) {
            try {
                return Long.parseLong(vmIdStr);
            } catch (NumberFormatException e) {
                log.debug("vmId is not a number: {}", vmIdStr);
            }
        }

        if (result.getInstances() != null && !result.getInstances().isEmpty()) {
            ProvisionResultMessage.InstanceInfo first = result.getInstances().get(0);
            String externalId = first.getExternalId();

            if (externalId != null && !externalId.isBlank()) {
                try {
                    return Long.parseLong(externalId);
                } catch (NumberFormatException e) {
                    log.debug("externalId is not a number: {}", externalId);
                }
            }
        }

        return null;
    }

    // ====== 유틸리티 ======

    private Long extractTfRunId(ProvisionResultMessage result, Map<String, Object> headers) {
        if (result.getTfRunId() != null) {
            return result.getTfRunId();
        }

        for (String key : new String[]{"tfRunId", "tf_run_id", "TfRunId", "TFRUNID"}) {
            Object tfRunIdObj = headers.get(key);
            if (tfRunIdObj != null) {
                try {
                    if (tfRunIdObj instanceof Number n) {
                        return n.longValue();
                    }
                    return Long.parseLong(String.valueOf(tfRunIdObj));
                } catch (NumberFormatException e) {
                    log.debug("tfRunId is not a number in header '{}': {}", key, tfRunIdObj);
                }
            }
        }

        return null;
    }

    private String extractCorrelationId(Message m, Map<String, Object> headers) {
        Object h = headers.get("correlation_id");
        String v = asString(h);
        if (v != null && !v.isBlank()) return v;

        Object cid = m.getMessageProperties().getCorrelationId();
        if (cid instanceof byte[] b) return new String(b, StandardCharsets.UTF_8);
        if (cid != null) return String.valueOf(cid);

        Object alt = headers.get("amqp_correlationId");
        return asString(alt);
    }

    private String asString(Object o) {
        if (o == null) return null;
        if (o instanceof byte[] b) return new String(b, StandardCharsets.UTF_8);
        return String.valueOf(o);
    }

    private Optional<String> firstNonBlank(String... values) {
        if (values == null) return Optional.empty();
        for (String v : values) {
            if (v != null && !v.isBlank()) return Optional.of(v);
        }
        return Optional.empty();
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        if (s.length() <= max) return s;
        return s.substring(0, max) + "...";
    }

    private String safeHeaderPreview(Map<String, Object> headers) {
        try {
            return "{correlation_id=" + headers.get("correlation_id")
                    + ", jobId=" + headers.get("jobId")
                    + ", tfRunId=" + headers.get("tfRunId") + "}";
        } catch (Exception e) {
            return "{preview-failed}";
        }
    }
}
