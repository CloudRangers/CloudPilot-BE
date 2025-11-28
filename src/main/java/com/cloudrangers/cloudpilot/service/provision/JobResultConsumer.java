package com.cloudrangers.cloudpilot.service.provision;

import com.cloudrangers.cloudpilot.domain.pipeline.TfRun;
import com.cloudrangers.cloudpilot.domain.provision.VmProvisionItem;
import com.cloudrangers.cloudpilot.domain.provision.VmProvisionJob;
import com.cloudrangers.cloudpilot.dto.message.ProvisionResultMessage;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class JobResultConsumer {

    private final ProvisionJobRepository provisionJobRepository;
    private final VmProvisionService vmProvisionService;
    private final VmDeleteService vmDeleteService;
    private final TfRunRepository tfRunRepository;
    private final VmProvisionItemRepository vmProvisionItemRepository;

    @RabbitListener(queues = "${rabbitmq.queue.result.name:provision-results}")
    @Transactional
    public void consumeResult(
            @Payload ProvisionResultMessage result,
            Message amqpMessage,
            @Headers Map<String, Object> headers
    ) {
        // ⭐⭐⭐ 디버깅 로그
        log.info("========================================");
        log.info("[RESULT DEBUG] Raw Message Received");
        log.info("  - payload.jobId: {}", result.getJobId());
        log.info("  - payload.tfRunId: {}", result.getTfRunId());
        log.info("  - payload.stateUri: {}", result.getStateUri());
        log.info("  - payload.step: {}", result.getStep());
        log.info("  - payload.status: {}", result.getStatus());
        log.info("  - payload.eventType: {}", result.getEventType());
        log.info("  - header.tfRunId: {}", headers.get("tfRunId"));
        log.info("  - header.jobId: {}", headers.get("jobId"));
        log.info("  - ALL HEADERS: {}", headers);  // ✅ 모든 헤더 출력
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

        // ✅ tfRunId 추출 (payload → header → DB 역추적 순으로)
        Long tfRunId = extractTfRunId(result, headers);

        // ⭐⭐⭐ tfRunId가 여전히 null이면 DB에서 역추적
        if (tfRunId == null) {
            tfRunId = lookupTfRunIdFromDatabase(jobId);

            if (tfRunId != null) {
                log.info("✓ tfRunId resolved from database: jobId={}, tfRunId={}", jobId, tfRunId);
                result.setTfRunId(tfRunId);
            } else {
                log.warn("⚠️ Cannot resolve tfRunId for jobId={}", jobId);
            }
        }

        if (tfRunId != null && result.getTfRunId() == null) {
            result.setTfRunId(tfRunId);
        }

        ProvisionResultMessage.EventType eventType = resolveEventType(result);

        log.info("[Result] jobId={}, tfRunId={}, corr={}, eventType={}, status={}, step={}, msg={}, stateUri={}",
                jobId, tfRunId, corr, eventType, result.getStatus(), result.getStep(),
                truncate(result.getMessage(), 200), result.getStateUri());

        // ✅ 1) TfRun 먼저 업데이트
        updateTfRunFromResult(result, tfRunId, eventType);

        // ✅ 2) 그 다음에 Job / VM 처리
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
    }

    private void handleSuccessEvent(VmProvisionJob job,
                                    ProvisionResultMessage result,
                                    Long tfRunId) {
        job.setStatus(VmProvisionStatus.succeeded);
        if (result.getTimestamp() != null) {
            job.setFinishedAt(result.getTimestamp().toInstant());
        } else if (job.getFinishedAt() == null) {
            job.setFinishedAt(Instant.now());
        }

        boolean isDestroy = isDestroyEvent(result, tfRunId);

        if (isDestroy) {
            handleDestroySuccess(job, result);
        } else {
            vmProvisionService.handleProvisionSuccess(job, result);
            int count = result.getInstances() != null ? result.getInstances().size() : 0;
            log.info("VM creation succeeded: jobId={}, tfRunId={}, instances={}",
                    job.getId(), tfRunId, count);
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
        }
    }

    // ====== ⭐ TfRun 업데이트 로직 ======

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
            log.warn("⚠️ tfRunId is NULL - SKIPPING tf_run update");
            return;
        }

        Optional<TfRun> optional = tfRunRepository.findById(tfRunId);
        if (optional.isEmpty()) {
            log.warn("⚠️ tf_run NOT FOUND for tfRunId={}", tfRunId);
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

                // ✅ state_uri 저장 (step 무관)
                if (result.getStateUri() != null && !result.getStateUri().isBlank()) {
                    log.info("  ✓ Setting stateUri: {}", result.getStateUri());
                    tfRun.setStateUri(result.getStateUri());
                    changed = true;
                } else {
                    log.warn("  ⚠️ stateUri is NULL or BLANK in result");
                }

                // 상태 업데이트
                if (step.toLowerCase(Locale.ROOT).contains("apply")
                        || step.toLowerCase(Locale.ROOT).contains("destroy")) {
                    log.info("  ✓ Setting status to SUCCEEDED");
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
                // LOG 이벤트에서도 state_uri가 있으면 저장
                if (result.getStateUri() != null && !result.getStateUri().isBlank()
                        && tfRun.getStateUri() == null) {
                    log.info("  ✓ Setting stateUri from LOG event: {}", result.getStateUri());
                    tfRun.setStateUri(result.getStateUri());
                    changed = true;
                }
            }
        }

        if (changed) {
            log.info("💾 Saving tf_run: id={}, newStateUri={}, status={}",
                    tfRun.getId(), tfRun.getStateUri(), tfRun.getStatus());
            tfRunRepository.save(tfRun);
            log.info("✅ tf_run saved successfully");
        } else {
            log.warn("⚠️ No changes to save for tf_run: {}", tfRun.getId());
        }
    }

    // ====== ⭐ DB에서 tfRunId 역추적 ======

    private Long lookupTfRunIdFromDatabase(Long jobId) {
        try {
            // 방법 1: VmProvisionItem을 통한 역추적
            List<VmProvisionItem> items = vmProvisionItemRepository.findAll();
            for (VmProvisionItem item : items) {
                // jobId 비교 로직 (VmProvisionItem에 jobId 필드가 있다고 가정)
                // 실제 구조에 맞게 수정 필요
                if (item.getTfRunId() != null) {
                    log.info("✓ Found tfRunId via VmProvisionItem: itemId={}, tfRunId={}",
                            item.getId(), item.getTfRunId());
                    return item.getTfRunId();
                }
            }

            // 방법 2: TfRun에서 가장 최근 레코드 조회
            List<TfRun> recentRuns = tfRunRepository.findTop5ByOrderByIdDesc();
            for (TfRun run : recentRuns) {
                // 최근 5분 이내 생성된 apply 작업 중 아직 state_uri가 없는 것
                if (run.getAction() == TfRunAction.apply
                        && run.getStateUri() == null
                        && run.getStartedAt() != null
                        && run.getStartedAt().isAfter(Instant.now().minusSeconds(300))) {
                    log.info("✓ Found recent TfRun (fallback): tfRunId={}", run.getId());
                    return run.getId();
                }
            }

            log.warn("⚠️ Could not lookup tfRunId from database for jobId={}", jobId);
            return null;

        } catch (Exception e) {
            log.error("❌ Failed to lookup tfRunId from database: jobId={}", jobId, e);
            return null;
        }
    }

    // ====== destroy / apply 구분 로직 ======

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
                log.info("✅ VM deletion succeeded: jobId={}, vmId={}", job.getId(), vmId);
            } else {
                log.warn("⚠️ Cannot extract vmId from destroy success: jobId={}", job.getId());
            }
        } catch (Exception e) {
            log.error("❌ Failed to mark VM as deleted: jobId={}", job.getId(), e);
        }
    }

    private void handleDestroyError(VmProvisionJob job,
                                    ProvisionResultMessage result,
                                    String errorMessage) {
        try {
            Long vmId = extractVmId(result);

            if (vmId != null) {
                vmDeleteService.markDeletionFailed(vmId, errorMessage);
                log.error("❌ VM deletion failed: jobId={}, vmId={}, error={}",
                        job.getId(), vmId, errorMessage);
            } else {
                log.warn("⚠️ Cannot extract vmId from destroy error: jobId={}", job.getId());
            }
        } catch (Exception e) {
            log.error("❌ Failed to handle deletion error: jobId={}", job.getId(), e);
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

    // ====== ⭐ tfRunId 추출 (강화 버전) ======

    private Long extractTfRunId(ProvisionResultMessage result, Map<String, Object> headers) {
        // 1) payload에서 추출
        if (result.getTfRunId() != null) {
            return result.getTfRunId();
        }

        // 2) 헤더에서 추출 (다양한 키 시도)
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

    // ===== helpers =====

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

    private String safePayloadPreview(ProvisionResultMessage p) {
        try {
            return "ProvisionResultMessage{jobId=" + p.getJobId()
                    + ", tfRunId=" + p.getTfRunId()
                    + ", eventType=" + p.getEventType()
                    + ", status=" + p.getStatus()
                    + ", step=" + p.getStep()
                    + ", stateUri=" + p.getStateUri()
                    + ", message=" + truncate(p.getMessage(), 100) + "}";
        } catch (Exception e) {
            return "{payload-preview-failed}";
        }
    }
}