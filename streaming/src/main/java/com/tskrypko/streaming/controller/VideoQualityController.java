package com.tskrypko.streaming.controller;

import com.tskrypko.streaming.model.VideoQuality;
import com.tskrypko.streaming.service.VideoQualityService;
import com.tskrypko.streaming.repository.VideoQualityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for video quality management and testing
 */
@Slf4j
@RestController
@RequestMapping("/api/streaming/qualities")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:8080"})
public class VideoQualityController {

    private final VideoQualityRepository videoQualityRepository;
    private final VideoQualityService videoQualityService;

    /**
     * Get all qualities for a specific video
     */
    @GetMapping("/video/{videoId}")
    public ResponseEntity<List<VideoQuality>> getVideoQualities(@PathVariable UUID videoId) {
        log.info("Getting qualities for video: {}", videoId);
        
        List<VideoQuality> qualities = videoQualityRepository.findByVideoIdOrderByBitrateDesc(videoId);
        
        log.info("Found {} qualities for video: {}", qualities.size(), videoId);
        return ResponseEntity.ok(qualities);
    }

    /**
     * Get quality statistics for a video
     */
    @GetMapping("/video/{videoId}/stats")
    public ResponseEntity<Map<String, Object>> getVideoQualityStats(@PathVariable UUID videoId) {
        log.info("Getting quality statistics for video: {}", videoId);
        
        long completedCount = videoQualityService.getCompletedQualitiesCount(videoId);
        boolean hasQualities = videoQualityService.hasAvailableQualities(videoId);
        List<VideoQuality> allQualities = videoQualityRepository.findByVideoIdOrderByBitrateDesc(videoId);
        
        Map<String, Object> stats = Map.of(
            "videoId", videoId,
            "totalQualities", allQualities.size(),
            "completedQualities", completedCount,
            "hasAvailableQualities", hasQualities,
            "qualities", allQualities
        );
        
        return ResponseEntity.ok(stats);
    }

    /**
     * Get all qualities (for debugging)
     */
    @GetMapping("/all")
    public ResponseEntity<List<VideoQuality>> getAllQualities() {
        log.info("Getting all video qualities");
        
        List<VideoQuality> qualities = videoQualityRepository.findAll();
        
        log.info("Found {} total qualities in database", qualities.size());
        return ResponseEntity.ok(qualities);
    }

    /**
     * Health check for qualities functionality
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Video qualities service is running");
    }
} 