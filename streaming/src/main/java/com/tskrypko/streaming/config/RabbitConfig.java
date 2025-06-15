package com.tskrypko.streaming.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ configuration for streaming service
 */
@Configuration
@EnableRabbit
public class RabbitConfig {

    @Value("${rabbitmq.exchange.video}")
    private String videoExchange;

    @Value("${rabbitmq.queue.streaming}")
    private String streamingQueueName;

    @Value("${rabbitmq.routing.key.streaming}")
    private String streamingRoutingKey;

    /**
     * Declare video exchange (shared with other services)
     */
    @Bean
    public TopicExchange videoExchange() {
        return new TopicExchange(videoExchange);
    }

    /**
     * Declare streaming queue
     */
    @Bean
    public Queue streamingQueue() {
        return QueueBuilder.durable(streamingQueueName).build();
    }

    /**
     * Bind streaming queue to video exchange with routing key
     */
    @Bean
    public Binding streamingBinding() {
        return BindingBuilder
                .bind(streamingQueue())
                .to(videoExchange())
                .with(streamingRoutingKey);
    }

    /**
     * RabbitTemplate with JSON message converter
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, ObjectMapper objectMapper) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(new Jackson2JsonMessageConverter(objectMapper));
        return template;
    }
}
