package com.cloudrangers.cloudpilot.service.provision;

import com.cloudrangers.cloudpilot.domain.provision.VmProvisionJob;
import com.cloudrangers.cloudpilot.dto.message.ProvisionResultMessage;
import com.cloudrangers.cloudpilot.enums.VmProvisionStatus;
import com.cloudrangers.cloudpilot.repository.provision.ProvisionJobRepository;
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

    /**
     * 워커가 result-exchange -> provision-results 로 보내는 메시지 처리
     * - LOG : 테라폼 로그를 API 서버 로그에 남기고, Job 상태를 running 으로 전환
     * - SUCCESS : Job 상태 succeeded + vm_instance 저장
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

        Long jobId;
        try {
            jobId = Long.parseLong(jobIdStr);
        } catch (NumberFormatException nfe) {
            log.error("Invalid jobId format (expected Long): {}", jobIdStr);
            return;
        }

        log.info("[Result] jobId={}, corr={}, eventType={}, status={}, step={}, msg={}",
                jobId,
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
                case SUCCESS -> handleSuccessEvent(job, result);
                case ERROR   -> handleErrorEvent(job, result);
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

    private void handleSuccessEvent(VmProvisionJob job, ProvisionResultMessage result) {
        job.setStatus(VmProvisionStatus.succeeded);
        if (result.getTimestamp() != null) {
            job.setFinishedAt(result.getTimestamp().toInstant());
        } else if (job.getFinishedAt() == null) {
            job.setFinishedAt(Instant.now());
        }

        // vm_instance 저장
        vmProvisionService.handleProvisionSuccess(job, result);

        int count = result.getInstances() != null ? result.getInstances().size() : 0;
        log.info("Job succeeded: jobId={}, instances={}", job.getId(), count);
    }

    private void handleErrorEvent(VmProvisionJob job, ProvisionResultMessage result) {
        final String err = (result.getMessage() == null || result.getMessage().isBlank())
                ? "Worker reported failure (no message)"
                : result.getMessage();

        job.setStatus(VmProvisionStatus.failed);
        job.setErrorMessage(err);
        if (job.getFinishedAt() == null) {
            job.setFinishedAt(Instant.now());
        }

        log.error("Job failed: jobId={}, error={}", job.getId(), err);
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
                    + ", jobId=" + headers.get("jobId") + "}";
        } catch (Exception e) {
            return "{preview-failed}";
        }
    }

    private String safePayloadPreview(ProvisionResultMessage p) {
        try {
            return "ProvisionResultMessage{jobId=" + p.getJobId()
                    + ", eventType=" + p.getEventType()
                    + ", status=" + p.getStatus()
                    + ", step=" + p.getStep()
                    + ", message=" + truncate(p.getMessage(), 100) + "}";
        } catch (Exception e) {
            return "{payload-preview-failed}";
        }
    }
}
