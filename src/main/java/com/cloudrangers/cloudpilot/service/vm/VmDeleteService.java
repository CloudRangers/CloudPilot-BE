package com.cloudrangers.cloudpilot.service.vm;

import com.cloudrangers.cloudpilot.domain.vm.VmInstance;
import com.cloudrangers.cloudpilot.dto.response.DeleteVmResponse;
import com.cloudrangers.cloudpilot.repository.vm.VmInstanceRepository;
import com.cloudrangers.cloudpilot.service.provision.JobQueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.UUID;

/**
 * VM 삭제 서비스
 * - VM 삭제 요청 처리
 * - lifecycle 상태를 'deleting'으로 변경
 * - JobQueueService를 통해 destroy Job 전송
 *
 * 💡 주의:
 *  - 실제 Terraform destroy 시에는 VM 생성 시점에 사용된 tf_run의 state 파일이 필요하다.
 *  - 따라서 JobQueueService.pushDeleteJob(...) 내부에서
 *      - vm.getId()      → additionalConfig.vmId
 *      - vm.getTfRunId() → additionalConfig.tfRunId
 *      - vm.getStateUri()→ additionalConfig.stateUri
 *    를 넣어줘야 TerraformExecutor 가 destroy 시 이전 state 를 재사용할 수 있다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VmDeleteService {

    private final VmInstanceRepository vmInstanceRepository;
    private final JobQueueService jobQueueService;

    /**
     * VM 삭제 요청
     *
     * @param vmId 삭제할 VM ID
     * @param requestedBy 요청자 User ID
     * @return 삭제 Job 정보
     * @throws ResponseStatusException VM을 찾을 수 없거나 이미 삭제 중인 경우
     */
    @Transactional
    public DeleteVmResponse enqueueDeletion(Long vmId, Long requestedBy) {
        log.info("🗑️ [VmDeleteService] VM 삭제 요청: vmId={}, requestedBy={}", vmId, requestedBy);

        // 1. VM 조회
        VmInstance vm = vmInstanceRepository.findById(vmId)
                .orElseThrow(() -> {
                    log.error("❌ [VmDeleteService] VM not found: vmId={}", vmId);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "VM not found: " + vmId);
                });

        // 2. 상태 검증 - 이미 삭제 중이거나 삭제된 경우
        if ("deleting".equalsIgnoreCase(vm.getLifecycle())) {
            log.warn("⚠️ [VmDeleteService] VM already deleting: vmId={}, lifecycle={}",
                    vmId, vm.getLifecycle());
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "VM is already being deleted: " + vm.getName()
            );
        }

        if ("deleted".equalsIgnoreCase(vm.getLifecycle())) {
            log.warn("⚠️ [VmDeleteService] VM already deleted: vmId={}, lifecycle={}",
                    vmId, vm.getLifecycle());
            throw new ResponseStatusException(
                    HttpStatus.GONE,
                    "VM has already been deleted: " + vm.getName()
            );
        }

        // 3. Terraform state 연동 정보 로그 (디버깅/운영상 확인용)
        Long tfRunId = vm.getTfRunId();
        String stateUri = vm.getStateUri();

        log.info("🧩 [VmDeleteService] Current VM state before deletion: " +
                        "vmId={}, vmName={}, tfRunId={}, stateUri={}",
                vmId, vm.getName(), tfRunId, stateUri);

        if (stateUri == null || stateUri.isBlank()) {
            // 여기서 에러를 던지지 않고 경고만 남긴다.
            // - 이미 수동으로 삭제된 VM을 DB 상에서만 정리할 수도 있기 때문.
            // - Terraform 관점에서는 state 없이 destroy를 실행하면 "삭제할 리소스 없음"으로 끝난다.
            log.warn("⚠️ [VmDeleteService] VM {} 는 stateUri 가 없어 Terraform 기반 자동 삭제는 수행되지 않을 수 있음. " +
                            "이미 vSphere 에서 수동 삭제된 VM 이거나, 이전 버전에서 생성된 VM 일 수 있음.",
                    vmId);
        }

        // 4. 삭제 Job ID 생성 (워커에서 사용하는 논리적 jobId)
        String jobId = UUID.randomUUID().toString();

        // 5. VM 상태를 'deleting'으로 변경
        String previousLifecycle = vm.getLifecycle();   // 로그/롤백용으로 먼저 저장
        vm.setLifecycle("deleting");
        vm.setUpdatedAt(Instant.now());
        vm.setUpdatedBy(requestedBy);
        vmInstanceRepository.save(vm);

        log.info("✅ [VmDeleteService] VM lifecycle updated to 'deleting': " +
                        "vmId={}, vmName={}, previousLifecycle={}",
                vmId, vm.getName(), previousLifecycle);

        // 6. JobQueueService를 통해 destroy Job 전송
        try {
            // 💡 pushDeleteJob 내부에서 vmId / tfRunId / stateUri 를 additionalConfig에 넣어야 함
            jobQueueService.pushDeleteJob(jobId, vm, requestedBy);

            log.info("📤 [VmDeleteService] Destroy job enqueued: jobId={}, vmId={}",
                    jobId, vmId);
        } catch (Exception e) {
            log.error("❌ [VmDeleteService] Failed to enqueue destroy job: " +
                    "jobId={}, vmId={}", jobId, vmId, e);

            // Job 전송 실패 시 상태 롤백
            vm.setLifecycle(previousLifecycle != null ? previousLifecycle : "running");
            vm.setUpdatedAt(Instant.now());
            vmInstanceRepository.save(vm);

            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to enqueue VM deletion job: " + e.getMessage()
            );
        }

        // 7. 응답 생성
        DeleteVmResponse response = DeleteVmResponse.builder()
                .jobId(jobId)
                .vmId(vm.getId())
                .vmName(vm.getName())
                .status("QUEUED")
                .requestedBy(requestedBy)
                .requestedAt(Instant.now())
                .message("VM deletion job has been queued")
                .providerType(vm.getProviderType())
                .zoneId(vm.getZoneId())
                .build();

        log.info("✅ [VmDeleteService] VM 삭제 요청 완료: " +
                        "jobId={}, vmId={}, vmName={}, status={}",
                jobId, vmId, vm.getName(), response.getStatus());

        return response;
    }

    /**
     * VM 삭제 완료 처리 (Worker에서 SUCCESS 이벤트를 받은 후 호출)
     */
    @Transactional
    public void markAsDeleted(Long vmId) {
        log.info("✅ [VmDeleteService] Marking VM as deleted: vmId={}", vmId);

        VmInstance vm = vmInstanceRepository.findById(vmId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "VM not found: " + vmId
                ));

        vm.setLifecycle("deleted");
        vm.setPowerState("OFF");
        vm.setUpdatedAt(Instant.now());
        vmInstanceRepository.save(vm);

        log.info("✅ [VmDeleteService] VM marked as deleted: vmId={}, vmName={}",
                vmId, vm.getName());
    }

    /**
     * VM 삭제 실패 처리 (Worker에서 ERROR 이벤트를 받은 후 호출)
     */
    @Transactional
    public void markDeletionFailed(Long vmId, String errorMessage) {
        log.error("❌ [VmDeleteService] VM deletion failed: vmId={}, error={}",
                vmId, errorMessage);

        VmInstance vm = vmInstanceRepository.findById(vmId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "VM not found: " + vmId
                ));

        vm.setLifecycle("running");
        vm.setUpdatedAt(Instant.now());
        vmInstanceRepository.save(vm);

        log.warn("⚠️ [VmDeleteService] VM lifecycle reverted to 'running' after deletion failure: " +
                "vmId={}, vmName={}", vmId, vm.getName());
    }
}
