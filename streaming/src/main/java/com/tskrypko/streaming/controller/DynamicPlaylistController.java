package com.tskrypko.streaming.controller;

import com.tskrypko.streaming.service.DynamicMasterPlaylistService;
import com.tskrypko.streaming.service.VideoStreamingService;
import com.tskrypko.streaming.service.CurrentUserService;
import com.tskrypko.streaming.dto.PlaybackRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Controller for serving dynamic HLS master playlists
 * This replaces static master.m3u8 files with dynamic generation containing signed URLs
 */
@Slf4j
@RestController
@RequestMapping("/api/streaming/playlist")
@RequiredArgsConstructor
public class DynamicPlaylistController {

    private final DynamicMasterPlaylistService dynamicMasterPlaylistService;
    private final VideoStreamingService videoStreamingService;
    private final CurrentUserService currentUserService;

    /**
     * Generate dynamic master.m3u8 playlist for a video
     * This endpoint replaces static master.m3u8 files from S3/CDN
     * 
     * Example URL: GET /api/streaming/playlist/{videoId}/master.m3u8
     */
    @GetMapping("/{videoId}/master.m3u8")
    public ResponseEntity<String> getMasterPlaylist(
            @PathVariable String videoId,
            @RequestParam(defaultValue = "2") int expirationHours,
            HttpServletRequest request) {
        
        String clientIp = getClientIpAddress(request);
        String userId = currentUserService.getCurrentUserIdOrNull();
        
        log.info("Dynamic master playlist requested - Video: {}, User: {}, IP: {}, Expiry: {}h", 
                videoId, userId, clientIp, expirationHours);

        try {
            // Validate video access using existing streaming service logic
            // This ensures the user has permission to access this video
            PlaybackRequest playbackRequest = new PlaybackRequest();
            playbackRequest.setVideoId(UUID.fromString(videoId));
            playbackRequest.setFormat("hls");
            
            videoStreamingService.getVideoStream(playbackRequest, clientIp);

            // Generate dynamic master playlist with signed URLs
            return dynamicMasterPlaylistService.generateMasterPlaylistForUser(
                    videoId, userId, expirationHours);

        } catch (Exception e) {
            log.error("Error generating master playlist for video: {} user: {}", videoId, userId, e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Generate master playlist with custom expiration time
     */
    @GetMapping("/{videoId}/master.m3u8/expires/{timestamp}")
    public ResponseEntity<String> getMasterPlaylistWithTimestamp(
            @PathVariable String videoId,
            @PathVariable long timestamp,
            HttpServletRequest request) {
        
        String clientIp = getClientIpAddress(request);
        String userId = currentUserService.getCurrentUserIdOrNull();
        
        LocalDateTime expiryTime = LocalDateTime.ofInstant(
                java.time.Instant.ofEpochSecond(timestamp), 
                java.time.ZoneOffset.UTC
        );
        
        log.info("Dynamic master playlist with timestamp - Video: {}, User: {}, Expiry: {}", 
                videoId, userId, expiryTime);

        try {
            // Validate video access
            PlaybackRequest playbackRequest = new PlaybackRequest();
            playbackRequest.setVideoId(UUID.fromString(videoId));
            playbackRequest.setFormat("hls");
            
            videoStreamingService.getVideoStream(playbackRequest, clientIp);

            // Generate playlist with specific expiry time
            return dynamicMasterPlaylistService.generateMasterPlaylist(videoId, expiryTime);

        } catch (Exception e) {
            log.error("Error generating timestamped master playlist for video: {}", videoId, e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Health check endpoint for dynamic playlist service
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Dynamic playlist service is healthy");
    }

    /**
     * Extract client IP address from request
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
} 