package com.tskrypko.encoding.repository;

import com.tskrypko.encoding.model.Video;
import com.tskrypko.encoding.model.VideoStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface VideoRepository extends JpaRepository<Video, UUID> {

    @Query("SELECT v FROM Video v WHERE v.id = :id AND v.deletedAt IS NULL")
    Optional<Video> findById(@Param("id") UUID id);

    @Modifying
    @Query("UPDATE Video v SET v.status = :status WHERE v.id = :id")
    void updateStatus(@Param("id") UUID id, @Param("status") VideoStatus status);

    @Modifying
    @Query("UPDATE Video v SET v.status = :status, v.duration = :duration WHERE v.id = :id")
    void updateVideoAfterEncoding(@Param("id") UUID id, 
                                 @Param("status") VideoStatus status,
                                 @Param("duration") Long duration);
} 