package com.tskrypko.upload.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AbortMultipartUploadRequest;
import com.amazonaws.services.s3.model.ListMultipartUploadsRequest;
import com.amazonaws.services.s3.model.MultipartUpload;
import com.amazonaws.services.s3.model.MultipartUploadListing;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tskrypko.upload.model.MultipartUploadSession;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Service for cleaning up expired multipart uploads and managing timeouts
 */
@Service
@RequiredArgsConstructor
public class MultipartCleanupService {

    private static final Logger logger = LoggerFactory.getLogger(MultipartCleanupService.class);

    private final AmazonS3 amazonS3;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${aws.s3.bucket.name}")
    private String bucketName;

    @Value("${multipart.cleanup.max-age-hours:24}")
    private int maxAgeHours;

    @Value("${multipart.cleanup.enabled:true}")
    private boolean cleanupEnabled;

    /**
     * Scheduled cleanup of expired multipart sessions
     * Runs every hour to clean up expired sessions
     */
    @Scheduled(fixedRate = 3600000) // Every hour
    public void cleanupExpiredSessions() {
        if (!cleanupEnabled) {
            logger.debug("Multipart cleanup is disabled");
            return;
        }

        logger.info("Starting multipart cleanup process");
        
        try {
            int cleanedRedis = cleanupExpiredRedisSessions();
            int cleanedS3 = cleanupExpiredS3Uploads();
            
            logger.info("Multipart cleanup completed: {} Redis sessions, {} S3 uploads", 
                       cleanedRedis, cleanedS3);
        } catch (Exception e) {
            logger.error("Error during multipart cleanup process", e);
        }
    }

    /**
     * Clean up expired sessions in Redis
     */
    private int cleanupExpiredRedisSessions() {
        try {
            Set<String> sessionKeys = redisTemplate.keys("multipart:session:*");
            if (sessionKeys == null || sessionKeys.isEmpty()) {
                return 0;
            }

            int cleaned = 0;
            LocalDateTime now = LocalDateTime.now();

            for (String key : sessionKeys) {
                try {
                    String sessionJson = redisTemplate.opsForValue().get(key);
                    if (sessionJson == null) {
                        continue;
                    }

                    MultipartUploadSession session = objectMapper.readValue(sessionJson, MultipartUploadSession.class);
                    
                    if (session.getExpiresAt() != null && session.getExpiresAt().isBefore(now)) {
                        // Session expired, clean it up
                        String uploadId = key.replace("multipart:session:", "");
                        
                        // Abort S3 multipart upload
                        abortS3MultipartUpload(session.getS3Key(), uploadId);
                        
                        // Remove from Redis
                        redisTemplate.delete(key);
                        
                        cleaned++;
                        logger.info("Cleaned expired session: uploadId={}, s3Key={}", 
                                   uploadId, session.getS3Key());
                    }
                } catch (Exception e) {
                    logger.warn("Failed to process session key {}: {}", key, e.getMessage());
                }
            }

            return cleaned;
        } catch (Exception e) {
            logger.error("Error cleaning up Redis sessions", e);
            return 0;
        }
    }

    /**
     * Clean up expired multipart uploads in S3
     */
    private int cleanupExpiredS3Uploads() {
        try {
            ListMultipartUploadsRequest request = new ListMultipartUploadsRequest(bucketName);
            MultipartUploadListing listing = amazonS3.listMultipartUploads(request);
            
            int cleaned = 0;
            Date cutoffDate = new Date(System.currentTimeMillis() - TimeUnit.HOURS.toMillis(maxAgeHours));

            for (MultipartUpload upload : listing.getMultipartUploads()) {
                if (upload.getInitiated().before(cutoffDate)) {
                    try {
                        abortS3MultipartUpload(upload.getKey(), upload.getUploadId());
                        cleaned++;
                        logger.info("Cleaned expired S3 multipart upload: key={}, uploadId={}, age={}h", 
                                   upload.getKey(), upload.getUploadId(), 
                                   TimeUnit.MILLISECONDS.toHours(System.currentTimeMillis() - upload.getInitiated().getTime()));
                    } catch (Exception e) {
                        logger.warn("Failed to abort S3 multipart upload: key={}, uploadId={}, error={}", 
                                   upload.getKey(), upload.getUploadId(), e.getMessage());
                    }
                }
            }

            return cleaned;
        } catch (Exception e) {
            logger.error("Error cleaning up S3 multipart uploads", e);
            return 0;
        }
    }

