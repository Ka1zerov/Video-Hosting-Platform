package com.tskrypko.upload.dto;

import com.tskrypko.upload.model.VideoStatus;
import java.time.LocalDateTime;
import java.util.UUID;

public class UploadResponse {

    private UUID id;
    private String title;
    private String description;
    private String originalFilename;
    private Long fileSize;
    private VideoStatus status;
    private LocalDateTime uploadedAt;
    private String message;

    public UploadResponse() {}

    public UploadResponse(UUID id, String title, String description, String originalFilename, 
                         Long fileSize, VideoStatus status, LocalDateTime uploadedAt, String message) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.originalFilename = originalFilename;
        this.fileSize = fileSize;
        this.status = status;
        this.uploadedAt = uploadedAt;
        this.message = message;
    }

    // Геттеры и сеттеры
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "UploadResponse{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", originalFilename='" + originalFilename + '\'' +
                ", fileSize=" + fileSize +
                ", status=" + status +
                ", uploadedAt=" + uploadedAt +
                ", message='" + message + '\'' +
                '}';
    }
} 