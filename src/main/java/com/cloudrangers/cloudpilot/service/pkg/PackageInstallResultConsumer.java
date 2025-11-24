package com.cloudrangers.cloudpilot.service.pkg;

import com.cloudrangers.cloudpilot.domain.pkg.*;
import com.cloudrangers.cloudpilot.domain.vm.VmInstance;
import com.cloudrangers.cloudpilot.dto.message.InstallPackageResultMessage;
import com.cloudrangers.cloudpilot.repository.pkg.AnsRunRepository;
import com.cloudrangers.cloudpilot.repository.pkg.AnsTaskLogRepository;
import com.cloudrangers.cloudpilot.repository.pkg.AnsPackageResultRepository;
import com.cloudrangers.cloudpilot.repository.vm.VmInstanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PackageInstallResultConsumer {

    private final AnsRunRepository ansRunRepository;
    private final AnsTaskLogRepository ansTaskLogRepository;
    private final VmInstanceRepository vmInstanceRepository;
    private final AnsPackageResultRepository ansPackageResultRepository;

    @RabbitListener(queues = "${rabbitmq.queue.package-install-result.name}")
    @Transactional
    public void consume(InstallPackageResultMessage msg) {

        log.info("📨 Received package install result — ansRunId={}, jobId={}",
                msg.getAnsRunId(), msg.getJobId());

        VmInstance vm = vmInstanceRepository.findById(msg.getVmId())
                .orElseThrow(() -> new RuntimeException("VM not found: " + msg.getVmId()));

        AnsRun run = ansRunRepository.findById(msg.getAnsRunId())
                .orElseThrow(() -> new RuntimeException("AnsRun not found: " + msg.getAnsRunId()));

        // 🔥 전체 실행 결과 업데이트
        run.setStatus(msg.isSuccess()
                ? AnsRun.RunStatus.succeeded
                : AnsRun.RunStatus.failed);
        run.setFinishedAt(java.time.LocalDateTime.now());
        ansRunRepository.save(run);

        // 🔥 1) Task Log 저장
        if (msg.getTasks() != null) {
            for (var task : msg.getTasks()) {

                AnsTaskLog.TaskStatus st = task.isSuccess()
                        ? AnsTaskLog.TaskStatus.ok
                        : AnsTaskLog.TaskStatus.failed;

                AnsTaskLog logEntry = AnsTaskLog.builder()
                        .ansRun(run)
                        .status(st)
                        .stdoutSnippet(task.getStdout())
                        .stderrSnippet(task.getStderr())
                        .build();

                ansTaskLogRepository.save(logEntry);
            }
        }

        // 🔥 2) 패키지 설치 결과 저장(ans_package_result)
        if (msg.getPackages() != null) {

            msg.getPackages().forEach((pkgName, version) -> {

                String status = msg.isSuccess() ? "installed" : "failed";

                AnsPackageResult result = AnsPackageResult.builder()
                        .ansRunId(msg.getAnsRunId())
                        .vmId(msg.getVmId())
                        .packageName(pkgName)
                        .version(version)
                        .status(AnsPackageResult.InstallStatus.valueOf(status))
                        .build();

                ansPackageResultRepository.save(result);

                log.info("📦 Saved package result: {} {} ({})",
                        pkgName, version, status);
            });
        }

        log.info("✅ Completed save for ansRunId={}", msg.getAnsRunId());
    }
}
