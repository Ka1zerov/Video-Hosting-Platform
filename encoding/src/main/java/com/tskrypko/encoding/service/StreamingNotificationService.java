package com.tskrypko.encoding.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tskrypko.encoding.dto.VideoQualityCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service for sending notifications to streaming service
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StreamingNotificationService {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Value("${rabbitmq.exchange.video}")
    private String videoExchange;

    @Value("${rabbitmq.routing.key.streaming:video.streaming}")
    private String streamingRoutingKey;

    /**
     * Send video quality completion notification to streaming service
     */
    public void notifyVideoQualitiesCompleted(VideoQualityCompletedEvent event) {
        try {
            String message = objectMapper.writeValueAsString(event);
            
            rabbitTemplate.convertAndSend(
                videoExchange,
                streamingRoutingKey,
                message
            );
            
            log.info("Sent video qualities completion notification for video: {} with {} qualities", 
                    event.getVideoId(), event.getCompletedQualities().size());
            
        } catch (Exception e) {
            log.error("Failed to send video qualities completion notification for video: {}", 
                    event.getVideoId(), e);
        }
    }
} 