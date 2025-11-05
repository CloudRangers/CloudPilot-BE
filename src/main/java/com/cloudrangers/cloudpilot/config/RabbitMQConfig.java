package com.cloudrangers.cloudpilot.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 설정 (API 서버용)
 * - Queue 인자(x-message-ttl, DLX)는 'Policy'로 적용하여 선언 충돌 방지
 */
@EnableRabbit
@Configuration
public class RabbitMQConfig {

    // Job Queue
    @Value("${rabbitmq.queue.provision.name:provision-jobs}")
    private String provisionQueueName;

    @Value("${rabbitmq.exchange.provision.name:provision-exchange}")
    private String provisionExchangeName;

    @Value("${rabbitmq.routing-key.provision:provision.create}")
    private String provisionRoutingKey;

    // Result Queue
    @Value("${rabbitmq.queue.result.name:provision-results}")
    private String resultQueueName;

    @Value("${rabbitmq.exchange.result.name:result-exchange}")
    private String resultExchangeName;

    @Value("${rabbitmq.routing-key.result:result.completed}")
    private String resultRoutingKey;

    // Dead Letter (DLX/DLQ) — 큐 쪽 인자는 Policy로, 여기서는 교환/바인딩만 선언
    @Value("${rabbitmq.queue.dlq.name:provision-jobs.dlq}")
    private String dlqName;

    @Value("${rabbitmq.exchange.dlx.name:provision-dlx}")
    private String dlxName;

    @Value("${rabbitmq.routing-key.dlq:provision.failed}")
    private String dlqRoutingKey;

    /** JSON 메시지 변환기 */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /** RabbitTemplate */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }

    // ========= Job Queue/Exchange/Binding (인자 없는 선언) =========

    @Bean
    public Queue provisionQueue() {
        // 인자 없이 durable 큐만 선언 (TTL/DLX는 Policy에서 적용)
        return QueueBuilder.durable(provisionQueueName).build();
    }

    @Bean
    public TopicExchange provisionExchange() {
        return new TopicExchange(provisionExchangeName, true, false);
    }

    @Bean
    public Binding provisionBinding(
            @Qualifier("provisionQueue") Queue provisionQueue,
            @Qualifier("provisionExchange") TopicExchange provisionExchange) {
        return BindingBuilder.bind(provisionQueue).to(provisionExchange).with(provisionRoutingKey);
    }

    // ========= Result Queue/Exchange/Binding =========

    @Bean
    public Queue resultQueue() {
        return QueueBuilder.durable(resultQueueName).build();
    }

    @Bean
    public TopicExchange resultExchange() {
        return new TopicExchange(resultExchangeName, true, false);
    }

    @Bean
    public Binding resultBinding(
            @Qualifier("resultQueue") Queue resultQueue,
            @Qualifier("resultExchange") TopicExchange resultExchange) {
        return BindingBuilder.bind(resultQueue).to(resultExchange).with(resultRoutingKey);
    }

    // ========= Dead Letter Exchange/Queue/Binding =========
    // DLX 자체와 DLQ는 우리가 선언. (프로비저닝 큐 -> DLX 라우팅은 Policy가 맡음)

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(dlxName, true, false);
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(dlqName).build();
    }

    @Bean
    public Binding deadLetterBinding(
            @Qualifier("deadLetterQueue") Queue deadLetterQueue,
            @Qualifier("deadLetterExchange") DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange).with(dlqRoutingKey);
    }

    /** Listener 컨테이너 팩토리 */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        factory.setConcurrentConsumers(1);
        factory.setMaxConcurrentConsumers(5);
        factory.setPrefetchCount(1);
        // factory.setDefaultRequeueRejected(false); // 필요 시 NACK 재큐잉 막기
        return factory;
    }

    // @Configuration 클래스 내
    @Bean
    public MessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

}
