package com.cloudrangers.cloudpilot.service.provision;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Job을 RabbitMQ에 발행하는 서비스 (JSON 문자열 전송)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JobQueueService {

    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange.provision.name:provision-exchange}")
    private String exchangeName;

    @Value("${rabbitmq.routing-key.provision:provision.create}")
    private String routingKey;

    /**
     * 일반 우선순위 Job 발행 (JSON 문자열)
     */
    public void pushJob(String jobJson) {
        try {
            log.info("Publishing job to RabbitMQ (bytes={}): exchange={}, routingKey={}",
                    jobJson.length(), exchangeName, routingKey);

            rabbitTemplate.convertAndSend(exchangeName, routingKey, jobJson);

            log.info("Job successfully published.");
        } catch (Exception e) {
            log.error("Failed to publish job to RabbitMQ", e);
            throw new RuntimeException("Failed to publish job to queue", e);
        }
    }

    /**
     * 우선순위가 높은 Job 발행 (JSON 문자열)
     */
    public void pushHighPriorityJob(String jobJson) {
        try {
            log.info("Publishing HIGH priority job to RabbitMQ: exchange={}, routingKey={}",
                    exchangeName, routingKey);

            rabbitTemplate.convertAndSend(
                    exchangeName,
                    routingKey,
                    jobJson,
                    message -> {
                        message.getMessageProperties().setPriority(10); // 우선순위 설정
                        return message;
                    }
            );

            log.info("High priority job successfully published.");
        } catch (Exception e) {
            log.error("Failed to publish high priority job", e);
            throw new RuntimeException("Failed to publish job to queue", e);
        }
    }
}
