package com.tskrypko.encoding.repository;

import com.tskrypko.encoding.model.EncodingJob;
import com.tskrypko.encoding.model.EncodingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EncodingJobRepository extends JpaRepository<EncodingJob, UUID> {

    @Query("SELECT j FROM EncodingJob j WHERE j.status = :status AND j.deletedAt IS NULL")
    List<EncodingJob> findByStatus(@Param("status") EncodingStatus status);

    @Query("SELECT j FROM EncodingJob j WHERE j.videoId = :videoId AND j.deletedAt IS NULL")
    Optional<EncodingJob> findByVideoId(@Param("videoId") String videoId);

    @Query("SELECT j FROM EncodingJob j WHERE j.userId = :userId AND j.deletedAt IS NULL ORDER BY j.createdAt DESC")
    List<EncodingJob> findByUserId(@Param("userId") String userId);

    @Query("SELECT j FROM EncodingJob j WHERE j.status = :status AND j.retryCount < :maxRetries AND j.deletedAt IS NULL ORDER BY j.createdAt ASC")
    List<EncodingJob> findFailedJobsForRetry(@Param("status") EncodingStatus status, @Param("maxRetries") int maxRetries);

    @Query("SELECT j FROM EncodingJob j WHERE j.status = 'PROCESSING' AND j.startedAt < :timeoutThreshold AND j.deletedAt IS NULL")
    List<EncodingJob> findStaleProcessingJobs(@Param("timeoutThreshold") LocalDateTime timeoutThreshold);

    @Query("SELECT COUNT(j) FROM EncodingJob j WHERE j.status = :status AND j.deletedAt IS NULL")
    long countByStatus(@Param("status") EncodingStatus status);
} 