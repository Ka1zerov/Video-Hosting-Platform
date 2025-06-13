package com.tskrypko.streaming.service;

import com.tskrypko.streaming.exception.SessionGenerationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionManagementService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String SESSION_PREFIX = "stream_session:";
    private static final String VIDEO_ACCESS_PREFIX = "video_access:";

    /**
     * Generate unique session ID for video streaming
     */
    public String generateSessionId(Long videoId, String userId, String ipAddress) {
        try {
            String sessionId = UUID.randomUUID().toString();
            
            // Track session info for analytics
            trackStreamSession(sessionId, videoId, userId, ipAddress);
            
            log.debug("Generated session ID {} for video {} by user {}", sessionId, videoId, userId);
            return sessionId;

        } catch (Exception e) {
            log.error("Error generating session ID for video {}", videoId, e);
            throw new SessionGenerationException("Failed to generate session ID", e);
        }
    }

    /**
     * Validate that user has access to video
     */
    public boolean validateVideoAccess(Long videoId, String userId) {
        try {
            // This will be validated in VideoStreamingService
            // based on video public status or ownership
            log.debug("Validating video access for video {} by user {}", videoId, userId);
            return true; // Basic validation, detailed check in VideoStreamingService
            
        } catch (Exception e) {
            log.error("Error validating video access for video {}", videoId, e);
            return false;
        }
    }

    /**
     * Track streaming session for analytics
     */
    private void trackStreamSession(String sessionId, Long videoId, String userId, String ipAddress) {
        try {
            String sessionKey = SESSION_PREFIX + sessionId;
            StreamSessionInfo sessionInfo = new StreamSessionInfo(videoId, userId, ipAddress, LocalDateTime.now());
            
            // Store session info for 24 hours
            redisTemplate.opsForValue().set(sessionKey, sessionInfo, 24, TimeUnit.HOURS);
            
            // Track video access
            String accessKey = VIDEO_ACCESS_PREFIX + videoId + ":" + sessionId;
            redisTemplate.opsForValue().set(accessKey, LocalDateTime.now(), 24, TimeUnit.HOURS);
            
        } catch (Exception e) {
            log.error("Error tracking stream session {}", sessionId, e);
        }
    }

    /**
     * Get session info
     */
    public StreamSessionInfo getSessionInfo(String sessionId) {
        try {
            String sessionKey = SESSION_PREFIX + sessionId;
            return (StreamSessionInfo) redisTemplate.opsForValue().get(sessionKey);
        } catch (Exception e) {
            log.error("Error getting session info for {}", sessionId, e);
            return null;
        }
    }

    /**
     * End session
     */
    public void endSession(String sessionId) {
        try {
            String sessionKey = SESSION_PREFIX + sessionId;
            redisTemplate.delete(sessionKey);
            log.debug("Ended session: {}", sessionId);
        } catch (Exception e) {
            log.error("Error ending session {}", sessionId, e);
        }
    }

    /**
     * Get active streaming sessions count
     */
    public long getActiveSessionsCount() {
        try {
            return redisTemplate.keys(SESSION_PREFIX + "*").size();
        } catch (Exception e) {
            log.error("Error getting active sessions count", e);
            return 0;
        }
    }

    /**
     * Cleanup expired sessions (manual cleanup if needed)
     */
    public void cleanupExpiredSessions() {
        try {
            // Redis TTL handles automatic cleanup, but we can add manual cleanup here if needed
            log.debug("Session cleanup completed (handled by Redis TTL)");
        } catch (Exception e) {
            log.error("Error during session cleanup", e);
        }
    }

    // Data class for Redis storage
    public static class StreamSessionInfo {
        public Long videoId;
        public String userId;
        public String ipAddress;
        public LocalDateTime startTime;

        public StreamSessionInfo() {}

        public StreamSessionInfo(Long videoId, String userId, String ipAddress, LocalDateTime startTime) {
            this.videoId = videoId;
            this.userId = userId;
            this.ipAddress = ipAddress;
            this.startTime = startTime;
        }
    }
} 