package com.tskrypko.upload.dto;

import com.tskrypko.upload.model.Video;
import com.tskrypko.upload.model.VideoStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UploadResponse {

    private UUID id;
    private String title;
    private String description;
    private String originalFilename;
    private Long fileSize;
    private VideoStatus status;
    private LocalDateTime uploadedAt;
    private String message;

    // Constructor from Video model
    public UploadResponse(Video video, String message) {
        this.id = video.getId();
        this.title = video.getTitle();
        this.description = video.getDescription();
        this.originalFilename = video.getOriginalFilename();
        this.fileSize = video.getFileSize();
        this.status = video.getStatus();
        this.uploadedAt = video.getUploadedAt();
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