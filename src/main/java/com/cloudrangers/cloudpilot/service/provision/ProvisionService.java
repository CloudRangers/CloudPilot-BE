package com.cloudrangers.cloudpilot.service.provision;

import com.cloudrangers.cloudpilot.domain.provision.VmProvisionJob;
import com.cloudrangers.cloudpilot.enums.ProviderType;
import com.cloudrangers.cloudpilot.enums.VmProvisionStatus;
import com.cloudrangers.cloudpilot.dto.message.ProvisionJobMessage;
import com.cloudrangers.cloudpilot.dto.request.ProvisionRequest;
import com.cloudrangers.cloudpilot.dto.response.ProvisionResponse;
import com.cloudrangers.cloudpilot.exception.ProvisionException;
import com.cloudrangers.cloudpilot.repository.provision.ProvisionJobRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProvisionService {

    private final ProvisionJobRepository provisionJobRepository;
    private final JobQueueService jobQueueService;
    private final ObjectMapper objectMapper;

    @Transactional
    public ProvisionResponse createProvisionJob(ProvisionRequest request, Long userId, Long teamId) {
        log.info("Creating provision job for user={}, team={}, provider={}, zone={}",
                userId, teamId, request.getProviderType(), request.getZoneId());

        try {
            // 1) DB 저장 (DDL 필드만)
            VmProvisionJob job = VmProvisionJob.builder()
                    .catalogId(request.getCatalogId())
                    .teamId(teamId)
                    .userId(userId)
                    .createdBy(userId)
                    .zoneId(request.getZoneId())
                    .status(VmProvisionStatus.queued)
                    .retryCount(0)
                    .maxRetries(3)
                    .purpose(request.getPurpose()) // ProvisionRequest에 purpose 있으면 매핑, 없으면 null
                    .createdAt(Instant.now())
                    .updatedBy(userId)
                    .build();

            VmProvisionJob saved = provisionJobRepository.save(job);

            // 2) 자격증명 (온프레미스 vsphere 기본)
            Map<String, String> credentials = getVsphereCredentials();

            // 3) 메시지 구성 (상관관계 ID = DB PK 문자열)
            ProvisionJobMessage message = ProvisionJobMessage.builder()
                    .jobId(String.valueOf(saved.getId()))
                    .userId(userId)
                    .teamId(teamId)
                    .zoneId(request.getZoneId())
                    .providerType(request.getProviderType() != null
                            ? request.getProviderType()
                            : enumVsphereFallback())  // 간단하고 명확함
                    .credentials(credentials)
                    .request(request)
                    .action("apply")
                    .build();

            // 4) 큐 발행
            jobQueueService.pushJob(message, false);

            log.info("Provision job pushed. jobId={}", saved.getId());
            return mapToResponse(saved);

        } catch (Exception e) {
            log.error("Failed to create provision job", e);
            throw new ProvisionException("프로비저닝 Job 생성 실패: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public ProvisionResponse getJobStatus(String jobIdStr) {
        Long jobId = parseId(jobIdStr);
        VmProvisionJob job = provisionJobRepository.findById(jobId)
                .orElseThrow(() -> new ProvisionException("Job을 찾을 수 없습니다: " + jobIdStr));
        return mapToResponse(job);
    }

    @Transactional(readOnly = true)
    public List<ProvisionResponse> getTeamJobs(Long teamId) {
        return provisionJobRepository.findByTeamId(teamId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public void retryJob(String jobIdStr) {
        Long jobId = parseId(jobIdStr);
        VmProvisionJob job = provisionJobRepository.findById(jobId)
                .orElseThrow(() -> new ProvisionException("Job을 찾을 수 없습니다: " + jobIdStr));

        if (job.getRetryCount() != null && job.getMaxRetries() != null
                && job.getRetryCount() >= job.getMaxRetries()) {
            throw new ProvisionException("최대 재시도 횟수를 초과했습니다");
        }

        try {
            // 기존 요청은 별도 저장 X → 워커 입력은 최근 API 요청 본문을 사용하도록 설계
            // 필요하면 job_id 기준 별도 테이블에 Request 스냅샷 보관하도록 확장 가능
            Map<String, String> credentials = getVsphereCredentials();

            ProvisionJobMessage message = ProvisionJobMessage.builder()
                    .jobId(String.valueOf(job.getId()))
                    .userId(job.getCreatedBy())
                    .teamId(job.getTeamId())
                    .zoneId(job.getZoneId())
                    .providerType(enumVsphereFallback())
                    .credentials(credentials)
                    .request(null) // 이전 요청 스냅샷 보관 안 하면 null (워커에서 처리 방식에 맞게 조정)
                    .action("apply")
                    .build();

            // 상태/카운터 갱신
            job.setRetryCount(Optional.ofNullable(job.getRetryCount()).orElse(0) + 1);
            job.setStatus(VmProvisionStatus.queued);
            job.setStartedAt(null);
            job.setFinishedAt(null);
            job.setErrorMessage(null);

            provisionJobRepository.save(job);
            jobQueueService.pushJob(message, true); // 재시도: 우선순위↑

            log.info("Job retry pushed: jobId={}, retryCount={}", job.getId(), job.getRetryCount());
        } catch (Exception e) {
            log.error("Failed to retry job: {}", job.getId(), e);
            throw new ProvisionException("Job 재시도 실패: " + e.getMessage());
        }
    }

    // ===== helpers =====

    private Long parseId(String idStr) {
        try {
            return Long.parseLong(idStr);
        } catch (NumberFormatException nfe) {
            throw new ProvisionException("잘못된 jobId 형식입니다 (숫자여야 함): " + idStr);
        }
    }

    // 온프레미스 vSphere 고정 자격증명 (Vault/Zone 연계로 대체 가능)
    private Map<String, String> getVsphereCredentials() {
        Map<String, String> credentials = new HashMap<>();
        credentials.put("server", System.getenv("VSPHERE_SERVER"));
        credentials.put("username", System.getenv("VSPHERE_USERNAME"));
        credentials.put("password", System.getenv("VSPHERE_PASSWORD"));
        return credentials;
    }

    // providerType 비었을 때 VSPHERE로 대체 (DTO enum이 있음을 가정)
    private ProviderType enumVsphereFallback() {
        return com.cloudrangers.cloudpilot.enums.ProviderType.VSPHERE;
    }

    private ProvisionResponse mapToResponse(VmProvisionJob job) {
        // 기존 ProvisionResponse 스키마가 남아있다고 가정: 없는 값은 null로
        return ProvisionResponse.builder()
                .id(job.getId())
                .catalogId(job.getCatalogId())
                .jobId(String.valueOf(job.getId()))
                .catalogId(null)                 // DDL에 없음
                .userId(job.getCreatedBy())
                .teamId(job.getTeamId())
                .providerType(null)              // DDL에 없음(온프레미스 고정이면 클라이언트에서 vsphere로 처리)
                .status(mapStatus(job.getStatus()))
                .vmResourceId(null)              // DDL에 없음 (필요 시 별도 엔티티에서 조회)
                .errorMessage(job.getErrorMessage())
                .retryCount(job.getRetryCount())
                .startedAt(job.getStartedAt())
                .completedAt(job.getFinishedAt())
                .createdAt(job.getCreatedAt())
                .updatedAt(job.getFinishedAt() != null ? job.getFinishedAt() : job.getCreatedAt())
                .build();
    }

    // 기존 응답 DTO의 상태(enum)가 RUNNING/SUCCEEDED/FAILED/QUEUED 라면 매핑
    private VmProvisionStatus mapStatus(VmProvisionStatus s) {
        if (s == null) return null;
        return switch (s) {
            case queued -> VmProvisionStatus.queued;
            case running -> VmProvisionStatus.running;
            case succeeded -> VmProvisionStatus.succeeded;
            case failed, canceled -> VmProvisionStatus.failed;
        };
    }
}
