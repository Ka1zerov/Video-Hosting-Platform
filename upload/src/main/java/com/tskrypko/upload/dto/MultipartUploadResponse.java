package com.tskrypko.upload.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MultipartUploadResponse {

    private String uploadId;
    private String s3Key;
    private String message;
    private Integer totalParts;
    private Long partSize;

    @Override
    public String toString() {
        return "MultipartUploadResponse{" +
                "uploadId='" + uploadId + '\'' +
                ", s3Key='" + s3Key + '\'' +
                ", message='" + message + '\'' +
                ", totalParts=" + totalParts +
                ", partSize=" + partSize +
                '}';
    }
} 