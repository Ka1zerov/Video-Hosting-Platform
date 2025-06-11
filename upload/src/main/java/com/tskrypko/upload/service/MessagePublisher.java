package com.tskrypko.upload.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tskrypko.upload.model.Video;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MessagePublisher {

    private static final Logger logger = LoggerFactory.getLogger(MessagePublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Value("${rabbitmq.exchange.video:video.exchange}")
    private String videoExchange;

    @Value("${rabbitmq.routing.key.encoding:video.encoding}")
    private String encodingRoutingKey;

    public void publishVideoUploadedMessage(Video video) {
        try {
            Map<String, Object> message = createVideoMessage(video);
            
            rabbitTemplate.convertAndSend(
                videoExchange, 
                encodingRoutingKey, 
                objectMapper.writeValueAsString(message)
            );
            
            logger.info("Video upload message sent: videoId={}, exchange={}, routingKey={}", 
                       video.getId(), videoExchange, encodingRoutingKey);
            
        } catch (Exception e) {
            logger.error("Error sending video upload message: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send message to queue", e);
        }
    }

    private Map<String, Object> createVideoMessage(Video video) {
        Map<String, Object> message = new HashMap<>();
        message.put("videoId", video.getId());
        message.put("userId", video.getUserId());
        message.put("title", video.getTitle());
        message.put("originalFilename", video.getOriginalFilename());
        message.put("s3Key", video.getS3Key());
        message.put("fileSize", video.getFileSize());
        message.put("mimeType", video.getMimeType());
        message.put("status", video.getStatus().toString());
        message.put("uploadedAt", video.getUploadedAt().toString());
        message.put("timestamp", System.currentTimeMillis());
        
        return message;
    }
} 