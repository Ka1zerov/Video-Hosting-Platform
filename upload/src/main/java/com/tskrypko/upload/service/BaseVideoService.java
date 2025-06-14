package com.tskrypko.upload.service;

import com.tskrypko.upload.dto.UploadResponse;
import com.tskrypko.upload.model.Video;
import com.tskrypko.upload.model.VideoStatus;
import com.tskrypko.upload.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public abstract class BaseVideoService {

    private static final Logger logger = LoggerFactory.getLogger(BaseVideoService.class);

    // Common constants
    protected static final List<String> ALLOWED_VIDEO_TYPES = Arrays.asList(
            "video/mp4", "video/avi", "video/mov", "video/wmv", "video/flv",
            "video/webm", "video/mkv", "video/m4v"
    );

    protected static final long MAX_FILE_SIZE = 2L * 1024 * 1024 * 1024; // 2GB
    protected static final long MIN_MULTIPART_SIZE = 5L * 1024 * 1024; // 5MB

    protected final VideoRepository videoRepository;
    protected final MessagePublisher messagePublisher;

    /**
     * Common video file validation
     */
    protected void validateVideoFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds maximum allowed size (2GB)");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_VIDEO_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Unsupported file type. Allowed: " +
                    String.join(", ", ALLOWED_VIDEO_TYPES));
        }

        String filename = file.getOriginalFilename();
        if (filename == null || filename.trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid filename");
        }
    }

    /**
     * Common video metadata validation
     */
    protected void validateVideoMetadata(String title, String description, String originalFilename, 
                                       Long fileSize, String mimeType) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Title is required");
        }

        if (fileSize == null || fileSize <= 0) {
            throw new IllegalArgumentException("Invalid file size");
        }

        if (fileSize > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds maximum allowed size (2GB)");
        }

        if (mimeType == null || !ALLOWED_VIDEO_TYPES.contains(mimeType.toLowerCase())) {
            throw new IllegalArgumentException("Unsupported file type. Allowed: " +
                    String.join(", ", ALLOWED_VIDEO_TYPES));
        }

        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid filename");
        }
    }

    /**
     * Create video record with common fields
     */
    protected Video createVideoRecord(String title, String description, String originalFilename,
                                    Long fileSize, String mimeType, String userId, String s3Key) {
        Video video = new Video();
        video.setTitle(title);
        video.setDescription(description);
        video.setOriginalFilename(originalFilename);
        video.setFileSize(fileSize);
        video.setMimeType(mimeType);
        video.setUserId(userId);
        video.setS3Key(s3Key);
        video.setStatus(VideoStatus.UPLOADED);
        return video;
    }

    /**
     * Send video to encoding queue - SAFELY (without affecting transaction)
     * This method should be called AFTER transaction is committed to prevent data inconsistency
     */
    protected void sendToEncodingQueueSafely(Video video, String uploadType) {
        try {
            messagePublisher.publishVideoUploadedMessage(video);
            logger.info("{} upload message sent to queue: videoId={}", uploadType, video.getId());
        } catch (Exception e) {
            logger.error("CRITICAL: Failed to send {} upload message to queue - " +
                        "manual intervention may be required for videoId={}: {}", 
                        uploadType, video.getId(), e.getMessage(), e);
            // TODO: Implement retry mechanism or dead letter queue
            // For now, we don't throw exception to avoid breaking the upload flow
        }
    }

    /**
     * Send video to encoding queue - STRICTLY (affects transaction)
     * Only use this if you want message sending failure to rollback the transaction
     */
    protected void sendToEncodingQueueStrictly(Video video, String uploadType) {
        try {
            messagePublisher.publishVideoUploadedMessage(video);
            logger.info("{} upload message sent to queue: videoId={}", uploadType, video.getId());
        } catch (Exception e) {
            logger.error("Error sending {} upload message to queue - transaction will be rolled back: {}", 
                        uploadType, e.getMessage(), e);
            throw new RuntimeException("Failed to send message to queue", e);
        }
    }

    /**
     * @deprecated Use sendToEncodingQueueSafely or sendToEncodingQueueStrictly instead
     */
    @Deprecated
    protected void sendToEncodingQueue(Video video, String uploadType) {
        sendToEncodingQueueSafely(video, uploadType);
    }

    /**
     * Create standardized upload response
     */
    protected UploadResponse createUploadResponse(Video video, String message) {
        return new UploadResponse(video, message);
    }

    /**
     * Generate unique S3 key
     */
    protected String generateUniqueKey(String userId, String fileExtension, String prefix) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String uuid = UUID.randomUUID().toString();
        return String.format("%s%s/%s_%s%s", prefix, userId, timestamp, uuid, fileExtension);
    }

    /**
     * Extract file extension from filename
     */
    protected String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }
} 