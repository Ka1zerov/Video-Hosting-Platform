package com.tskrypko.metadata.repository;

import com.tskrypko.metadata.model.Video;
import com.tskrypko.metadata.model.VideoStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface VideoRepository extends JpaRepository<Video, UUID> {

    /**
     * Find all ready videos (pageable) - основной метод для MVP
     */
    Page<Video> findByStatusAndDeletedAtIsNullOrderByUploadedAtDesc(VideoStatus status, Pageable pageable);

    /**
     * Find video by ID if ready and not deleted
     */
    Optional<Video> findByIdAndStatusAndDeletedAtIsNull(UUID id, VideoStatus status);

    /**
     * Search videos by title
     */
    @Query("SELECT v FROM Video v WHERE LOWER(v.title) LIKE LOWER(CONCAT('%', :title, '%')) AND v.status = :status AND v.deletedAt IS NULL ORDER BY v.uploadedAt DESC")
    Page<Video> searchByTitleAndStatus(@Param("title") String title, @Param("status") VideoStatus status, Pageable pageable);

    /**
     * Get popular videos (by views)
     */
    Page<Video> findByStatusAndDeletedAtIsNullOrderByViewsCountDescUploadedAtDesc(VideoStatus status, Pageable pageable);

    /**
     * Get recently accessed videos
     */
    Page<Video> findByStatusAndDeletedAtIsNullAndLastAccessedIsNotNullOrderByLastAccessedDesc(VideoStatus status, Pageable pageable);
} 