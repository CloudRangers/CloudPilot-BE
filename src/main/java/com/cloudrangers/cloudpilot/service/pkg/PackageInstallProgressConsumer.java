package com.cloudrangers.cloudpilot.service.pkg;

import com.cloudrangers.cloudpilot.dto.message.InstallPackageProgressMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PackageInstallProgressConsumer {

    private final PackageInstallSseService sseService;

    @RabbitListener(queues = "${rabbitmq.queue.package-install-progress.name}")
    public void consumeProgress(InstallPackageProgressMessage msg) {

        log.info("📨 [Progress-Consume] jobId={}, progress={}, stage={}",
                msg.getJobId(), msg.getProgress(), msg.getStage());

        sseService.sendProgress(msg);
    }
}
