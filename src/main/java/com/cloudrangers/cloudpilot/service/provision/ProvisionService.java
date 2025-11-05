package com.cloudrangers.cloudpilot.service.provision;

import com.cloudrangers.cloudpilot.domain.provision.ProvisionJob;
import com.cloudrangers.cloudpilot.dto.request.ProvisionRequest;
import com.cloudrangers.cloudpilot.dto.response.ProvisionResponse;
import com.cloudrangers.cloudpilot.enums.ProvisionStatus;
import com.cloudrangers.cloudpilot.exception.ProvisionException;
import com.cloudrangers.cloudpilot.repository.provision.ProvisionJobRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 프로비저닝 Job 관리 서비스 (API 서버용)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProvisionService {

    private final ProvisionJobRepository provisionJobRepository;
    private final JobQueueService jobQueueService;
    private final ObjectMapper objectMapper;

    /**
     * 프로비저닝 Job 생성 및 큐에 푸시
     */
    @Transactional
    public ProvisionResponse createProvisionJob(ProvisionRequest request, Long userId, Long teamId) {

        log.info("Creating provision job for user={}, team={}, provider={}",
                userId, teamId, request.getProviderType());

        try {
            // 1) Job ID 생성
            String jobId = UUID.randomUUID().toString();

            // 2) Terraform 설정(JSON) 저장용
            String terraformConfig = generateTerraformConfig(request);

            // 3) DB 저장
            ProvisionJob job = ProvisionJob.builder()
                    .jobId(jobId)
                    .catalogId(request.getCatalogId())
                    .userId(userId)
                    .teamId(teamId)
                    .providerType(request.getProviderType())
                    .status(ProvisionStatus.QUEUED)
                    .terraformConfig(terraformConfig)
                    .retryCount(0)
                    .build();

            ProvisionJob savedJob = provisionJobRepository.save(job);
            log.info("Job saved to database: {}", savedJob.getJobId());

            // 4) Provider 자격증명
            Map<String, String> credentials = getProviderCredentials(request.getProviderType(), teamId);

            // 5) 큐 메시지(payload) 구성
            Map<String, Object> messagePayload = new HashMap<>();
            messagePayload.put("jobId", jobId);
            messagePayload.put("userId", userId);
            messagePayload.put("teamId", teamId);
            messagePayload.put("credentials", credentials);
            messagePayload.put("request", request);

            // 6) JSON 직렬화 후 큐 발행
            String jobJson = objectMapper.writeValueAsString(messagePayload);
            jobQueueService.pushJob(jobJson);

            log.info("Provision job pushed to RabbitMQ: {}", jobId);

            return mapToResponse(savedJob);

        } catch (Exception e) {
            log.error("Failed to create provision job", e);
            throw new ProvisionException("프로비저닝 Job 생성 실패: " + e.getMessage());
        }
    }

    /**
     * Job 상태 조회
     */
    @Transactional(readOnly = true)
    public ProvisionResponse getJobStatus(String jobId) {
        ProvisionJob job = provisionJobRepository.findByJobId(jobId)
                .orElseThrow(() -> new ProvisionException("Job을 찾을 수 없습니다: " + jobId));
        return mapToResponse(job);
    }

    /**
     * 팀별 Job 목록 조회
     */
    @Transactional(readOnly = true)
    public List<ProvisionResponse> getTeamJobs(Long teamId) {
        return provisionJobRepository.findByTeamId(teamId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Job 재시도 (다시 큐에 푸시)
     */
    @Transactional
    public void retryJob(String jobId) {
        ProvisionJob job = provisionJobRepository.findByJobId(jobId)
                .orElseThrow(() -> new ProvisionException("Job을 찾을 수 없습니다: " + jobId));

        if (!job.canRetry()) {
            throw new ProvisionException("최대 재시도 횟수를 초과했습니다");
        }

        try {
            // 기존 저장된 terraformConfig(JSON)에서 ProvisionRequest 복원
            ProvisionRequest request = objectMapper.readValue(job.getTerraformConfig(), ProvisionRequest.class);

            // 최신 자격증명 재조회 (회전/갱신 고려)
            Map<String, String> credentials = getProviderCredentials(job.getProviderType(), job.getTeamId());

            Map<String, Object> messagePayload = new HashMap<>();
            messagePayload.put("jobId", job.getJobId());     // 동일 Job ID로 재시도
            messagePayload.put("userId", job.getUserId());
            messagePayload.put("teamId", job.getTeamId());
            messagePayload.put("credentials", credentials);
            messagePayload.put("request", request);

            String jobJson = objectMapper.writeValueAsString(messagePayload);

            // 상태/재시도 카운트 갱신 후 재발행
            job.incrementRetryCount();
            job.updateStatus(ProvisionStatus.QUEUED);
            provisionJobRepository.save(job);

            jobQueueService.pushJob(jobJson);

            log.info("Job retry pushed: jobId={}, retryCount={}", job.getJobId(), job.getRetryCount());
        } catch (Exception e) {
            log.error("Failed to retry job: {}", job.getJobId(), e);
            throw new ProvisionException("Job 재시도 실패: " + e.getMessage());
        }
    }

    /**
     * Terraform 설정 생성(JSON)
     */
    private String generateTerraformConfig(ProvisionRequest request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (Exception e) {
            throw new ProvisionException("Terraform 설정 생성 실패: " + e.getMessage());
        }
    }

    /**
     * Provider 자격증명 가져오기
     * TODO: CredentialService에서 안전하게 가져와야 함
     */
    private Map<String, String> getProviderCredentials(
            com.cloudrangers.cloudpilot.enums.ProviderType providerType,
            Long teamId) {

        Map<String, String> credentials = new HashMap<>();

        switch (providerType) {
            case AWS:
                credentials.put("region", System.getenv("AWS_REGION"));
                credentials.put("access_key", System.getenv("AWS_ACCESS_KEY_ID"));
                credentials.put("secret_key", System.getenv("AWS_SECRET_ACCESS_KEY"));
                break;
            case VSPHERE:
                credentials.put("server", System.getenv("VSPHERE_SERVER"));
                credentials.put("username", System.getenv("VSPHERE_USERNAME"));
                credentials.put("password", System.getenv("VSPHERE_PASSWORD"));
                break;
            default:
                throw new IllegalArgumentException("Unsupported provider: " + providerType);
        }

        return credentials;
    }

    /**
     * Entity → Response DTO 변환
     */
    private ProvisionResponse mapToResponse(ProvisionJob job) {
        return ProvisionResponse.builder()
                .id(job.getId())
                .jobId(job.getJobId())
                .catalogId(job.getCatalogId())
                .userId(job.getUserId())
                .teamId(job.getTeamId())
                .providerType(job.getProviderType())
                .status(job.getStatus())
                .vmResourceId(job.getVmResourceId())
                .errorMessage(job.getErrorMessage())
                .retryCount(job.getRetryCount())
                .startedAt(job.getStartedAt())
                .completedAt(job.getCompletedAt())
                .createdAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt())
                .build();
    }
}
