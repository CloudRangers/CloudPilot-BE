package com.cloudrangers.cloudpilot.service.pkg;

import com.cloudrangers.cloudpilot.domain.pkg.AnsPackageResult;
import com.cloudrangers.cloudpilot.domain.pkg.AnsRun;
import com.cloudrangers.cloudpilot.domain.pkg.AnsTaskLog;
import com.cloudrangers.cloudpilot.domain.vm.VmInstance;
import com.cloudrangers.cloudpilot.dto.message.InstallPackageResultMessage;
import com.cloudrangers.cloudpilot.repository.pkg.AnsPackageResultRepository;
import com.cloudrangers.cloudpilot.repository.pkg.AnsRunRepository;
import com.cloudrangers.cloudpilot.repository.pkg.AnsTaskLogRepository;
import com.cloudrangers.cloudpilot.repository.vm.VmInstanceRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PackageInstallResultConsumer {

    private final VmInstanceRepository vmInstanceRepository;
    private final AnsRunRepository ansRunRepository;
    private final AnsTaskLogRepository ansTaskLogRepository;
    private final AnsPackageResultRepository ansPackageResultRepository;

    @RabbitListener(queues = "${rabbitmq.queue.package-install-result.name}")
    @Transactional
    public void consume(InstallPackageResultMessage msg) {

        log.info("📨 [Package-Result] Received — ansRunId={}, jobId={}, vmId={}",
                msg.getAnsRunId(), msg.getJobId(), msg.getVmId());

        try {
            // ============================
            // 1) VM / AnsRun 조회
            // ============================
            VmInstance vm = vmInstanceRepository.findById(msg.getVmId())
                    .orElseThrow(() ->
                            new EntityNotFoundException("VM not found: " + msg.getVmId()));

            AnsRun run = ansRunRepository.findById(msg.getAnsRunId())
                    .orElseThrow(() ->
                            new EntityNotFoundException("AnsRun not found: " + msg.getAnsRunId()));

            // ============================
            // 2) Task Log 저장
            // ============================
            if (msg.getTasks() != null && !msg.getTasks().isEmpty()) {

                msg.getTasks().forEach(task -> {

                    AnsTaskLog.TaskStatus st =
                            task.isSuccess()
                                    ? AnsTaskLog.TaskStatus.ok
                                    : AnsTaskLog.TaskStatus.failed;

                    AnsTaskLog taskLog = AnsTaskLog.builder()
                            .ansRun(run)
                            .status(st)
                            .stdoutSnippet(task.getStdout())
                            .stderrSnippet(task.getStderr())
                            .build();

                    ansTaskLogRepository.save(taskLog);

                    log.info("📝 [Task-Log] Saved task log — status={}, stdoutLength={}, stderrLength={}",
                            st,
                            task.getStdout() != null ? task.getStdout().length() : 0,
                            task.getStderr() != null ? task.getStderr().length() : 0
                    );
                });
            }

            // ============================
            // 3) 패키지 설치 결과 저장(ans_package_result)
            // ============================
            if (msg.getPackages() != null && !msg.getPackages().isEmpty()) {

                msg.getPackages().forEach((pkgName, version) -> {

                    // 패키지 단위 성공 여부 (확장 여지)
                    boolean pkgSuccess = msg.isSuccess();

                    AnsPackageResult.InstallStatus status =
                            pkgSuccess
                                    ? AnsPackageResult.InstallStatus.installed
                                    : AnsPackageResult.InstallStatus.failed;

                    AnsPackageResult result = AnsPackageResult.builder()
                            .ansRunId(msg.getAnsRunId())
                            .vmId(msg.getVmId())
                            .packageName(pkgName)
                            .version(version)
                            .status(status)
                            .build();

                    ansPackageResultRepository.save(result);

                    log.info("📦 [Package-Result] Saved package result — {} {} ({})",
                            pkgName, version, status);
                });
            }

            // ============================
            // 4) Run 상태 업데이트
            // ============================
            run.setStatus(msg.isSuccess()
                    ? AnsRun.RunStatus.succeeded
                    : AnsRun.RunStatus.failed);

            ansRunRepository.save(run);

            log.info("✅ [Package-Result] Completed ansRunId={} — finalStatus={}",
                    run.getId(), run.getStatus());

        } catch (EntityNotFoundException ex) {
            // 재시도 불필요 → 즉시 ACK 후 메시지 버림
            log.error("❌ [Package-Result] Missing entity. Discarding message. jobId={}, reason={}",
                    msg.getJobId(), ex.getMessage());
        } catch (Exception ex) {
            // 재시도 필요 → throw 하면 RabbitMQ에서 재시도 또는 DLQ 이동
            log.error("🔥 [Package-Result] Unexpected error — jobId={}, error={}",
                    msg.getJobId(), ex.getMessage(), ex);
            throw ex;
        }
    }
}
