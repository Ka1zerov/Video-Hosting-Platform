package com.tskrypko.streaming.repository;

import com.tskrypko.streaming.model.ViewSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ViewSessionRepository extends JpaRepository<ViewSession, UUID> {

    /**
     * Find sessions by video ID
     */
    List<ViewSession> findByVideoIdOrderByStartedAtDesc(UUID videoId);
    
    Page<ViewSession> findByVideoIdOrderByStartedAtDesc(UUID videoId, Pageable pageable);

    /**
     * Find sessions by user ID
     */
    List<ViewSession> findByUserIdOrderByStartedAtDesc(String userId);
    
    Page<ViewSession> findByUserIdOrderByStartedAtDesc(String userId, Pageable pageable);

    /**
     * Find session by session ID
     */
    Optional<ViewSession> findBySessionId(String sessionId);

    /**
     * Find active sessions (not ended)
     */
    List<ViewSession> findByEndedAtIsNullOrderByLastHeartbeatDesc();

    /**
     * Find sessions by IP address
     */
    List<ViewSession> findByIpAddressOrderByStartedAtDesc(String ipAddress);

    /**
     * Count total sessions for a video
     */
    long countByVideoId(UUID videoId);

    /**
     * Count unique users for a video (excluding anonymous)
     */
    @Query("SELECT COUNT(DISTINCT vs.userId) FROM ViewSession vs WHERE vs.videoId = :videoId AND vs.userId IS NOT NULL")
    long countUniqueUsersForVideo(@Param("videoId") UUID videoId);

    /**
     * Get unique viewers count for video (by IP)
     */
    @Query("SELECT COUNT(DISTINCT vs.ipAddress) FROM ViewSession vs WHERE vs.videoId = :videoId")
    Long getUniqueViewersForVideo(@Param("videoId") UUID videoId);

    /**
     * Get total watch time for a video
     */
    @Query("SELECT COALESCE(SUM(vs.watchDuration), 0) FROM ViewSession vs WHERE vs.videoId = :videoId")
    Long getTotalWatchTimeForVideo(@Param("videoId") UUID videoId);

    /**
     * Get completion rate for video
     */
    @Query("SELECT (COUNT(vs) * 100.0 / NULLIF((SELECT COUNT(vs2) FROM ViewSession vs2 WHERE vs2.videoId = :videoId), 0)) " +
           "FROM ViewSession vs WHERE vs.videoId = :videoId AND vs.isComplete = true")
    Double getCompletionRateForVideo(@Param("videoId") UUID videoId);

    /**
     * Find sessions within date range
     */
    List<ViewSession> findByStartedAtBetweenOrderByStartedAtDesc(LocalDateTime start, LocalDateTime end);

    /**
     * Find completed sessions (watched to the end)
     */
    List<ViewSession> findByVideoIdAndIsCompleteOrderByStartedAtDesc(UUID videoId, Boolean isComplete);

    /**
     * Find sessions that need heartbeat cleanup (old active sessions)
     */
    @Query("SELECT vs FROM ViewSession vs WHERE vs.endedAt IS NULL AND vs.lastHeartbeat < :cutoffTime")
    List<ViewSession> findStaleActiveSessions(@Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * Get analytics data - sessions by date
     */
    @Query("SELECT DATE(vs.startedAt) as date, COUNT(vs) as count FROM ViewSession vs WHERE vs.videoId = :videoId GROUP BY DATE(vs.startedAt) ORDER BY date DESC")
    List<Object[]> getSessionsCountByDate(@Param("videoId") UUID videoId);
} 