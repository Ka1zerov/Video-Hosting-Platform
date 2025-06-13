package com.tskrypko.streaming.repository;

import com.tskrypko.streaming.model.EncodingStatus;
import com.tskrypko.streaming.model.VideoQuality;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VideoQualityRepository extends JpaRepository<VideoQuality, Long> {
    
    /**
     * Find all qualities for a specific video
     */
    List<VideoQuality> findByVideoIdOrderByBitrateAsc(Long videoId);
    
    /**
     * Find completed qualities for a video
     */
    List<VideoQuality> findByVideoIdAndEncodingStatusOrderByBitrateAsc(Long videoId, EncodingStatus status);
    
    /**
     * Find a specific quality for a video
     */
    Optional<VideoQuality> findByVideoIdAndQualityName(Long videoId, String qualityName);
    
    /**
     * Find qualities with HLS playlists
     */
    List<VideoQuality> findByVideoIdAndHlsPlaylistUrlIsNotNullOrderByBitrateAsc(Long videoId);
    
    /**
     * Find best available quality for video (highest bitrate that's completed)
     */
    @Query("SELECT vq FROM VideoQuality vq WHERE vq.video.id = :videoId AND vq.encodingStatus = :status ORDER BY vq.bitrate DESC")
    List<VideoQuality> findBestQualityForVideo(@Param("videoId") Long videoId, @Param("status") EncodingStatus status);
    
    /**
     * Find qualities in progress
     */
    List<VideoQuality> findByEncodingStatusOrderByCreatedAtAsc(EncodingStatus status);
    
    /**
     * Count completed qualities for video
     */
    long countByVideoIdAndEncodingStatus(Long videoId, EncodingStatus status);
    
    /**
     * Find qualities by bitrate range
     */
    List<VideoQuality> findByVideoIdAndBitrateBetweenAndEncodingStatusOrderByBitrateAsc(
            Long videoId, Integer minBitrate, Integer maxBitrate, EncodingStatus status);
} 