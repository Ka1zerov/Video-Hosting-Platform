package com.tskrypko.streaming.controller;

import com.tskrypko.streaming.model.VideoQualityEnum;
import com.tskrypko.streaming.model.VideoStatus;
import com.tskrypko.streaming.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * REST controller for video quality management (MVP version)
 */
@Slf4j
@RestController
@RequestMapping("/api/streaming/qualities")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:8080"})
public class VideoQualityController {

    private final VideoRepository videoRepository;

    /**
     * Get all qualities for a specific video (MVP: returns static qualities)
     */
    @GetMapping("/video/{videoId}")
    public ResponseEntity<List<Map<String, Object>>> getVideoQualities(@PathVariable UUID videoId) {
        log.info("Getting qualities for video: {}", videoId);
        
        // Check if video exists and is ready
        boolean videoExists = videoRepository.findByIdAndStatusAndDeletedAtIsNull(videoId, VideoStatus.READY).isPresent();
        
        if (!videoExists) {
            log.warn("Video not found or not ready: {}", videoId);
            return ResponseEntity.ok(new ArrayList<>());
        }

        // For MVP: return all static qualities
        List<Map<String, Object>> qualities = Arrays.stream(VideoQualityEnum.values())
                .map(quality -> {
                    Map<String, Object> qualityMap = new HashMap<>();
                    qualityMap.put("qualityName", quality.getQualityName());
                    qualityMap.put("width", quality.getWidth());
                    qualityMap.put("height", quality.getHeight());
                    qualityMap.put("bitrate", quality.getBitrate());
                    qualityMap.put("hlsPlaylistUrl", quality.buildHlsPlaylistUrl(videoId.toString()));
                    qualityMap.put("available", true);
                    return qualityMap;
                })
                .collect(Collectors.toList());
        
        log.info("Found {} qualities for video: {}", qualities.size(), videoId);
        return ResponseEntity.ok(qualities);
    }

    /**
     * Get quality statistics for a video (MVP version)
     */
    @GetMapping("/video/{videoId}/stats")
    public ResponseEntity<Map<String, Object>> getVideoQualityStats(@PathVariable UUID videoId) {
        log.info("Getting quality statistics for video: {}", videoId);
        
        boolean videoExists = videoRepository.findByIdAndStatusAndDeletedAtIsNull(videoId, VideoStatus.READY).isPresent();
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("videoId", videoId);
        stats.put("totalQualities", videoExists ? VideoQualityEnum.values().length : 0);
        stats.put("completedQualities", videoExists ? VideoQualityEnum.values().length : 0);
        stats.put("hasAvailableQualities", videoExists);
        stats.put("availableQualities", videoExists ? 
            Arrays.stream(VideoQualityEnum.values())
                .map(VideoQualityEnum::getQualityName)
                .collect(Collectors.toList()) : 
            new ArrayList<>());
        
        return ResponseEntity.ok(stats);
    }

    /**
     * Get all qualities (for debugging) - MVP version
     */
    @GetMapping("/all")
    public ResponseEntity<List<Map<String, Object>>> getAllQualities() {
        log.info("Getting all video qualities (MVP static list)");
        
        // For MVP: return count of ready videos with static qualities
        long readyVideosCount = videoRepository.countByStatusAndDeletedAtIsNull(VideoStatus.READY);
        
        List<Map<String, Object>> allQualities = Arrays.stream(VideoQualityEnum.values())
                .map(quality -> {
                    Map<String, Object> qualityMap = new HashMap<>();
                    qualityMap.put("qualityName", quality.getQualityName());
                    qualityMap.put("width", quality.getWidth());
                    qualityMap.put("height", quality.getHeight());
                    qualityMap.put("bitrate", quality.getBitrate());
                    qualityMap.put("availableForVideos", readyVideosCount);
                    return qualityMap;
                })
                .collect(Collectors.toList());
        
        log.info("Found {} quality types available for {} ready videos", allQualities.size(), readyVideosCount);
        return ResponseEntity.ok(allQualities);
    }

    /**
     * Health check for qualities functionality
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Video qualities service is running (MVP mode)");
    }
} 