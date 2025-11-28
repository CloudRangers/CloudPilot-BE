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

/**
 * VM 프로비저닝 서비스 (단일/다중 통합)
 * - vmCount=1: Job 1개 생성 → VM 1개
 * - vmCount=N: Job N개 생성 → VM N개 (각 Job은 VM 1개씩 담당)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProvisionService {

    private final ProvisionJobRepository provisionJobRepository;
    private final JobQueueService jobQueueService;
    private final ObjectMapper objectMapper;

    /**
     * VM 프로비저닝 요청 처리 (단일/다중 통합)
     *
     * @param request      클라이언트 요청 (teamId = VM 소유 팀)
     * @param userId       호출자 사용자 ID
     * @param teamId       호출자 팀 ID (없을 수 있음)
     */
    @Transactional
    public ProvisionResponse createProvisionJob(ProvisionRequest request, Long userId, Long teamId) {
        // vmCount 기본값 및 검증
        int vmCount = validateVmCount(request.getVmCount());

        // 호출자 팀과 요청의 teamId를 기준으로 최종 소유 팀 결정
        Long ownerTeamId = resolveOwnerTeamId(request, userId, teamId);

        log.info("Creating {} provision job(s) for user={}, callerTeam={}, ownerTeam={}, provider={}, zone={}",
                vmCount, userId, teamId, ownerTeamId, request.getProviderType(), request.getZoneId());

        try {
            String batchId = vmCount > 1 ? UUID.randomUUID().toString() : null;
            List<Long> jobIds = new ArrayList<>();

            // vmCount만큼 Job 생성 (1이면 1번, 3이면 3번)
            for (int i = 0; i < vmCount; i++) {
                // 1. VM 이름 생성
                String vmName = generateVmName(request.getVmName(), i, vmCount);

                // 2. DB에 Job 레코드 생성
                VmProvisionJob job = createJobRecord(request, userId, ownerTeamId, vmName, batchId, i, vmCount);
                VmProvisionJob saved = provisionJobRepository.save(job);
                jobIds.add(saved.getId());

                // ★ 2-1. jobId 기반으로 Terraform state URI 미리 계산
                String stateUri = buildTerraformStateUri(saved.getId());

                // 3. 큐에 메시지 발행 (항상 vmCount=1로 고정)
                publishJobMessage(request, userId, ownerTeamId, saved.getId(), vmName, batchId, i, stateUri);

                log.debug("Job created: id={}, vmName={}, batch={}, index={}/{} (stateUri={})",
                        saved.getId(), vmName,
                        batchId != null ? batchId.substring(0, 8) : "single",
                        i + 1, vmCount,
                        stateUri);
            }

            log.info("Provision job(s) created successfully. count={}, jobIds={}", vmCount, jobIds);

            // 응답 생성 (단일/다중 모두 동일한 구조)
            return buildResponse(jobIds, vmCount, batchId);

        } catch (Exception e) {
            log.error("Failed to create provision job(s). vmCount={}", vmCount, e);
            throw new ProvisionException("프로비저닝 Job 생성 실패: " + e.getMessage(), e);
        }
    }

    // ===== Private Methods =====

    /**
     * 호출자 팀 + 요청 바디의 teamId를 기반으로 VM 소유 팀 결정
     *
     * 규칙:
     * - callerTeamId != null (일반 팀원)
     *   - request.teamId == null → callerTeamId 사용
     *   - request.teamId != callerTeamId → 예외 (다른 팀에 생성 불가)
     * - callerTeamId == null (부장/관리자)
     *   - request.teamId == null → 예외 (어느 팀 소유인지 명시 필요)
     *   - request.teamId != null → 그 팀을 소유 팀으로 사용
     */
    private Long resolveOwnerTeamId(ProvisionRequest request, Long userId, Long callerTeamId) {
        Long requestTeamId = request.getTeamId();

        // 1) 일반 팀원: callerTeamId != null
        if (callerTeamId != null) {
            if (requestTeamId == null) {
                return callerTeamId;
            }
            if (!callerTeamId.equals(requestTeamId)) {
                throw new ProvisionException(
                        "다른 팀으로 VM을 생성할 수 없습니다. (요청 teamId=" +
                                requestTeamId + ", callerTeamId=" + callerTeamId + ")"
                );
            }
            return callerTeamId;
        }

        // 2) 부장/관리자: callerTeamId == null → 요청에 teamId가 반드시 있어야 함
        if (requestTeamId == null) {
            throw new ProvisionException("팀이 없는 사용자는 요청에 teamId를 반드시 포함해야 합니다.");
        }

        // TODO: userId가 해당 teamId에 대한 프로비저닝 권한이 있는지 추가 검증 가능
        return requestTeamId;
    }

    /**
     * vmCount 검증
     */
    private int validateVmCount(Integer vmCount) {
        int count = vmCount != null ? vmCount : 1;
        if (count < 1) {
            throw new ProvisionException("vmCount는 최소 1이어야 합니다: " + count);
        }
        if (count > 100) {
            throw new ProvisionException("vmCount는 최대 100까지 가능합니다: " + count);
        }
        return count;
    }

    /**
     * VM 이름 생성
     * - 단일(vmCount=1): web-server (그대로)
     * - 다중(vmCount>1): web-server-01, web-server-02, ...
     */
    private String generateVmName(String baseName, int index, int totalCount) {
        // 기본 이름이 없으면 자동 생성
        if (baseName == null || baseName.isBlank()) {
            baseName = "vm-" + UUID.randomUUID().toString().substring(0, 8);
        }

        // 단일 생성이면 그대로 반환
        if (totalCount == 1) {
            return baseName;
        }

        // 다중 생성: 이미 숫자로 끝나면 제거
        String cleanName = baseName.replaceAll("-?\\d+$", "");

        // 패딩 길이 계산 (99개 이하: 2자리, 100개: 3자리)
        int padding = totalCount <= 99 ? 2 : 3;
        String suffix = String.format("%0" + padding + "d", index + 1);

        return cleanName + "-" + suffix;
    }

    /**
     * Job 레코드 생성
     */
    private VmProvisionJob createJobRecord(
            ProvisionRequest request, Long userId, Long ownerTeamId,
            String vmName, String batchId, int index, int totalCount) {

        String purpose = request.getPurpose() != null ? request.getPurpose() : "VM Provisioning";

        return VmProvisionJob.builder()
                .catalogId(request.getCatalogId())
                .teamId(ownerTeamId)           // ★ 소유 팀 기준
                .userId(userId)
                .createdBy(userId)
                .zoneId(toShort(request.getZoneId()))
                .status(VmProvisionStatus.queued)
                .retryCount(0)
                .maxRetries(3)
                .purpose(purpose)
                .createdAt(Instant.now())
                .updatedBy(userId)
                .build();
    }

    /**
     * 큐에 메시지 발행
     * 중요: 항상 vmCount=1로 고정!
     */
    private void publishJobMessage(
            ProvisionRequest request, Long userId, Long ownerTeamId,
            Long jobId, String vmName, String batchId, int index,
            String stateUri // ★ 새로 추가: Terraform state URI
    ) {

        // 태그 생성
        Map<String, String> tags = request.getTags() != null
                ? new HashMap<>(request.getTags())   // 원래 태그 복사
                : new HashMap<>();                   // null이면 빈 Map

        // TeamId 태그도 같이 넣어두고 싶으면 (선택)
        tags.putIfAbsent("TeamId", String.valueOf(ownerTeamId));

        // 추가 설정
        Map<String, Object> additionalConfig = new LinkedHashMap<>(
                request.getAdditionalConfig() != null ? request.getAdditionalConfig() : new HashMap<>());

        // ★ stateUri도 additionalConfig에 같이 넣어두면 워커/AI 쪽에서 참조하기 편함
        if (stateUri != null && !stateUri.isBlank()) {
            additionalConfig.putIfAbsent("terraform_state_uri", stateUri);
        }

        ProvisionJobMessage message = ProvisionJobMessage.builder()
                .jobId(String.valueOf(jobId))
                .action("apply")   // 생성 요청은 항상 apply
                .providerType(request.getProviderType() != null
                        ? request.getProviderType()
                        : ProviderType.VSPHERE)
                .zoneId(request.getZoneId() != null
                        ? request.getZoneId().longValue()
                        : null)
                .userId(userId)
                .teamId(ownerTeamId)                 // ★ 소유 팀 기준
                .vmCount(1)                          // 각 Job은 VM 1개만
                .vmName(vmName)
                .cpuCores(request.getCpuCores())
                .memoryGb(request.getMemoryGb())
                .diskGb(request.getDiskGb())
                .tags(tags)
                .additionalConfig(additionalConfig)
                .request(request)
                .stateUri(stateUri)                  // ★ 메시지에도 stateUri 실어 보냄
                .build();

        jobQueueService.pushJob(message, false);
    }

    /**
     * 응답 생성
     * - 단일/다중 모두 동일한 구조 사용
     */
    private ProvisionResponse buildResponse(List<Long> jobIds, int vmCount, String batchId) {
        ProvisionResponse response = new ProvisionResponse();

        // 기본 정보
        response.setTotalCount(vmCount);
        response.setJobIds(jobIds);
        response.setCreatedAt(Instant.now());

        // 단일 생성
        if (vmCount == 1) {
            response.setJobId(String.valueOf(jobIds.get(0)));
            response.setStatus(VmProvisionStatus.queued);
            response.setMessage("VM 생성 작업이 큐에 등록되었습니다");
        }
        // 다중 생성
        else {
            response.setBatchId(batchId);
            response.setStatus(VmProvisionStatus.queued);
            response.setMessage(String.format("%d개의 VM 생성 작업이 큐에 등록되었습니다", vmCount));
        }

        return response;
    }

    /**
     * Integer → short 변환
     */
    private short toShort(Integer v) {
        if (v == null) {
            throw new ProvisionException("zoneId는 필수입니다");
        }
        if (v < 0 || v > Short.MAX_VALUE) {
            throw new ProvisionException("zoneId 범위 초과(SMALLINT): " + v);
        }
        return v.shortValue();
    }

    /**
     * ★ jobId 기준 Terraform state URI 생성
     *   - 워커에서 기본값으로도 이렇게 쓰고 있으니까 BE에서도 동일 규칙으로 맞춰줌
     */
    private String buildTerraformStateUri(Long jobId) {
        if (jobId == null) {
            return null;
        }
        return "/tmp/terraform/" + jobId + "/terraform.tfstate";
    }
}
