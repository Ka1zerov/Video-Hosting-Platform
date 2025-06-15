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
    public void handleVideoQualityCompletedMessage(String message) {
        log.info("Received video quality completion message: {}", message);

        try {
            // Parse the message to check event type
            JsonNode messageNode = objectMapper.readTree(message);
            String eventType = messageNode.path("eventType").asText();

            if ("VIDEO_QUALITIES_COMPLETED".equals(eventType)) {
                // Parse the complete event
                VideoQualityCompletedEvent event = objectMapper.readValue(message, VideoQualityCompletedEvent.class);
                
                log.info("Processing video qualities completed event for video: {} with {} qualities", 
                        event.getVideoId(), event.getCompletedQualities().size());

                // Process the completed qualities
                videoQualityService.processCompletedQualities(event);

                log.info("Successfully processed video qualities completed event for video: {}", 
                        event.getVideoId());
            } else {
                log.debug("Ignoring message with event type: {}", eventType);
            }

        } catch (Exception e) {
            log.error("Error processing video quality completion message: {}", message, e);
            // In production, you might want to send this to a dead letter queue
            // or implement retry logic
        }
    }
} 