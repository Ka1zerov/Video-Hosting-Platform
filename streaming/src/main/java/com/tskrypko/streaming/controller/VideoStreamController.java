package com.tskrypko.streaming.controller;

import com.tskrypko.streaming.dto.PlaybackRequest;
import com.tskrypko.streaming.dto.VideoStreamResponse;
import com.tskrypko.streaming.service.VideoStreamingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/streaming")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:8080"})
public class VideoStreamController {

    private final VideoStreamingService videoStreamingService;

    /**
     * Get streaming information for a specific video
     */
    @PostMapping("/play")
    public ResponseEntity<VideoStreamResponse> getVideoStream(
            @Valid @RequestBody PlaybackRequest request,
            HttpServletRequest httpRequest) {
        
        log.info("Stream request for video: {} from IP: {}", request.getVideoId(), getClientIpAddress(httpRequest));
        
        String ipAddress = getClientIpAddress(httpRequest);
        VideoStreamResponse response = videoStreamingService.getVideoStream(request, ipAddress);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get available videos for streaming (paginated)
     */
    @GetMapping("/videos")
    public ResponseEntity<Page<VideoStreamResponse>> getAvailableVideos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.info("Getting available videos, page: {}, size: {}", page, size);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<VideoStreamResponse> videos = videoStreamingService.getAvailableVideos(pageable);
        
        return ResponseEntity.ok(videos);
    }

    /**
     * Search videos by title
     */
    @GetMapping("/videos/search")
    public ResponseEntity<Page<VideoStreamResponse>> searchVideos(
            @RequestParam String title,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.info("Searching videos by title: '{}', page: {}, size: {}", title, page, size);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<VideoStreamResponse> videos = videoStreamingService.searchVideos(title, pageable);
        
        return ResponseEntity.ok(videos);
    }

    /**
     * Get popular videos (most viewed)
     */
    @GetMapping("/videos/popular")
    public ResponseEntity<Page<VideoStreamResponse>> getPopularVideos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.info("Getting popular videos, page: {}, size: {}", page, size);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<VideoStreamResponse> videos = videoStreamingService.getPopularVideos(pageable);
        
        return ResponseEntity.ok(videos);
    }

    /**
     * Get recently accessed videos
     */
    @GetMapping("/videos/recent")
    public ResponseEntity<Page<VideoStreamResponse>> getRecentlyAccessedVideos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.info("Getting recently accessed videos, page: {}, size: {}", page, size);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<VideoStreamResponse> videos = videoStreamingService.getRecentlyAccessedVideos(pageable);
        
        return ResponseEntity.ok(videos);
    }

    /**
     * Get user's videos (requires authentication via gateway)
     */
    @GetMapping("/videos/user/{userId}")
    public ResponseEntity<Page<VideoStreamResponse>> getUserVideos(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.info("Getting videos for user: {}, page: {}, size: {}", userId, page, size);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<VideoStreamResponse> videos = videoStreamingService.getUserVideos(userId, pageable);
        
        return ResponseEntity.ok(videos);
    }

    /**
     * Get video by ID (basic info without streaming URLs for security)
     */
    @GetMapping("/videos/{videoId}")
    public ResponseEntity<VideoStreamResponse> getVideoById(@PathVariable UUID videoId) {
        
        log.info("Getting video info for ID: {}", videoId);
        
        // Create a simple request to get basic video info
        PlaybackRequest request = new PlaybackRequest();
        request.setVideoId(videoId);
        request.setSessionId("info-only");
        
        VideoStreamResponse response = videoStreamingService.getVideoStream(request, "127.0.0.1");
        
        // Remove streaming URLs and session info for public endpoint
        response.setHlsManifestUrl(null);
        response.setDashManifestUrl(null);
        response.setQualities(null);
        response.setStreamToken(null);
        response.setTokenExpiresAt(null);
        response.setCdnUrls(null);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Streaming service is running");
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