package com.tskrypko.streaming.repository;

import com.tskrypko.streaming.model.Video;
import com.tskrypko.streaming.model.VideoStatus;
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
public interface VideoRepository extends JpaRepository<Video, Long> {
    
    /**
     * Find video by ID only if it's ready for streaming
     */
    Optional<Video> findByIdAndStatus(Long id, VideoStatus status);
    
    /**
     * Find videos that are ready for streaming
     */
    Page<Video> findByStatusOrderByCreatedAtDesc(VideoStatus status, Pageable pageable);
    
    /**
     * Find videos by user ID
     */
    Page<Video> findByUserIdAndStatusOrderByCreatedAtDesc(String userId, VideoStatus status, Pageable pageable);
    
    /**
     * Search videos by title (case-insensitive)
     */
    @Query("SELECT v FROM Video v WHERE LOWER(v.title) LIKE LOWER(CONCAT('%', :title, '%')) AND v.status = :status")
    Page<Video> findByTitleContainingIgnoreCaseAndStatus(@Param("title") String title, @Param("status") VideoStatus status, Pageable pageable);
    
    /**
     * Get top viewed videos
     */
    Page<Video> findByStatusOrderByViewsCountDescCreatedAtDesc(VideoStatus status, Pageable pageable);
    
    /**
     * Get recently accessed videos
     */
    Page<Video> findByStatusAndLastAccessedIsNotNullOrderByLastAccessedDesc(VideoStatus status, Pageable pageable);
    
    /**
     * Update view count
     */
    @Modifying
    @Query("UPDATE Video v SET v.viewsCount = v.viewsCount + 1, v.lastAccessed = :accessTime WHERE v.id = :videoId")
    void incrementViewCount(@Param("videoId") Long videoId, @Param("accessTime") LocalDateTime accessTime);
    
    /**
     * Find videos with HLS manifest URLs
     */
    List<Video> findByStatusAndHlsManifestUrlIsNotNull(VideoStatus status);
    
    /**
     * Find videos with DASH manifest URLs
     */
    List<Video> findByStatusAndDashManifestUrlIsNotNull(VideoStatus status);
    
    /**
     * Count videos by status
     */
    long countByStatus(VideoStatus status);
    
    /**
     * Find videos created after specific date
     */
    List<Video> findByStatusAndCreatedAtAfterOrderByCreatedAtDesc(VideoStatus status, LocalDateTime date);
} 