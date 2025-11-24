package com.cloudrangers.cloudpilot.service.pkg.event;

import com.cloudrangers.cloudpilot.dto.message.InstallPackageJobMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class PackageInstallJobEventListener {

    private final RabbitTemplate rabbitTemplate;

    // properties 사용하지 않고 소스 코드 고정
    private static final String EXCHANGE = "package-install-exchange";
    private static final String ROUTING_KEY = "package.install";

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAfterCommit(PackageInstallJobEvent event) {

        for (InstallPackageJobMessage msg : event.getMessages()) {

            log.info("📤 [AFTER_COMMIT] Sending package install job — jobId={}, vmId={}",
                    msg.getJobId(), msg.getVmId());

            rabbitTemplate.convertAndSend(
                    EXCHANGE,
                    ROUTING_KEY,
                    msg
            );
        }

        log.info("✅ [AFTER_COMMIT] All package install jobs sent: {} messages",
                event.getMessages().size());
    }
}
