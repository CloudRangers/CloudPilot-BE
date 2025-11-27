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

        // 3. 삭제 Job ID 생성
        String jobId = UUID.randomUUID().toString();

        // 4. VM 상태를 'deleting'으로 변경
        vm.setLifecycle("deleting");
        vm.setUpdatedAt(Instant.now());
        vm.setUpdatedBy(requestedBy);
        vmInstanceRepository.save(vm);

        log.info("✅ [VmDeleteService] VM lifecycle updated to 'deleting': " +
                        "vmId={}, vmName={}, previousLifecycle={}",
                vmId, vm.getName(), vm.getLifecycle());

        // 5. JobQueueService를 통해 destroy Job 전송
        try {
            jobQueueService.pushDeleteJob(jobId, vm, requestedBy);
            log.info("📤 [VmDeleteService] Destroy job enqueued: jobId={}, vmId={}",
                    jobId, vmId);
        } catch (Exception e) {
            log.error("❌ [VmDeleteService] Failed to enqueue destroy job: " +
                    "jobId={}, vmId={}", jobId, vmId, e);

            // Job 전송 실패 시 상태 롤백
            vm.setLifecycle("running");  // 원래 상태로 복구 (가정)
            vm.setUpdatedAt(Instant.now());
            vmInstanceRepository.save(vm);

            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to enqueue VM deletion job: " + e.getMessage()
            );
        }

        // 6. 응답 생성
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
     *
     * @param vmId 삭제된 VM ID
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
     *
     * @param vmId 삭제 실패한 VM ID
     * @param errorMessage 에러 메시지
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

        // 삭제 실패 시 상태를 다시 'running'으로 복구 (또는 'deletion_failed' 등 별도 상태 추가)
        vm.setLifecycle("running");
        vm.setUpdatedAt(Instant.now());
        vmInstanceRepository.save(vm);

        log.warn("⚠️ [VmDeleteService] VM lifecycle reverted to 'running' after deletion failure: " +
                "vmId={}, vmName={}", vmId, vm.getName());
    }
}