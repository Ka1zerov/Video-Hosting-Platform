package com.tskrypko.upload.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Unified request DTO for all types of video uploads (regular and multipart)
 */
@Setter
@Getter
@NoArgsConstructor
public class VideoUploadRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    // Fields for multipart upload
    private String originalFilename;
    private Long fileSize;
    private String mimeType;

    // Constructor for regular upload (from form data)
    public VideoUploadRequest(String title, String description) {
        this.title = title;
        this.description = description;
    }

    // Constructor for multipart upload (from JSON)
    public VideoUploadRequest(String title, String description, String originalFilename, 
                            Long fileSize, String mimeType) {
        this.title = title;
        this.description = description;
        this.originalFilename = originalFilename;
        this.fileSize = fileSize;
        this.mimeType = mimeType;
    }

    /**
     * Check if this is a multipart upload request
     */
    public boolean isMultipartRequest() {
        return originalFilename != null && fileSize != null && mimeType != null;
    }

    @Override
    public String toString() {
        return "VideoUploadRequest{" +
                "title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", originalFilename='" + originalFilename + '\'' +
                ", fileSize=" + fileSize +
                ", mimeType='" + mimeType + '\'' +
                ", isMultipart=" + isMultipartRequest() +
                '}';
    }
} 