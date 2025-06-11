package com.tskrypko.upload.repository;

import com.tskrypko.upload.model.Video;
import com.tskrypko.upload.model.VideoStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VideoRepository extends JpaRepository<Video, UUID> {

    @Query("SELECT v FROM Video v WHERE v.userId = :userId AND v.deletedAt IS NULL")
    List<Video> findByUserId(@Param("userId") String userId);

    @Query("SELECT v FROM Video v WHERE v.status = :status AND v.deletedAt IS NULL")
    List<Video> findByStatus(@Param("status") VideoStatus status);

    @Query("SELECT v FROM Video v WHERE v.userId = :userId AND v.status = :status AND v.deletedAt IS NULL")
    List<Video> findByUserIdAndStatus(@Param("userId") String userId, @Param("status") VideoStatus status);

    @Query("SELECT v FROM Video v WHERE v.id = :id AND v.userId = :userId AND v.deletedAt IS NULL")
    Optional<Video> findByIdAndUserId(@Param("id") UUID id, @Param("userId") String userId);

    @Query("SELECT v FROM Video v WHERE v.userId = :userId AND v.deletedAt IS NULL ORDER BY v.uploadedAt DESC")
    List<Video> findByUserIdOrderByUploadedAtDesc(@Param("userId") String userId);

    @Query("SELECT COUNT(v) FROM Video v WHERE v.userId = :userId AND v.deletedAt IS NULL")
    long countByUserId(@Param("userId") String userId);

    @Query("SELECT SUM(v.fileSize) FROM Video v WHERE v.userId = :userId AND v.deletedAt IS NULL")
    Long getTotalFileSizeByUserId(@Param("userId") String userId);

    @Query("SELECT v FROM Video v WHERE v.id = :id AND v.userId = :userId")
    Optional<Video> findByIdAndUserIdIncludingDeleted(@Param("id") UUID id, @Param("userId") String userId);
} 