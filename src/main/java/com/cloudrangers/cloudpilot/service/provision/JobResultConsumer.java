package com.cloudrangers.cloudpilot.service.provision;

import com.cloudrangers.cloudpilot.domain.provision.ProvisionJob;
import com.cloudrangers.cloudpilot.dto.response.ProvisionResponse;
import com.cloudrangers.cloudpilot.enums.ProvisionStatus;
import com.cloudrangers.cloudpilot.repository.provision.ProvisionJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Worker로부터 실행 결과를 수신하는 Consumer (RabbitMQ)
 * - 결과 DTO는 ProvisionResponse만 사용
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JobResultConsumer {

    private final ProvisionJobRepository provisionJobRepository;

    /**
     * RabbitMQ에서 결과 메시지 수신
     * Jackson2JsonMessageConverter 사용 시 JSON → ProvisionResponse 매핑됨
     */
    @RabbitListener(queues = "${rabbitmq.queue.result.name:provision-results}")
    @Transactional
    public void consumeResult(ProvisionResponse result) {
        final String jobId = result.getJobId();

        log.info("Received result for job: {}, status: {}", jobId, result.getStatus());

        try {
            ProvisionJob job = provisionJobRepository.findByJobId(jobId)
                    .orElseThrow(() -> new RuntimeException("Job not found: " + jobId));

            // 상태별 처리
            if (result.getStatus() == ProvisionStatus.RUNNING) {
                handleRunningStatus(job, result);
            } else if (result.getStatus() == ProvisionStatus.SUCCEEDED) {
                handleSuccessStatus(job, result);
            } else if (result.getStatus() == ProvisionStatus.FAILED) {
                handleFailedStatus(job, result);
            } else {
                log.warn("Unexpected status for job {}: {}", jobId, result.getStatus());
            }

            provisionJobRepository.save(job);

        } catch (Exception e) {
            log.error("Failed to process result for job: {}", jobId, e);
            // RabbitMQ 재시도를 위해 예외 전파
            throw new RuntimeException("Failed to process result", e);
        }
    }

    /**
     * RUNNING 상태 처리
     */
    private void handleRunningStatus(ProvisionJob job, ProvisionResponse result) {
        job.updateStatus(ProvisionStatus.RUNNING);
        if (result.getStartedAt() != null) {
            job.updateStartedAt(result.getStartedAt());
        }
        log.info("Job started: {}", job.getJobId());
    }

    /**
     * SUCCEEDED 상태 처리
     * - ProvisionResponse에 포함된 vmResourceId만 사용
     * - (기존 provisionOutput 등은 사용하지 않음: DTO 단일화 정책)
     */
    private void handleSuccessStatus(ProvisionJob job, ProvisionResponse result) {
        final String vmId = result.getVmResourceId();

        if (vmId == null || vmId.isBlank()) {
            log.warn("SUCCEEDED인데 vmResourceId가 없음: jobId={}", job.getJobId());
        }

        // 도메인 메서드로 성공 처리 (output은 현재 정책상 null)
        job.markAsSucceeded(vmId, null);

        // 혹시 markAsSucceeded가 상태 갱신을 안 한다면 대비(멱등)
        job.updateStatus(ProvisionStatus.SUCCEEDED);

        // 완료시각을 외부에서 주입하고 싶다면(세터가 있을 때만) 아래 주석 해제
        // if (result.getCompletedAt() != null) {
        //     job.setCompletedAt(result.getCompletedAt());
        // }

        log.info("Job succeeded: {}, VM ID: {}", job.getJobId(), vmId);
    }

    private void handleFailedStatus(ProvisionJob job, ProvisionResponse result) {
        // 에러 메시지 null/blank 방지
        final String err = (result.getErrorMessage() == null || result.getErrorMessage().isBlank())
                ? "Worker reported failure (no message)"
                : result.getErrorMessage();

        // 도메인 메서드로 실패 처리
        job.markAsFailed(err);

        // 혹시 markAsFailed가 상태 갱신을 안 한다면 대비(멱등)
        job.updateStatus(ProvisionStatus.FAILED);

        // 실패 완료시각을 외부에서 주입하고 싶다면(세터가 있을 때만) 아래 주석 해제
        // if (result.getCompletedAt() != null) {
        //     job.setCompletedAt(result.getCompletedAt());
        // }

        log.error("Job failed: {}, Error: {}", job.getJobId(), err);
    }

}
