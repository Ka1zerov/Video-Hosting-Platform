package com.tskrypko.upload.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MultipartUploadSession implements Serializable {

    private String uploadId;
    private String s3Key;
    private String userId;
    private String title;
    private String description;
    private String originalFilename;
    private Long fileSize;
    private String mimeType;
    private Integer totalParts;
    private Long partSize;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    
    // Map<partNumber, etag>
    private Map<Integer, String> uploadedParts = new ConcurrentHashMap<>();

    public void addUploadedPart(Integer partNumber, String etag) {
        uploadedParts.put(partNumber, etag);
    }

    public boolean isCompleted() {
        return uploadedParts.size() == totalParts;
    }

    public int getUploadedPartsCount() {
        return uploadedParts.size();
    }

    public double getProgressPercentage() {
        if (totalParts == 0) return 0.0;
        return (double) uploadedParts.size() / totalParts * 100.0;
    }

    @Override
    public String toString() {
        return "MultipartUploadSession{" +
                "uploadId='" + uploadId + '\'' +
                ", s3Key='" + s3Key + '\'' +
                ", userId='" + userId + '\'' +
                ", title='" + title + '\'' +
                ", originalFilename='" + originalFilename + '\'' +
                ", fileSize=" + fileSize +
                ", totalParts=" + totalParts +
                ", uploadedParts=" + uploadedParts.size() +
                ", progress=" + String.format("%.2f", getProgressPercentage()) + "%" +
                '}';
    }
} 