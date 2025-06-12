package com.tskrypko.upload.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ChunkUploadRequest {

    @NotBlank(message = "Upload ID is required")
    private String uploadId;

    @NotNull(message = "Part number is required")
    private Integer partNumber;

    @NotBlank(message = "S3 key is required")
    private String s3Key;

    @Override
    public String toString() {
        return "ChunkUploadRequest{" +
                "uploadId='" + uploadId + '\'' +
                ", partNumber=" + partNumber +
                ", s3Key='" + s3Key + '\'' +
                '}';
    }
} 