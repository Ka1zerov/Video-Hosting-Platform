package com.tskrypko.streaming.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tskrypko.streaming.dto.VideoQualityCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

/**
 * RabbitMQ message listener for video quality completion events
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VideoQualityMessageListener {

    private final VideoQualityService videoQualityService;
    private final ObjectMapper objectMapper;

    /**
     * Listen for video quality completion messages from encoding service
     */
    @RabbitListener(queues = "${rabbitmq.queue.streaming}")
    public void handleVideoQualityCompletedMessage(VideoQualityCompletedEvent event) {
        log.info("Received video quality completion event: {}", event);

        try {
            if (event != null && "VIDEO_QUALITIES_COMPLETED".equals(event.getEventType())) {
                log.info("Processing video qualities completed event for video: {} with {} qualities", 
                        event.getVideoId(), event.getCompletedQualities().size());

                // Process the completed qualities
                videoQualityService.processCompletedQualities(event);

                log.info("Successfully processed video qualities completed event for video: {}", 
                        event.getVideoId());
            } else {
                log.warn("Ignoring event with type: {}", event != null ? event.getEventType() : "null");
            }

        } catch (Exception e) {
            log.error("Error processing video quality completion event: {}", event, e);
            // In production, you might want to send this to a dead letter queue
            // or implement retry logic
        }
    }
} 