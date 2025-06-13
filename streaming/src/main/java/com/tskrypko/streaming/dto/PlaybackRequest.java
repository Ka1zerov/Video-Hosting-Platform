package com.tskrypko.streaming.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlaybackRequest {
    
    @NotNull(message = "Video ID is required")
    private Long videoId;
    
    private String preferredQuality; // "480p", "720p", "1080p", "auto"
    private String format; // "hls", "dash", "mp4"
    private String userAgent;
    private String sessionId;
    private Boolean allowDownload = false;
} 