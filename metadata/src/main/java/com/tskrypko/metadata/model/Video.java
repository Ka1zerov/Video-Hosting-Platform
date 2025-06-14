package com.tskrypko.metadata.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
@Entity
@Table(name = "videos")
@NoArgsConstructor
public class Video extends BaseEntity {

    @NotBlank(message = "Title is required")
    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "duration")
    private Long duration; // Duration in seconds

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VideoStatus status = VideoStatus.UPLOADED;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt = LocalDateTime.now();

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "views_count")
    private Long viewsCount = 0L;

    @Column(name = "last_accessed")
    private LocalDateTime lastAccessed;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @Column(name = "modified_by", nullable = false)
    private String modifiedBy;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
