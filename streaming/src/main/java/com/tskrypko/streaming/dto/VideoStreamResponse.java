package com.tskrypko.streaming.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VideoStreamResponse {
    
    private UUID id;
    private String title;
    private String description;
    private Long duration; // in seconds
    private String thumbnailUrl;
    private Long viewsCount;
    
    // Streaming URLs
    private String hlsManifestUrl;
    private String dashManifestUrl;
    
    // Available qualities
    private List<QualityOption> qualities;
    
    // Playback token for secure streaming
    private String streamToken;
    private LocalDateTime tokenExpiresAt;
    
    // CDN URLs (when CloudFront is enabled)
    private StreamUrls cdnUrls;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QualityOption {
        private String qualityName; // 480p, 720p, 1080p
        private Integer width;
        private Integer height;
        private Integer bitrate;
        private String hlsPlaylistUrl;
        private Boolean available;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StreamUrls {
        private String hlsUrl;
        private String dashUrl;
        private String thumbnailUrl;
        private Boolean cdnEnabled;
    }
} 