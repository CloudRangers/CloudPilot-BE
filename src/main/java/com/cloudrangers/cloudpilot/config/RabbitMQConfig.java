package com.cloudrangers.cloudpilot.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@EnableRabbit
@Configuration
public class RabbitMQConfig {

    // ===== Job =====
    @Value("${rabbitmq.queue.provision.name:provision-jobs}")
    private String provisionQueueName;

    @Value("${rabbitmq.exchange.provision.name:provision-exchange}")
    private String provisionExchangeName;

    // 토픽 확장성 (발행은 provision.create[.provider] 형태로 보냄)
    @Value("${rabbitmq.routing-key.provision.pattern:provision.#}")
    private String provisionRoutingPattern;

    // ===== Result =====
    @Value("${rabbitmq.queue.result.name:provision-results}")
    private String resultQueueName;

    @Value("${rabbitmq.exchange.result.name:result-exchange}")
    private String resultExchangeName;

    @Value("${rabbitmq.routing-key.result.pattern:result.provision.#}")
    private String resultRoutingPattern;

    // ===== DLX/DLQ =====
    @Value("${rabbitmq.queue.dlq.name:provision-jobs.dlq}")
    private String dlqName;

    @Value("${rabbitmq.exchange.dlx.name:provision-dlx}")
    private String dlxName;

    @Value("${rabbitmq.routing-key.dlq:provision.failed}")
    private String dlqRoutingKey;

    /** JSON 메시지 변환기 */
    @Bean
    public MessageConverter jsonMessageConverter() {
        ObjectMapper objectMapper = new ObjectMapper();

        // ✅ Enum 대소문자 구분 안 함
        objectMapper.enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS);

        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        return new Jackson2JsonMessageConverter(objectMapper);
    }

    /** RabbitTemplate */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         MessageConverter jsonMessageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter);
        return rabbitTemplate;
    }


    // ========= Job Queue/Exchange/Binding =========
    @Bean
    public Queue provisionQueue() {
        // x-max-priority 추가 (우선순위 발행 사용 시 필수)
        return QueueBuilder.durable(provisionQueueName)
                .withArgument("x-dead-letter-exchange", dlxName)
                .withArgument("x-dead-letter-routing-key", dlqRoutingKey)
                .build();
    }

    @Bean
    public TopicExchange provisionExchange() {
        return new TopicExchange(provisionExchangeName, true, false);
    }

    @Bean
    public Binding provisionBinding(
            @Qualifier("provisionQueue") Queue provisionQueue,
            @Qualifier("provisionExchange") TopicExchange provisionExchange) {
        return BindingBuilder.bind(provisionQueue).to(provisionExchange).with(provisionRoutingPattern);
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
        return BindingBuilder.bind(resultQueue).to(resultExchange).with(resultRoutingPattern);
    }

    // ========= Dead Letter Exchange/Queue/Binding =========
    @Bean
    public TopicExchange deadLetterExchange() {
        return new TopicExchange(dlxName, true, false);
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(dlqName).build();
    }

    @Bean
    public Binding deadLetterBinding(
            @Qualifier("deadLetterQueue") Queue deadLetterQueue,
            @Qualifier("deadLetterExchange") TopicExchange deadLetterExchange) {
        return BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange).with(dlqRoutingKey);
    }

    /** Listener 컨테이너 */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            SimpleRabbitListenerContainerFactoryConfigurer configurer,
            MessageConverter jsonMessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setMessageConverter(jsonMessageConverter);
        return factory;
    }
}
