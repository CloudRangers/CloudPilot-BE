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
            // 1) DB 저장: Integer → Short 캐스팅
            VmProvisionJob job = VmProvisionJob.builder()
                    .catalogId(request.getCatalogId())
                    .teamId(teamId)
                    .userId(userId)            // 엔티티에 있으면 유지
                    .createdBy(userId)
                    .zoneId(toShort(request.getZoneId()))   // ★ 여기서 Short로 저장
                    .status(VmProvisionStatus.queued)
                    .retryCount(0)
                    .maxRetries(3)
                    .purpose(request.getPurpose())
                    .createdAt(Instant.now())
                    .updatedBy(userId)
                    .build();

            VmProvisionJob saved = provisionJobRepository.save(job);

            // 2) 워커로 보낼 메시지: Integer 유지
            ProvisionJobMessage message = ProvisionJobMessage.builder()
                    .jobId(String.valueOf(saved.getId()))
                    .userId(userId)
                    .teamId(teamId)
                    .zoneId(request.getZoneId()) // ★ 메시지는 Integer
                    .providerType(request.getProviderType() != null
                            ? request.getProviderType()
                            : enumVsphereFallback())
                    .action("apply")
                    .request(request)
                    .vmCount(request.getVmCount())
                    .vmName(request.getVmName())
                    .cpuCores(request.getCpuCores())
                    .memoryGb(request.getMemoryGb())
                    .diskGb(request.getDiskGb())
                    .tags(request.getTags())
                    .additionalConfig(request.getAdditionalConfig())
                    .build();

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
            // ★ request 변수 없음 → job에서 꺼내고 Short→Integer로 변환
            Integer zoneIdForMsg = (job.getZoneId() != null) ? Integer.valueOf(job.getZoneId()) : null;

            ProvisionJobMessage message = ProvisionJobMessage.builder()
                    .jobId(String.valueOf(job.getId()))
                    .userId(job.getCreatedBy())
                    .teamId(job.getTeamId())
                    .zoneId(zoneIdForMsg)          // ★ 메시지는 Integer
                    .providerType(enumVsphereFallback())
                    .action("apply")
                    .request(null) // 스냅샷 없으면 null
                    .build();

            job.setRetryCount(Optional.ofNullable(job.getRetryCount()).orElse(0) + 1);
            job.setStatus(VmProvisionStatus.queued);
            job.setStartedAt(null);
            job.setFinishedAt(null);
            job.setErrorMessage(null);

            provisionJobRepository.save(job);
            jobQueueService.pushJob(message, true);

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

    // (선택) vSphere ENV를 사용해야 할 경우 — 오타 수정: VSPHERE_USER
    @SuppressWarnings("unused")
    private Map<String, String> getVsphereCredentials() {
        Map<String, String> credentials = new HashMap<>();
        credentials.put("server",   System.getenv("VSPHERE_SERVER"));
        credentials.put("username", System.getenv("VSPHERE_USER"));      // <- USERNAME 아님
        credentials.put("password", System.getenv("VSPHERE_PASSWORD"));
        return credentials;
    }

    private ProviderType enumVsphereFallback() {
        return com.cloudrangers.cloudpilot.enums.ProviderType.VSPHERE;
    }

    private ProvisionResponse mapToResponse(VmProvisionJob job) {
        return ProvisionResponse.builder()
                .id(job.getId())
                .jobId(String.valueOf(job.getId()))
                .catalogId(job.getCatalogId())           // ✅ 중복/널 오버라이드 제거
                .userId(job.getCreatedBy())
                .teamId(job.getTeamId())
                .status(mapStatus(job.getStatus()))
                .errorMessage(job.getErrorMessage())
                .retryCount(job.getRetryCount())
                .startedAt(job.getStartedAt())
                .completedAt(job.getFinishedAt())
                .createdAt(job.getCreatedAt())
                .updatedAt(job.getFinishedAt() != null ? job.getFinishedAt() : job.getCreatedAt())
                .build();
    }

    private VmProvisionStatus mapStatus(VmProvisionStatus s) {
        if (s == null) return null;
        return switch (s) {
            case queued -> VmProvisionStatus.queued;
            case running -> VmProvisionStatus.running;
            case succeeded -> VmProvisionStatus.succeeded;
            case failed, canceled -> VmProvisionStatus.failed;
        };
    }

    private short toShort(Integer v) {
        if (v == null) throw new ProvisionException("zoneId는 필수입니다");
        if (v < 0 || v > Short.MAX_VALUE) throw new ProvisionException("zoneId 범위 초과(SMALLINT): " + v);
        return v.shortValue();
    }
}
