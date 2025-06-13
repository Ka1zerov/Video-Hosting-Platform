package com.tskrypko.streaming.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

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

    @NotBlank(message = "Original filename is required")
    @Column(name = "original_filename", nullable = false)
    private String originalFilename;

    @NotNull(message = "File size is required")
    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "mime_type")
    private String mimeType;

    @Column(name = "s3_key")
    private String s3Key;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VideoStatus status = VideoStatus.UPLOADED;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt = LocalDateTime.now();

    @Column(name = "user_id", nullable = false)
    private String userId;

    // Streaming-specific fields
    @Column(name = "duration")
    private Long duration; // Duration in seconds

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @Column(name = "hls_manifest_url")
    private String hlsManifestUrl;

    @Column(name = "dash_manifest_url")
    private String dashManifestUrl;

    @OneToMany(mappedBy = "video", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<VideoQuality> qualities;

    @Column(name = "views_count")
    private Long viewsCount = 0L;

    @Column(name = "last_accessed")
    private LocalDateTime lastAccessed;

    @Override
    public String toString() {
        return "Video{" +
                "id=" + getId() +
                ", title='" + title + '\'' +
                ", originalFilename='" + originalFilename + '\'' +
                ", fileSize=" + fileSize +
                ", status=" + status +
                ", duration=" + duration +
                ", viewsCount=" + viewsCount +
                ", uploadedAt=" + uploadedAt +
                '}';
    }
} 