package com.tskrypko.encoding.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "encoding_jobs")
@NoArgsConstructor
public class EncodingJob extends BaseEntity {

    @NotNull(message = "Video ID is required")
    @Column(name = "video_id", nullable = false)
    private UUID videoId;

    @NotBlank(message = "User ID is required")
    @Column(name = "user_id", nullable = false)
    private String userId;

    @NotBlank(message = "Title is required")
    @Column(nullable = false)
    private String title;

    @NotBlank(message = "Original filename is required")
    @Column(name = "original_filename", nullable = false)
    private String originalFilename;

    @NotNull(message = "File size is required")
    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "mime_type")
    private String mimeType;

    @NotBlank(message = "S3 key is required")
    @Column(name = "s3_key", nullable = false)
    private String s3Key;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EncodingStatus status = EncodingStatus.PENDING;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "retry_count")
    private Integer retryCount = 0;

    @Column(name = "progress")
    private Integer progress = 0;

    @Override
    public String toString() {
        return "EncodingJob{" +
                "id=" + getId() +
                ", videoId='" + videoId + '\'' +
                ", originalFilename='" + originalFilename + '\'' +
                ", status=" + status +
                ", progress=" + progress +
                '}';
    }
} 