    /**
     * Manually cleanup specific session
     */
    public boolean cleanupSession(String uploadId) {
        try {
            String sessionKey = "multipart:session:" + uploadId;
            String sessionJson = redisTemplate.opsForValue().get(sessionKey);
            
            if (sessionJson != null) {
                MultipartUploadSession session = objectMapper.readValue(sessionJson, MultipartUploadSession.class);
                abortS3MultipartUpload(session.getS3Key(), uploadId);
                redisTemplate.delete(sessionKey);
                
                logger.info("Manually cleaned session: uploadId={}, s3Key={}", uploadId, session.getS3Key());
                return true;
            }
        } catch (Exception e) {
            logger.error("Error manually cleaning session {}: {}", uploadId, e.getMessage());
        }
        return false;
    }

    /**
     * Check if session is expired
     */
    public boolean isSessionExpired(String uploadId) {
        try {
            String sessionKey = "multipart:session:" + uploadId;
            String sessionJson = redisTemplate.opsForValue().get(sessionKey);
            
            if (sessionJson == null) {
                return true; // Session doesn't exist
            }

            MultipartUploadSession session = objectMapper.readValue(sessionJson, MultipartUploadSession.class);
            return session.getExpiresAt() != null && session.getExpiresAt().isBefore(LocalDateTime.now());
        } catch (Exception e) {
            logger.error("Error checking session expiry {}: {}", uploadId, e.getMessage());
            return true; // Assume expired on error
        }
    }

    /**
     * Get cleanup statistics
     */
    public CleanupStats getCleanupStats() {
        try {
            Set<String> sessionKeys = redisTemplate.keys("multipart:session:*");
            int totalSessions = sessionKeys != null ? sessionKeys.size() : 0;
            
            ListMultipartUploadsRequest request = new ListMultipartUploadsRequest(bucketName);
            MultipartUploadListing listing = amazonS3.listMultipartUploads(request);
            int totalS3Uploads = listing.getMultipartUploads().size();
            
            return new CleanupStats(totalSessions, totalS3Uploads, maxAgeHours, cleanupEnabled);
        } catch (Exception e) {
            logger.error("Error getting cleanup stats", e);
            return new CleanupStats(0, 0, maxAgeHours, cleanupEnabled);
        }
    }

    private void abortS3MultipartUpload(String s3Key, String uploadId) {
        try {
            AbortMultipartUploadRequest abortRequest = new AbortMultipartUploadRequest(bucketName, s3Key, uploadId);
            amazonS3.abortMultipartUpload(abortRequest);
        } catch (Exception e) {
            logger.warn("Failed to abort S3 multipart upload: key={}, uploadId={}, error={}", 
                       s3Key, uploadId, e.getMessage());
        }
    }

    /**
     * Statistics for cleanup operations
     */
    public static class CleanupStats {
        private final int totalRedisSessions;
        private final int totalS3Uploads;
        private final int maxAgeHours;
        private final boolean cleanupEnabled;

        public CleanupStats(int totalRedisSessions, int totalS3Uploads, int maxAgeHours, boolean cleanupEnabled) {
            this.totalRedisSessions = totalRedisSessions;
            this.totalS3Uploads = totalS3Uploads;
            this.maxAgeHours = maxAgeHours;
            this.cleanupEnabled = cleanupEnabled;
        }

        public int getTotalRedisSessions() { return totalRedisSessions; }
        public int getTotalS3Uploads() { return totalS3Uploads; }
        public int getMaxAgeHours() { return maxAgeHours; }
        public boolean isCleanupEnabled() { return cleanupEnabled; }
    }
} 