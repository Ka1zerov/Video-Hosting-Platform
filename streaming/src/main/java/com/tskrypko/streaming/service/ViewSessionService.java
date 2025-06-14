package com.tskrypko.streaming.service;

import com.tskrypko.streaming.dto.ViewSessionRequest;
import com.tskrypko.streaming.exception.SessionAccessDeniedException;
import com.tskrypko.streaming.model.StreamQuality;
import com.tskrypko.streaming.model.ViewSession;
import com.tskrypko.streaming.repository.ViewSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ViewSessionService {

    private final ViewSessionRepository viewSessionRepository;
    private final VideoStreamingService videoStreamingService;
    private final CurrentUserService currentUserService;

    /**
     * Start a new viewing session
     */
    @Transactional
    public ViewSession startViewSession(UUID videoId, String userId, String ipAddress, String userAgent) {
        log.info("Starting view session for video: {} by user: {}", videoId, userId);

        String sessionId = UUID.randomUUID().toString();
        
        ViewSession session = new ViewSession(videoId, sessionId, ipAddress);
        session.setUserId(userId);
        session.setUserAgent(userAgent);
        
        ViewSession savedSession = viewSessionRepository.save(session);
        
        // Increment video view count asynchronously
        incrementVideoViewCountAsync(videoId);
        
        log.debug("Started view session: {} for video: {}", sessionId, videoId);
        return savedSession;
    }

    /**
     * Update viewing session with current progress
     */
    @Transactional
    public void updateViewSession(ViewSessionRequest request) {
        log.debug("Updating view session: {}", request.getSessionId());

        try {
            Optional<ViewSession> sessionOpt = viewSessionRepository.findBySessionId(request.getSessionId());
            if (sessionOpt.isPresent()) {
                ViewSession session = sessionOpt.get();
                session.setLastHeartbeat(LocalDateTime.now());
                session.setWatchDuration(request.getWatchDuration());
                session.setMaxPosition(request.getCurrentPosition());
                
                // Update quality if provided
                if (request.getQuality() != null) {
                    StreamQuality quality = parseQuality(request.getQuality());
                    session.setQuality(quality);
                }
                
                viewSessionRepository.save(session);
            }
            
        } catch (Exception e) {
            log.error("Error updating view session: {}", request.getSessionId(), e);
        }
    }

    /**
     * End viewing session
     */
    @Transactional
    public void endViewSession(String sessionId, boolean isComplete) {
        log.debug("Ending view session: {}", sessionId);

        try {
            Optional<ViewSession> sessionOpt = viewSessionRepository.findBySessionId(sessionId);
            if (sessionOpt.isPresent()) {
                ViewSession session = sessionOpt.get();
                session.setEndedAt(LocalDateTime.now());
                session.setIsComplete(isComplete);
                viewSessionRepository.save(session);
            }
            
        } catch (Exception e) {
            log.error("Error ending view session: {}", sessionId, e);
        }
    }

    /**
     * Get active session by session ID
     */
    @Transactional(readOnly = true)
    public Optional<ViewSession> getActiveSession(String sessionId) {
        Optional<ViewSession> sessionOpt = viewSessionRepository.findBySessionId(sessionId);
        return sessionOpt.filter(session -> session.getEndedAt() == null);
    }

    /**
     * Get viewing sessions for a video (with access control)
     */
    @Transactional(readOnly = true)
    public Page<ViewSession> getVideoSessions(UUID videoId, Pageable pageable) {
        // TODO: Add access control - only video owner or admin can view sessions
        String currentUserId = currentUserService.getCurrentUserIdOrNull();
        log.debug("Getting sessions for video: {} by user: {}", videoId, currentUserId);
        
        return viewSessionRepository.findByVideoIdOrderByStartedAtDesc(videoId, pageable);
    }

    /**
     * Get viewing sessions for a user (with access control)
     */
    @Transactional(readOnly = true)
    public Page<ViewSession> getUserSessions(String userId, Pageable pageable) {
        // Verify that current user is requesting their own sessions
        String currentUserId = currentUserService.getCurrentUserIdOrNull();
        if (currentUserId == null || !currentUserId.equals(userId)) {
            throw new SessionAccessDeniedException("Cannot access sessions for user: " + userId);
        }
        
        return viewSessionRepository.findByUserIdOrderByStartedAtDesc(userId, pageable);
    }

    /**
     * Get total watch time for a video
     */
    @Transactional(readOnly = true)
    public Long getTotalWatchTime(UUID videoId) {
        return viewSessionRepository.getTotalWatchTimeForVideo(videoId);
    }

    /**
     * Get unique viewers count for a video
     */
    @Transactional(readOnly = true)
    public Long getUniqueViewersCount(UUID videoId) {
        return viewSessionRepository.getUniqueViewersForVideo(videoId);
    }

    /**
     * Get completion rate for a video
     */
    @Transactional(readOnly = true)
    public Double getCompletionRate(UUID videoId) {
        Double rate = viewSessionRepository.getCompletionRateForVideo(videoId);
        return rate != null ? rate : 0.0;
    }

    /**
     * Get all active sessions (admin only)
     */
    @Transactional(readOnly = true)
    public List<ViewSession> getActiveSessions() {
        // TODO: Add admin role check when roles are implemented
        String currentUserId = currentUserService.getCurrentUserIdOrNull();
        log.debug("Getting active sessions requested by user: {}", currentUserId);
        
        return viewSessionRepository.findByEndedAtIsNullOrderByLastHeartbeatDesc();
    }

    /**
     * Cleanup stale sessions (scheduled task)
     */
    @Scheduled(fixedRate = 300000) // Run every 5 minutes
    @Transactional
    public void cleanupStaleSessions() {
        try {
            LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(30); // 30 minutes timeout
            List<ViewSession> staleSessions = viewSessionRepository.findStaleActiveSessions(cutoffTime);
            
            if (!staleSessions.isEmpty()) {
                log.info("Found {} stale sessions to cleanup", staleSessions.size());
                
                for (ViewSession session : staleSessions) {
                    endViewSession(session.getSessionId(), false);
                }
            }
            
        } catch (Exception e) {
            log.error("Error during stale sessions cleanup", e);
        }
    }

    /**
     * Get viewing analytics for a video
     */
    @Transactional(readOnly = true)
    public VideoAnalytics getVideoAnalytics(UUID videoId) {
        VideoAnalytics analytics = new VideoAnalytics();
        analytics.setVideoId(videoId);
        analytics.setTotalWatchTime(getTotalWatchTime(videoId));
        analytics.setUniqueViewers(getUniqueViewersCount(videoId));
        analytics.setCompletionRate(getCompletionRate(videoId));
        
        // Get recent sessions count (last 24 hours)
        LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
        List<ViewSession> recentSessions = viewSessionRepository.findByStartedAtBetweenOrderByStartedAtDesc(
            yesterday, LocalDateTime.now()
        );
        analytics.setRecentViewsCount((long) recentSessions.size());
        
        return analytics;
    }

    /**
     * Increment video view count asynchronously
     */
    @Async
    public void incrementVideoViewCountAsync(UUID videoId) {
        try {
            videoStreamingService.incrementViewCount(videoId);
        } catch (Exception e) {
            log.error("Error incrementing view count for video: {}", videoId, e);
        }
    }

    /**
     * Parse quality string to enum
     */
    private StreamQuality parseQuality(String qualityStr) {
        if (qualityStr == null) {
            return StreamQuality.AUTO;
        }
        
        try {
            switch (qualityStr.toLowerCase()) {
                case "480p":
                    return StreamQuality.Q_480P;
                case "720p":
                    return StreamQuality.Q_720P;
                case "1080p":
                    return StreamQuality.Q_1080P;
                default:
                    return StreamQuality.AUTO;
            }
        } catch (Exception e) {
            log.warn("Invalid quality string: {}", qualityStr);
            return StreamQuality.AUTO;
        }
    }

    /**
     * Analytics data class
     */
    public static class VideoAnalytics {
        private UUID videoId;
        private Long totalWatchTime;
        private Long uniqueViewers;
        private Double completionRate;
        private Long recentViewsCount;

        // Getters and setters
        public UUID getVideoId() { return videoId; }
        public void setVideoId(UUID videoId) { this.videoId = videoId; }

        public Long getTotalWatchTime() { return totalWatchTime; }
        public void setTotalWatchTime(Long totalWatchTime) { this.totalWatchTime = totalWatchTime; }

        public Long getUniqueViewers() { return uniqueViewers; }
        public void setUniqueViewers(Long uniqueViewers) { this.uniqueViewers = uniqueViewers; }

        public Double getCompletionRate() { return completionRate; }
        public void setCompletionRate(Double completionRate) { this.completionRate = completionRate; }

        public Long getRecentViewsCount() { return recentViewsCount; }
        public void setRecentViewsCount(Long recentViewsCount) { this.recentViewsCount = recentViewsCount; }
    }
} 