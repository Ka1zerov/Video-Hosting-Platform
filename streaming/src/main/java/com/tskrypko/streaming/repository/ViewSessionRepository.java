package com.tskrypko.streaming.repository;

import com.tskrypko.streaming.model.StreamQuality;
import com.tskrypko.streaming.model.ViewSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ViewSessionRepository extends JpaRepository<ViewSession, Long> {
    
    /**
     * Find active session by session ID
     */
    Optional<ViewSession> findBySessionIdAndEndedAtIsNull(String sessionId);
    
    /**
     * Find sessions for specific video
     */
    Page<ViewSession> findByVideoIdOrderByStartedAtDesc(Long videoId, Pageable pageable);
    
    /**
     * Find sessions for specific user
     */
    Page<ViewSession> findByUserIdOrderByStartedAtDesc(String userId, Pageable pageable);
    
    /**
     * Find active sessions (not ended)
     */
    List<ViewSession> findByEndedAtIsNullOrderByStartedAtDesc();
    
    /**
     * Find stale sessions (no heartbeat for given time)
     */
    @Query("SELECT vs FROM ViewSession vs WHERE vs.endedAt IS NULL AND vs.lastHeartbeat < :cutoffTime")
    List<ViewSession> findStaleSessions(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    /**
     * Update session heartbeat
     */
    @Modifying
    @Query("UPDATE ViewSession vs SET vs.lastHeartbeat = :heartbeat, vs.watchDuration = :duration, vs.maxPosition = :position WHERE vs.sessionId = :sessionId")
    void updateSessionHeartbeat(@Param("sessionId") String sessionId, 
                              @Param("heartbeat") LocalDateTime heartbeat,
                              @Param("duration") Long duration,
                              @Param("position") Long position);
    
    /**
     * Update session quality
     */
    @Modifying
    @Query("UPDATE ViewSession vs SET vs.quality = :quality WHERE vs.sessionId = :sessionId")
    void updateSessionQuality(@Param("sessionId") String sessionId, @Param("quality") StreamQuality quality);
    
    /**
     * End session
     */
    @Modifying
    @Query("UPDATE ViewSession vs SET vs.endedAt = :endTime, vs.isComplete = :isComplete WHERE vs.sessionId = :sessionId")
    void endSession(@Param("sessionId") String sessionId, @Param("endTime") LocalDateTime endTime, @Param("isComplete") Boolean isComplete);
    
    /**
     * Get total watch time for video
     */
    @Query("SELECT COALESCE(SUM(vs.watchDuration), 0) FROM ViewSession vs WHERE vs.videoId = :videoId")
    Long getTotalWatchTimeForVideo(@Param("videoId") Long videoId);
    
    /**
     * Get unique viewers count for video
     */
    @Query("SELECT COUNT(DISTINCT vs.ipAddress) FROM ViewSession vs WHERE vs.videoId = :videoId")
    Long getUniqueViewersForVideo(@Param("videoId") Long videoId);
    
    /**
     * Get session completion rate for video
     */
    @Query("SELECT (COUNT(vs) * 100.0 / NULLIF((SELECT COUNT(vs2) FROM ViewSession vs2 WHERE vs2.videoId = :videoId), 0)) " +
           "FROM ViewSession vs WHERE vs.videoId = :videoId AND vs.isComplete = true")
    Double getCompletionRateForVideo(@Param("videoId") Long videoId);
    
    /**
     * Get sessions within date range
     */
    List<ViewSession> findByStartedAtBetweenOrderByStartedAtDesc(LocalDateTime startDate, LocalDateTime endDate);
} 