package com.cloudrangers.cloudpilot.service.provision;

import com.cloudrangers.cloudpilot.domain.pipeline.TfRun;
import com.cloudrangers.cloudpilot.domain.provision.VmProvisionJob;
import com.cloudrangers.cloudpilot.dto.message.ProvisionResultMessage;
import com.cloudrangers.cloudpilot.enums.TfRunStatus;
import com.cloudrangers.cloudpilot.enums.VmProvisionStatus;
import com.cloudrangers.cloudpilot.repository.pipeline.TfRunRepository;
import com.cloudrangers.cloudpilot.repository.provision.ProvisionJobRepository;
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
    private final TfRunRepository tfRunRepository;  // ✅ 추가

    /**
     * 워커가 result-exchange -> provision-results 로 보내는 메시지 처리
     * - LOG : 테라폼 로그를 API 서버 로그에 남기고, Job 상태를 running 으로 전환
     * - SUCCESS : Job 상태 succeeded + vm_instance 저장 (생성) 또는 삭제 완료 (destroy)
     * - ERROR : Job 상태 failed
     */
    @RabbitListener(queues = "${rabbitmq.queue.result.name:provision-results}")
    @Transactional
    public void consumeResult(
            @Payload ProvisionResultMessage result,
            Message amqpMessage,
            @Headers Map<String, Object> headers
    ) {
        final String corr = extractCorrelationId(amqpMessage, headers);
        final String jobIdStr = firstNonBlank(
                result.getJobId(),
                asString(headers.get("jobId")),
                corr
        ).orElse(null);

        if (jobIdStr == null) {
            log.error("Result dropped: jobId/correlationId 없음. headers={}, payload={}",
                    safeHeaderPreview(headers), safePayloadPreview(result));
            return;
        }

        // ✅ tfRunId 추출
        Long tfRunId = extractTfRunId(result, headers);

        Long jobId;
        try {
            jobId = Long.parseLong(jobIdStr);
        } catch (NumberFormatException nfe) {
            log.error("Invalid jobId format (expected Long): {}", jobIdStr);
            return;
        }

        log.info("[Result] jobId={}, tfRunId={}, corr={}, eventType={}, status={}, step={}, msg={}",
                jobId,
                tfRunId,
                corr,
                result.getEventType(),
                result.getStatus(),
                result.getStep(),
                truncate(result.getMessage(), 200)
        );

        try {
            VmProvisionJob job = provisionJobRepository.findById(jobId).orElse(null);
            if (job == null) {
                log.warn("Job not found for result. jobId={}, headers={}",
                        jobId, safeHeaderPreview(headers));
                return;
            }

            // 첫 이벤트가 오면 queued → running 으로 변경
            if (job.getStatus() == VmProvisionStatus.queued) {
                job.setStatus(VmProvisionStatus.running);
                if (job.getStartedAt() == null) {
                    if (result.getTimestamp() != null) {
                        job.setStartedAt(result.getTimestamp().toInstant());
                    } else {
                        job.setStartedAt(Instant.now());
                    }
                }
            }

            ProvisionResultMessage.EventType eventType = resolveEventType(result);

            switch (eventType) {
                case LOG    -> handleLogEvent(job, result);
                case SUCCESS -> handleSuccessEvent(job, result, tfRunId);  // ✅ tfRunId 전달
                case ERROR   -> handleErrorEvent(job, result, tfRunId);    // ✅ tfRunId 전달
                default      -> log.warn("Unknown eventType for job {}: {} (status={})",
                        jobId, eventType, result.getStatus());
            }

            provisionJobRepository.save(job);
        } catch (Exception e) {
            log.error("Failed to process result for job: {}", jobIdStr, e);
            throw new RuntimeException("Failed to process result", e);
        }
    }

    /**
     * 워커에서 AmqpRejectAndDontRequeueException 던져서
     * 원본 job 메시지가 DLQ(provision-jobs.dlq)로 간 경우 처리.
     */
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

        // 이미 성공이면 건들지 않음
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
        // 필요하면 나중에 별도 로그 테이블에 적재하는 로직 추가 가능
    }

    /**
     * ⭐ 수정: VM 생성/삭제 구분 처리 + TfRun 업데이트
     */
    private void handleSuccessEvent(VmProvisionJob job, ProvisionResultMessage result, Long tfRunId) {
        job.setStatus(VmProvisionStatus.succeeded);
        if (result.getTimestamp() != null) {
            job.setFinishedAt(result.getTimestamp().toInstant());
        } else if (job.getFinishedAt() == null) {
            job.setFinishedAt(Instant.now());
        }

        // ✅ TfRun 업데이트
        updateTfRunSuccess(tfRunId, result);

        // ⭐ destroy 이벤트인지 확인
        boolean isDestroy = isDestroyEvent(result);

        if (isDestroy) {
            // VM 삭제 처리
            handleDestroySuccess(job, result);
        } else {
            // VM 생성 처리
            vmProvisionService.handleProvisionSuccess(job, result);
            int count = result.getInstances() != null ? result.getInstances().size() : 0;
            log.info("VM creation succeeded: jobId={}, tfRunId={}, instances={}",
                    job.getId(), tfRunId, count);
        }
    }

    /**
     * ⭐ 수정: VM 생성/삭제 실패 구분 처리 + TfRun 업데이트
     */
    private void handleErrorEvent(VmProvisionJob job, ProvisionResultMessage result, Long tfRunId) {
        final String err = (result.getMessage() == null || result.getMessage().isBlank())
                ? "Worker reported failure (no message)"
                : result.getMessage();

        job.setStatus(VmProvisionStatus.failed);
        job.setErrorMessage(err);
        if (job.getFinishedAt() == null) {
            job.setFinishedAt(Instant.now());
        }

        // ✅ TfRun 업데이트
        updateTfRunFailure(tfRunId, err);

        // ⭐ destroy 이벤트인지 확인
        boolean isDestroy = isDestroyEvent(result);

        if (isDestroy) {
            // VM 삭제 실패 처리
            handleDestroyError(job, result, err);
        } else {
            // VM 생성 실패 처리
            log.error("VM creation failed: jobId={}, tfRunId={}, error={}",
                    job.getId(), tfRunId, err);
        }
    }

    // ====== ⭐ 새로 추가: TfRun 업데이트 로직 ======

    /**
     * TfRun 성공 처리
     */
    private void updateTfRunSuccess(Long tfRunId, ProvisionResultMessage result) {
        if (tfRunId == null) {
            log.warn("⚠️ No tfRunId in result message - cannot update TfRun");
            return;
        }

        try {
            TfRun tfRun = tfRunRepository.findById(tfRunId).orElse(null);

            if (tfRun == null) {
                log.warn("⚠️ TfRun not found: tfRunId={}", tfRunId);
                return;
            }

            tfRun.setStatus(TfRunStatus.succeeded);
            tfRun.setFinishedAt(Instant.now());

            // ✅ state_uri 저장 (Worker가 보낸 경로)
            if (result.getStateUri() != null && !result.getStateUri().isBlank()) {
                tfRun.setStateUri(result.getStateUri());
                log.info("✓ State URI saved: tfRunId={}, stateUri={}",
                        tfRunId, result.getStateUri());
            }

            tfRunRepository.save(tfRun);

            log.info("✓ TfRun updated: id={}, status=succeeded, stateUri={}",
                    tfRunId, tfRun.getStateUri());

        } catch (Exception e) {
            log.error("❌ Failed to update TfRun success: tfRunId={}", tfRunId, e);
        }
    }

    /**
     * TfRun 실패 처리
     */
    private void updateTfRunFailure(Long tfRunId, String errorMessage) {
        if (tfRunId == null) {
            log.warn("⚠️ No tfRunId in result message - cannot update TfRun");
            return;
        }

        try {
            TfRun tfRun = tfRunRepository.findById(tfRunId).orElse(null);

            if (tfRun == null) {
                log.warn("⚠️ TfRun not found: tfRunId={}", tfRunId);
                return;
            }

            tfRun.setStatus(TfRunStatus.failed);
            tfRun.setFinishedAt(Instant.now());

            tfRunRepository.save(tfRun);

            log.error("✗ TfRun updated: id={}, status=failed, error={}",
                    tfRunId, truncate(errorMessage, 100));

        } catch (Exception e) {
            log.error("❌ Failed to update TfRun failure: tfRunId={}", tfRunId, e);
        }
    }

    /**
     * tfRunId 추출
     */
    private Long extractTfRunId(ProvisionResultMessage result, Map<String, Object> headers) {
        // 1. result 메시지에서 직접 추출
        if (result.getTfRunId() != null) {
            return result.getTfRunId();
        }

        // 2. headers에서 추출
        Object tfRunIdObj = headers.get("tfRunId");
        if (tfRunIdObj != null) {
            try {
                return Long.parseLong(String.valueOf(tfRunIdObj));
            } catch (NumberFormatException e) {
                log.debug("tfRunId is not a number: {}", tfRunIdObj);
            }
        }

        return null;
    }

    // ====== ⭐ VM 삭제 처리 로직 ======

    /**
     * destroy 이벤트인지 확인
     * - step에 "destroy" 포함
     * - message에 "destroy" 포함
     */
    private boolean isDestroyEvent(ProvisionResultMessage result) {
        String step = result.getStep();
        String message = result.getMessage();

        if (step != null && step.toLowerCase().contains("destroy")) {
            return true;
        }

        if (message != null && message.toLowerCase().contains("destroy")) {
            return true;
        }

        // terraform_apply는 생성으로 간주
        if (step != null && step.equals("terraform_apply")) {
            return false;
        }

        return false;
    }

    /**
     * VM 삭제 성공 처리
     */
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

    /**
     * VM 삭제 실패 처리
     */
    private void handleDestroyError(VmProvisionJob job, ProvisionResultMessage result, String errorMessage) {
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

    /**
     * ProvisionResultMessage에서 vmId 추출
     */
    private Long extractVmId(ProvisionResultMessage result) {
        // 1. vmId 필드에서 직접 추출
        String vmIdStr = result.getVmId();
        if (vmIdStr != null && !vmIdStr.isBlank()) {
            try {
                return Long.parseLong(vmIdStr);
            } catch (NumberFormatException e) {
                log.debug("vmId is not a number: {}", vmIdStr);
            }
        }

        // 2. instances에서 추출
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
                    + ", message=" + truncate(p.getMessage(), 100) + "}";
        } catch (Exception e) {
            return "{payload-preview-failed}";
        }
    }
}