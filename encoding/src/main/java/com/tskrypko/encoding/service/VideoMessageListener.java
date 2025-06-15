package com.tskrypko.encoding.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tskrypko.encoding.model.EncodingJob;
import com.tskrypko.encoding.model.EncodingStatus;
import com.tskrypko.encoding.repository.EncodingJobRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
public class VideoMessageListener {

    private static final Logger logger = LoggerFactory.getLogger(VideoMessageListener.class);

    private final EncodingJobRepository encodingJobRepository;
    private final VideoEncodingService videoEncodingService;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = "${rabbitmq.queue.encoding:video.encoding.queue}")
    @Transactional
    public void handleVideoUploadMessage(String message) {
        logger.info("Received video upload message: {}", message);

        try {
            JsonNode messageNode = objectMapper.readTree(message);

            String videoId = messageNode.get("videoId").asText();
            String userId = messageNode.get("userId").asText();
            String title = messageNode.get("title").asText();
            String originalFilename = messageNode.get("originalFilename").asText();
            String s3Key = messageNode.get("s3Key").asText();
            Long fileSize = messageNode.get("fileSize").asLong();
            String mimeType = messageNode.has("mimeType") ? messageNode.get("mimeType").asText() : null;

            // Check if job already exists
            if (encodingJobRepository.findByVideoId(videoId).isPresent()) {
                logger.warn("Encoding job already exists for video: {}", videoId);
                return;
            }

            // Create encoding job
            EncodingJob job = createEncodingJob(videoId, userId, title, originalFilename, s3Key, fileSize, mimeType);
            EncodingJob savedJob = encodingJobRepository.save(job);

            logger.info("Created encoding job: {}", savedJob.getId());

            // Start encoding process asynchronously after transaction commit
            TransactionSynchronizationManager.registerSynchronization(
                new org.springframework.transaction.support.TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        videoEncodingService.processEncodingJob(savedJob.getId().toString());
                    }
                }
            );

        } catch (Exception e) {
            logger.error("Error processing video upload message: {}", e.getMessage(), e);
        }
    }

    private EncodingJob createEncodingJob(String videoId, String userId, String title,
                                        String originalFilename, String s3Key, Long fileSize, String mimeType) {
        EncodingJob job = new EncodingJob();
        job.setVideoId(UUID.fromString(videoId));
        job.setUserId(userId);
        job.setTitle(title);
        job.setOriginalFilename(originalFilename);
        job.setS3Key(s3Key);
        job.setFileSize(fileSize);
        job.setMimeType(mimeType);
        job.setStatus(EncodingStatus.PENDING);
        job.setRetryCount(0);
        job.setProgress(0);

        return job;
    }
}
