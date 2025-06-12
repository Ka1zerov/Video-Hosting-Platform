package com.tskrypko.upload.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MultipartUploadRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    @NotBlank(message = "Original filename is required")
    private String originalFilename;

    @NotNull(message = "File size is required")
    private Long fileSize;

    @NotBlank(message = "MIME type is required")
    private String mimeType;

    @Override
    public String toString() {
        return "MultipartUploadRequest{" +
                "title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", originalFilename='" + originalFilename + '\'' +
                ", fileSize=" + fileSize +
                ", mimeType='" + mimeType + '\'' +
                '}';
    }
} 