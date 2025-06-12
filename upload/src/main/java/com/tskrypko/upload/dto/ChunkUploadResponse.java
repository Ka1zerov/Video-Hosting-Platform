package com.tskrypko.upload.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ChunkUploadResponse {

    private String etag;
    private Integer partNumber;
    private String message;
    private Integer uploadedParts;
    private Integer totalParts;

    @Override
    public String toString() {
        return "ChunkUploadResponse{" +
                "etag='" + etag + '\'' +
                ", partNumber=" + partNumber +
                ", message='" + message + '\'' +
                ", uploadedParts=" + uploadedParts +
                ", totalParts=" + totalParts +
                '}';
    }
} 