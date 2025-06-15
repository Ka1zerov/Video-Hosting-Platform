package com.tskrypko.streaming.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Event DTO received from encoding service when video qualities are completed
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoQualityCompletedEvent {
    
    private UUID videoId;
    private String eventType;
    private LocalDateTime timestamp;
    private List<CompletedQuality> completedQualities;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CompletedQuality {
        private String qualityName; // 1080p, 720p, 480p
        private Integer width;
        private Integer height;
        private Integer bitrate;
        private String hlsPlaylistUrl;
        private String s3Key;
        private Long fileSize;
        private String status;
    }
} 