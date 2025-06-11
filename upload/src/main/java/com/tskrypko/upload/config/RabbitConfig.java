package com.tskrypko.upload.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    @Value("${rabbitmq.exchange.video:video.exchange}")
    private String videoExchange;

    @Value("${rabbitmq.queue.encoding:video.encoding.queue}")
    private String encodingQueue;

    @Value("${rabbitmq.routing.key.encoding:video.encoding}")
    private String encodingRoutingKey;

    @Bean
    public TopicExchange videoExchange() {
        return new TopicExchange(videoExchange);
    }

    @Bean
    public Queue encodingQueue() {
        return QueueBuilder.durable(encodingQueue).build();
    }

    @Bean
    public Binding encodingBinding() {
        return BindingBuilder
                .bind(encodingQueue())
                .to(videoExchange())
                .with(encodingRoutingKey);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter());
        return factory;
    }
} 