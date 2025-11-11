package com.cloudrangers.cloudpilot.service.provision;

import com.cloudrangers.cloudpilot.dto.message.ProvisionJobMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobQueueService {

    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange.provision.name:provision-exchange}")
    private String exchangeName;

    // 예: provision.create.[provider]
    @Value("${rabbitmq.routing-key.provision.base:provision.create}")
    private String baseRoutingKey;

    public void pushJob(ProvisionJobMessage msg, boolean highPriority) {
        final String routingKey = buildRoutingKey(msg);
        try {
            log.info(String.format(
                    "Publishing job: jobId=%s, exchange=%s, rk=%s, provider=%s, zone=%s",
                    safeToString(msg.getJobId()), safeToString(exchangeName), routingKey,
                    safeToString(msg.getProviderType()), safeToString(msg.getZoneId())
            ));

            rabbitTemplate.convertAndSend(
                    exchangeName,
                    routingKey,
                    msg,
                    message -> {
                        // 상관관계 식별자 세팅(= DB PK를 문자열로)
                        message.getMessageProperties().setCorrelationId(msg.getJobId());
                        message.getMessageProperties().setHeader("jobId", msg.getJobId());
                        // 우선순위 큐 사용 시
                        message.getMessageProperties().setPriority(highPriority ? 9 : 0);
                        return message;
                    }
            );
        } catch (Exception e) {
            log.error("Failed to publish job to RabbitMQ. jobId={}", msg.getJobId(), e);
            throw new RuntimeException("Failed to publish job to queue", e);
        }
    }

    private String buildRoutingKey(ProvisionJobMessage msg) {
        // 온프레미스 기본값은 vsphere
        final String provider = Optional.ofNullable(msg.getProviderType())
                .map(Object::toString)
                .map(s -> s.toLowerCase(Locale.ROOT))
                .orElse("vsphere");
        return baseRoutingKey + "." + provider;
    }

    private static String safeToString(Object o) {
        return (o == null) ? "null" : o.toString();
    }
}
