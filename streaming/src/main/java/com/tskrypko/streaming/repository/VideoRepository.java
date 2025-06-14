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
import java.util.UUID;

@Repository
public interface VideoRepository extends JpaRepository<Video, UUID> {
    
    /**
     * Find video by ID only if it's ready for streaming and not deleted
     */
    Optional<Video> findByIdAndStatusAndDeletedAtIsNull(UUID id, VideoStatus status);
    
    /**
     * Find video by ID if not deleted
     */
    Optional<Video> findByIdAndDeletedAtIsNull(UUID id);
    
    /**
     * Find videos that are ready for streaming
     */
    Page<Video> findByStatusAndDeletedAtIsNullOrderByCreatedAtDesc(VideoStatus status, Pageable pageable);
    
    /**
     * Find videos by user ID
     */
    Page<Video> findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(String userId, Pageable pageable);
    
    /**
     * Find videos by user ID and status
     */
    Page<Video> findByUserIdAndStatusAndDeletedAtIsNullOrderByCreatedAtDesc(String userId, VideoStatus status, Pageable pageable);
    
    /**
     * Search videos by title (case-insensitive)
     */
    @Query("SELECT v FROM Video v WHERE LOWER(v.title) LIKE LOWER(CONCAT('%', :title, '%')) AND v.status = :status AND v.deletedAt IS NULL")
    Page<Video> findByTitleContainingIgnoreCaseAndStatusAndDeletedAtIsNull(@Param("title") String title, @Param("status") VideoStatus status, Pageable pageable);
    
    /**
     * Get top viewed videos
     */
    Page<Video> findByStatusAndDeletedAtIsNullOrderByViewsCountDescCreatedAtDesc(VideoStatus status, Pageable pageable);
    
    /**
     * Get recently accessed videos
     */
    Page<Video> findByStatusAndLastAccessedIsNotNullAndDeletedAtIsNullOrderByLastAccessedDesc(VideoStatus status, Pageable pageable);
    
    /**
     * Update view count and last accessed time
     */
    @Modifying
    @Query("UPDATE Video v SET v.viewsCount = v.viewsCount + 1, v.lastAccessed = :accessTime WHERE v.id = :videoId")
    void incrementViewCount(@Param("videoId") UUID videoId, @Param("accessTime") LocalDateTime accessTime);
    
    /**
     * Find videos with HLS manifest URLs
     */
    List<Video> findByStatusAndHlsManifestUrlIsNotNullAndDeletedAtIsNull(VideoStatus status);
    
    /**
     * Find videos with DASH manifest URLs
     */
    List<Video> findByStatusAndDashManifestUrlIsNotNullAndDeletedAtIsNull(VideoStatus status);
    
    /**
     * Count videos by status (excluding deleted)
     */
    long countByStatusAndDeletedAtIsNull(VideoStatus status);
    
    /**
     * Count videos by user (excluding deleted)
     */
    long countByUserIdAndDeletedAtIsNull(String userId);
    
    /**
     * Find videos created after specific date
     */
    List<Video> findByStatusAndCreatedAtAfterAndDeletedAtIsNullOrderByCreatedAtDesc(VideoStatus status, LocalDateTime date);
} 