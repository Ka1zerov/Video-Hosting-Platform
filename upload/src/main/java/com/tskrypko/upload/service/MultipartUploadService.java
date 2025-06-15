package com.tskrypko.upload.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tskrypko.upload.dto.*;
import com.tskrypko.upload.model.MultipartUploadSession;
import com.tskrypko.upload.model.Video;
import com.tskrypko.upload.repository.VideoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class MultipartUploadService extends BaseVideoService {

    private static final Logger logger = LoggerFactory.getLogger(MultipartUploadService.class);

    // Minimum part size - 5 MB (except for the last part)
    private static final long MIN_PART_SIZE = MIN_MULTIPART_SIZE;

    // Maximum number of parts in S3 multipart upload
    private static final int MAX_PARTS = 10000;

    // TTL for sessions in Redis (24 hours)
    private static final long SESSION_TTL_HOURS = 24;

    private final AmazonS3 amazonS3;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final MultipartCleanupService cleanupService;

    @Value("${aws.s3.bucket.name}")
    private String bucketName;

    @Value("${aws.s3.bucket.prefix:videos/}")
    private String keyPrefix;

    public MultipartUploadService(VideoRepository videoRepository, MessagePublisher messagePublisher,
                                AmazonS3 amazonS3, RedisTemplate<String, String> redisTemplate,
                                ObjectMapper objectMapper, MultipartCleanupService cleanupService) {
        super(videoRepository, messagePublisher);
        this.amazonS3 = amazonS3;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.cleanupService = cleanupService;
    }

    /**
     * Initialize multipart upload - using unified DTO
     */
    public MultipartUploadResponse initiateMultipartUpload(VideoUploadRequest request, String userId) {
        logger.info("Initiating multipart upload for user {}: {}", userId, request.getTitle());

        // Use inherited validation for metadata
        validateVideoMetadata(request.getTitle(), request.getDescription(),
                            request.getOriginalFilename(), request.getFileSize(), request.getMimeType());

        // Additional multipart-specific validation
        validateMultipartRequest(request);

        // Generate unique key using inherited method
        String s3Key = generateUniqueKey(userId, getFileExtension(request.getOriginalFilename()), keyPrefix);

        // Initialize multipart upload in S3
        InitiateMultipartUploadRequest s3Request = new InitiateMultipartUploadRequest(bucketName, s3Key);

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(request.getMimeType());
        metadata.addUserMetadata("original-filename", request.getOriginalFilename());
        metadata.addUserMetadata("user-id", userId);
        metadata.addUserMetadata("title", request.getTitle());

        s3Request.setObjectMetadata(metadata);

        InitiateMultipartUploadResult s3Result = amazonS3.initiateMultipartUpload(s3Request);
        String uploadId = s3Result.getUploadId();

        // Calculate part size and total parts count
        long partSize = calculateOptimalPartSize(request.getFileSize());
        int totalParts = (int) Math.ceil((double) request.getFileSize() / partSize);

        // Create multipart upload session
        MultipartUploadSession session = new MultipartUploadSession();
        session.setUploadId(uploadId);
        session.setS3Key(s3Key);
        session.setUserId(userId);
        session.setTitle(request.getTitle());
        session.setDescription(request.getDescription());
        session.setOriginalFilename(request.getOriginalFilename());
        session.setFileSize(request.getFileSize());
        session.setMimeType(request.getMimeType());
        session.setTotalParts(totalParts);
        session.setPartSize(partSize);
        session.setCreatedAt(LocalDateTime.now());
        session.setExpiresAt(LocalDateTime.now().plusHours(SESSION_TTL_HOURS));

        // Save session to Redis
        saveSessionToRedis(uploadId, session);

        logger.info("Multipart upload initiated: uploadId={}, s3Key={}, totalParts={}, partSize={}",
                uploadId, s3Key, totalParts, partSize);

        return new MultipartUploadResponse(
                uploadId,
                s3Key,
                "Multipart upload initiated successfully",
                totalParts,
                partSize
        );
    }

    // Backward compatibility method
    public MultipartUploadResponse initiateMultipartUpload(MultipartUploadRequest request, String userId) {
        // Convert to unified DTO
        VideoUploadRequest unifiedRequest = new VideoUploadRequest(
            request.getTitle(),
            request.getDescription(),
            request.getOriginalFilename(),
            request.getFileSize(),
            request.getMimeType()
        );

        return initiateMultipartUpload(unifiedRequest, userId);
    }

    /**
     * Upload individual chunk with timeout check
     */
    public ChunkUploadResponse uploadChunk(String uploadId, Integer partNumber, MultipartFile chunk) {
        logger.info("Uploading chunk: uploadId={}, partNumber={}, size={}",
                uploadId, partNumber, chunk.getSize());

        // Check if session is expired
        if (cleanupService.isSessionExpired(uploadId)) {
            logger.warn("Upload session expired: {}", uploadId);
            cleanupService.cleanupSession(uploadId);
            throw new IllegalArgumentException("Upload session expired: " + uploadId);
        }

        // Get session from Redis
        MultipartUploadSession session = getSessionFromRedis(uploadId);
        if (session == null) {
            throw new IllegalArgumentException("Upload session not found or expired: " + uploadId);
        }

        // Validate part number
        if (partNumber < 1 || partNumber > session.getTotalParts()) {
            throw new IllegalArgumentException("Invalid part number: " + partNumber +
                    ". Valid range: 1-" + session.getTotalParts());
        }

        // Validate chunk size
        validateChunkSize(chunk, partNumber, session);

        try {
            // Upload part to S3
            UploadPartRequest uploadPartRequest = new UploadPartRequest()
                    .withBucketName(bucketName)
                    .withKey(session.getS3Key())
                    .withUploadId(uploadId)
                    .withPartNumber(partNumber)
                    .withInputStream(chunk.getInputStream())
                    .withPartSize(chunk.getSize());

            UploadPartResult uploadPartResult = amazonS3.uploadPart(uploadPartRequest);
            String etag = uploadPartResult.getETag();

            // Update session
            session.addUploadedPart(partNumber, etag);
            saveSessionToRedis(uploadId, session);

            logger.info("Chunk uploaded successfully: uploadId={}, partNumber={}, etag={}, progress={}",
                    uploadId, partNumber, etag, String.format("%.2f%%", session.getProgressPercentage()));

            return new ChunkUploadResponse(
                    etag,
                    partNumber,
                    "Chunk uploaded successfully",
                    session.getUploadedPartsCount(),
                    session.getTotalParts()
            );

        } catch (IOException e) {
            logger.error("Error uploading chunk: uploadId={}, partNumber={}, error={}",
                    uploadId, partNumber, e.getMessage(), e);
            throw new RuntimeException("Failed to upload chunk", e);
        }
    }

    /**
     * Complete multipart upload with improved transaction handling
     */
    @Transactional
    public UploadResponse completeMultipartUpload(String uploadId) {
        logger.info("Completing multipart upload: {}", uploadId);

        // Check if session is expired
        if (cleanupService.isSessionExpired(uploadId)) {
            logger.warn("Upload session expired during completion: {}", uploadId);
            cleanupService.cleanupSession(uploadId);
            throw new IllegalArgumentException("Upload session expired: " + uploadId);
        }

        // Get session
        MultipartUploadSession session = getSessionFromRedis(uploadId);
        if (session == null) {
            throw new IllegalArgumentException("Upload session not found or expired: " + uploadId);
        }

        // Check that all parts are uploaded
        if (!session.isCompleted()) {
            throw new IllegalArgumentException("Not all parts uploaded. Progress: " +
                    session.getUploadedPartsCount() + "/" + session.getTotalParts());
        }

        Video savedVideo = null;

        try {
            // Prepare parts list for completion
            List<PartETag> partETags = new ArrayList<>();
            for (int i = 1; i <= session.getTotalParts(); i++) {
                String etag = session.getUploadedParts().get(i);
                if (etag == null) {
                    throw new IllegalStateException("Missing part: " + i);
                }
                partETags.add(new PartETag(i, etag));
            }

            // Complete multipart upload in S3
            CompleteMultipartUploadRequest completeRequest = new CompleteMultipartUploadRequest(
                    bucketName, session.getS3Key(), uploadId, partETags);

            CompleteMultipartUploadResult completeResult = amazonS3.completeMultipartUpload(completeRequest);

            // Create video record using inherited method
            Video video = createVideoRecord(
                session.getTitle(),
                session.getDescription(),
                session.getOriginalFilename(),
                session.getFileSize(),
                session.getMimeType(),
                session.getUserId(),
                session.getS3Key()
            );

            savedVideo = videoRepository.save(video);

            // Delete session from Redis (within transaction)
            deleteSessionFromRedis(uploadId);

            logger.info("Multipart upload completed successfully: uploadId={}, videoId={}, s3Key={}",
                    uploadId, savedVideo.getId(), session.getS3Key());

        } catch (Exception e) {
            logger.error("Error completing multipart upload: uploadId={}, error={}",
                    uploadId, e.getMessage(), e);

            // In case of error, try to abort multipart upload
            try {
                abortMultipartUpload(uploadId);
            } catch (Exception abortEx) {
                logger.error("Failed to abort multipart upload after completion error: {}",
                        abortEx.getMessage(), abortEx);
            }

            throw new RuntimeException("Failed to complete multipart upload", e);
        }

        // IMPORTANT: Send message AFTER transaction is committed
        // This prevents data inconsistency if message sending fails
        sendToEncodingQueueSafely(savedVideo, "Multipart");

        // Use inherited method to create response
        return createUploadResponse(savedVideo, "Video successfully uploaded via multipart upload");
    }

    /**
     * Abort multipart upload
     */
    public void abortMultipartUpload(String uploadId) {
        logger.info("Aborting multipart upload: {}", uploadId);

        MultipartUploadSession session = getSessionFromRedis(uploadId);
        if (session != null) {
            try {
                // Abort in S3
                AbortMultipartUploadRequest abortRequest = new AbortMultipartUploadRequest(
                        bucketName, session.getS3Key(), uploadId);
                amazonS3.abortMultipartUpload(abortRequest);

                logger.info("Multipart upload aborted in S3: uploadId={}, s3Key={}",
                        uploadId, session.getS3Key());
            } catch (Exception e) {
                logger.error("Error aborting multipart upload in S3: {}", e.getMessage(), e);
            }

            // Delete session from Redis
            deleteSessionFromRedis(uploadId);
        }
    }

    /**
     * Get multipart upload status
     */
    public MultipartUploadSession getUploadStatus(String uploadId) {
        return getSessionFromRedis(uploadId);
    }

    // Private methods - only multipart-specific logic

    private void validateMultipartRequest(VideoUploadRequest request) {
        if (request.getFileSize() < MIN_PART_SIZE) {
            throw new IllegalArgumentException("File too small for multipart upload. Minimum: " +
                    (MIN_PART_SIZE / 1024 / 1024) + " MB");
        }
    }

    private void validateChunkSize(MultipartFile chunk, Integer partNumber, MultipartUploadSession session) {
        long chunkSize = chunk.getSize();

        // All parts except the last one must be at least 5MB
        if (partNumber < session.getTotalParts() && chunkSize < MIN_PART_SIZE) {
            throw new IllegalArgumentException("Part size too small. Minimum: " +
                    (MIN_PART_SIZE / 1024 / 1024) + " MB");
        }

        // Check that part size doesn't exceed expected size
        long expectedSize = session.getPartSize();
        if (partNumber.equals(session.getTotalParts())) {
            // Last part can be smaller
            long remainingSize = session.getFileSize() % session.getPartSize();
            if (remainingSize > 0) {
                expectedSize = remainingSize;
            }
        }

        if (chunkSize > expectedSize + 1024) { // Allow small tolerance
            throw new IllegalArgumentException("Part size too large. Expected: " + expectedSize +
                    ", got: " + chunkSize);
        }
    }

    private long calculateOptimalPartSize(long fileSize) {
        // Calculate optimal part size
        long partSize = Math.max(MIN_PART_SIZE, fileSize / MAX_PARTS);

        // Round to nearest MB
        partSize = ((partSize + 1024 * 1024 - 1) / (1024 * 1024)) * 1024 * 1024;

        return partSize;
    }

    private void saveSessionToRedis(String uploadId, MultipartUploadSession session) {
        try {
            String sessionJson = objectMapper.writeValueAsString(session);
            String key = "multipart:session:" + uploadId; // Changed key format for cleanup service
            redisTemplate.opsForValue().set(key, sessionJson, SESSION_TTL_HOURS, TimeUnit.HOURS);
        } catch (JsonProcessingException e) {
            logger.error("Error saving session to Redis: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save upload session", e);
        }
    }

    private MultipartUploadSession getSessionFromRedis(String uploadId) {
        try {
            String key = "multipart:session:" + uploadId; // Changed key format for cleanup service
            String sessionJson = redisTemplate.opsForValue().get(key);

            if (sessionJson == null) {
                return null;
            }

            return objectMapper.readValue(sessionJson, MultipartUploadSession.class);
        } catch (JsonProcessingException e) {
            logger.error("Error reading session from Redis: {}", e.getMessage(), e);
            return null;
        }
    }

    private void deleteSessionFromRedis(String uploadId) {
        String key = "multipart:session:" + uploadId; // Changed key format for cleanup service
        redisTemplate.delete(key);
    }
}
