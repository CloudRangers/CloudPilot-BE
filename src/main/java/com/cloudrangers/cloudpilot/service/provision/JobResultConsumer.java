package com.cloudrangers.cloudpilot.service.provision;

import com.cloudrangers.cloudpilot.domain.provision.VmProvisionJob;
import com.cloudrangers.cloudpilot.enums.VmProvisionStatus;
import com.cloudrangers.cloudpilot.dto.response.ProvisionResponse;
import com.cloudrangers.cloudpilot.repository.provision.ProvisionJobRepository;
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
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobResultConsumer {

    private final ProvisionJobRepository provisionJobRepository;

    @RabbitListener(queues = "${rabbitmq.queue.result.name:provision-results}")
    @Transactional
    public void consumeResult(
            @Payload ProvisionResponse result,
            Message amqpMessage,
            @Headers Map<String, Object> headers
    ) {
        final String corr = extractCorrelationId(amqpMessage, headers);
        final String jobIdStr = firstNonBlank(
                corr,
                asString(headers.get("jobId")),
                result.getJobId()
        ).orElse(null);

        if (jobIdStr == null) {
            log.error("Result dropped: missing jobId/correlationId. headers={}, payload={}",
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

        log.info("Result received: jobId={}, corr={}, status={}, vmId={}",
                jobId, corr, result.getStatus(), result.getVmResourceId());

        try {
            VmProvisionJob job = provisionJobRepository.findById(jobId).orElse(null);
            if (job == null) {
                log.warn("Job not found for result. jobId={}, headers={}",
                        jobId, safeHeaderPreview(headers));
                return;
            }

            switch (String.valueOf(result.getStatus())) {
                case "RUNNING" -> handleRunningStatus(job, result);
                case "SUCCEEDED" -> handleSuccessStatus(job, result);
                case "FAILED" -> handleFailedStatus(job, result);
                default -> log.warn("Unexpected status for job {}: {}", jobId, result.getStatus());
            }

            provisionJobRepository.save(job);
        } catch (Exception e) {
            log.error("Failed to process result for job: {}", jobIdStr, e);
            throw new RuntimeException("Failed to process result", e);
        }
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

    private Optional<String> firstNonBlank(String... vals) {
        for (String v : vals) if (v != null && !v.isBlank()) return Optional.of(v);
        return Optional.empty();
    }

    private void handleRunningStatus(VmProvisionJob job, ProvisionResponse result) {
        job.setStatus(VmProvisionStatus.running);
        if (result.getStartedAt() != null) job.setStartedAt(result.getStartedAt());
        else if (job.getStartedAt() == null) job.setStartedAt(Instant.now());
        log.info("Job started: {}", job.getId());
    }

    private void handleSuccessStatus(VmProvisionJob job, ProvisionResponse result) {
        // vmResourceId는 DDL 상 job 테이블에 저장 공간 없음 → 필요하면 별도 테이블에 저장 고려
        job.setStatus(VmProvisionStatus.succeeded);
        if (result.getCompletedAt() != null) job.setFinishedAt(result.getCompletedAt());
        else job.setFinishedAt(Instant.now());
        log.info("Job succeeded: {}, VM ID: {}", job.getId(), result.getVmResourceId());
    }

    private void handleFailedStatus(VmProvisionJob job, ProvisionResponse result) {
        final String err = (result.getErrorMessage() == null || result.getErrorMessage().isBlank())
                ? "Worker reported failure (no message)" : result.getErrorMessage();
        job.setStatus(VmProvisionStatus.failed);
        job.setErrorMessage(err);
        job.setFinishedAt(Instant.now());
        log.error("Job failed: {}, Error: {}", job.getId(), err);
    }

    private String safeHeaderPreview(Map<String, Object> headers) {
        try {
            return "{correlation_id=" + headers.get("correlation_id")
                    + ", jobId=" + headers.get("jobId") + "}";
        } catch (Exception e) { return "{preview-failed}"; }
    }

    private String safePayloadPreview(ProvisionResponse p) {
        try {
            return "ProvisionResponse{jobId=" + p.getJobId() + ", status=" + p.getStatus()
                    + ", vmResourceId=" + p.getVmResourceId() + "}";
        } catch (Exception e) { return "{payload-preview-failed}"; }
    }
}
