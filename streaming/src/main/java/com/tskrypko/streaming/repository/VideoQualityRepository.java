package com.tskrypko.streaming.repository;

import com.tskrypko.streaming.model.EncodingStatus;
import com.tskrypko.streaming.model.VideoQuality;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VideoQualityRepository extends JpaRepository<VideoQuality, UUID> {
    
    /**
     * Find all qualities for a specific video
     */
    List<VideoQuality> findByVideoIdOrderByBitrateDesc(UUID videoId);
    
    /**
     * Find completed qualities for a video
     */
    List<VideoQuality> findByVideoIdAndEncodingStatusOrderByBitrateDesc(UUID videoId, EncodingStatus status);
    
    /**
     * Find a specific quality for a video
     */
    Optional<VideoQuality> findByVideoIdAndQualityName(UUID videoId, String qualityName);
    
    /**
     * Find qualities with HLS playlists
     */
    List<VideoQuality> findByVideoIdAndHlsPlaylistUrlIsNotNullOrderByBitrateAsc(UUID videoId);
    
    /**
     * Find best available quality for video (highest bitrate that's completed)
     */
    @Query("SELECT vq FROM VideoQuality vq WHERE vq.video.id = :videoId AND vq.encodingStatus = :status ORDER BY vq.bitrate DESC")
    List<VideoQuality> findBestQualityForVideo(@Param("videoId") UUID videoId, @Param("status") EncodingStatus status);
    
    /**
     * Find qualities in progress
     */
    List<VideoQuality> findByEncodingStatusOrderByCreatedAtAsc(EncodingStatus status);
    
    /**
     * Count completed qualities for video
     */
    long countByVideoIdAndEncodingStatus(UUID videoId, EncodingStatus status);
    
    /**
     * Find qualities by bitrate range
     */
    List<VideoQuality> findByVideoIdAndBitrateBetweenAndEncodingStatusOrderByBitrateAsc(
            UUID videoId, Integer minBitrate, Integer maxBitrate, EncodingStatus status);
    
    /**
     * Find qualities by encoding status
     */
    List<VideoQuality> findByEncodingStatus(EncodingStatus status);
    
    /**
     * Find qualities by video ID and status
     */
    List<VideoQuality> findByVideoIdAndEncodingStatus(UUID videoId, EncodingStatus status);
    
    /**
     * Count qualities by status
     */
    long countByEncodingStatus(EncodingStatus status);
    
    /**
     * Check if video has specific quality ready
     */
    @Query("SELECT COUNT(vq) > 0 FROM VideoQuality vq WHERE vq.videoId = :videoId AND vq.qualityName = :qualityName AND vq.encodingStatus = 'COMPLETED'")
    boolean hasQualityReady(@Param("videoId") UUID videoId, @Param("qualityName") String qualityName);
    
    /**
     * Get available qualities for a video (completed only)
     */
    @Query("SELECT vq FROM VideoQuality vq WHERE vq.videoId = :videoId AND vq.encodingStatus = 'COMPLETED' ORDER BY vq.bitrate DESC")
    List<VideoQuality> getAvailableQualities(@Param("videoId") UUID videoId);
} 