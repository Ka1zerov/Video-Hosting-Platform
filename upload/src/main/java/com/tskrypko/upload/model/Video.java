package com.tskrypko.upload.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "videos")
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

    public Video() {}

    public Video(String title, String description, String originalFilename, 
                 Long fileSize, String mimeType, String userId) {
        this.title = title;
        this.description = description;
        this.originalFilename = originalFilename;
        this.fileSize = fileSize;
        this.mimeType = mimeType;
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getS3Key() {
        return s3Key;
    }

    public void setS3Key(String s3Key) {
        this.s3Key = s3Key;
    }

    public VideoStatus getStatus() {
        return status;
    }

    public void setStatus(VideoStatus status) {
        this.status = status;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    @Override
    public String toString() {
        return "Video{" +
                "id=" + getId() +
                ", title='" + title + '\'' +
                ", originalFilename='" + originalFilename + '\'' +
                ", fileSize=" + fileSize +
                ", status=" + status +
                ", uploadedAt=" + uploadedAt +
                '}';
    }
} 