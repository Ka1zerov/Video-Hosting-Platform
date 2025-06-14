package com.tskrypko.metadata.controller;

import com.tskrypko.metadata.dto.VideoMetadataDto;
import com.tskrypko.metadata.exception.VideoNotFoundException;
import com.tskrypko.metadata.service.VideoMetadataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/metadata/videos")
@RequiredArgsConstructor
@Slf4j
public class VideoMetadataController {

    private final VideoMetadataService videoMetadataService;

    @GetMapping
    public ResponseEntity<Page<VideoMetadataDto>> getAllVideos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("Getting all videos - page: {}, size: {}", page, size);

        validatePagination(page, size);
        Pageable pageable = PageRequest.of(page, size);

        Page<VideoMetadataDto> videos = videoMetadataService.getAllReadyVideos(pageable);
        log.info("Found {} videos on page {}", videos.getNumberOfElements(), page);

        return ResponseEntity.ok(videos);
    }

    @GetMapping("/{videoId}")
    public ResponseEntity<VideoMetadataDto> getVideoById(@PathVariable UUID videoId) {
        log.info("Getting video by id: {}", videoId);

        VideoMetadataDto video = videoMetadataService.getVideoById(videoId)
                .orElseThrow(() -> new VideoNotFoundException(videoId));

        return ResponseEntity.ok(video);
    }

    @GetMapping("/search")
    public ResponseEntity<Page<VideoMetadataDto>> searchVideos(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("Searching videos by query: '{}' - page: {}, size: {}", query, page, size);

        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("Search query cannot be empty");
        }

        validatePagination(page, size);
        Pageable pageable = PageRequest.of(page, size);

        Page<VideoMetadataDto> videos = videoMetadataService.searchVideos(query.trim(), pageable);
        log.info("Found {} videos for query '{}' on page {}",
                videos.getNumberOfElements(), query, page);

        return ResponseEntity.ok(videos);
    }

    @GetMapping("/popular")
    public ResponseEntity<Page<VideoMetadataDto>> getPopularVideos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("Getting popular videos - page: {}, size: {}", page, size);

        validatePagination(page, size);
        Pageable pageable = PageRequest.of(page, size);

        Page<VideoMetadataDto> videos = videoMetadataService.getPopularVideos(pageable);
        log.info("Found {} popular videos on page {}", videos.getNumberOfElements(), page);

        return ResponseEntity.ok(videos);
    }

    @GetMapping("/recent")
    public ResponseEntity<Page<VideoMetadataDto>> getRecentlyWatchedVideos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("Getting recently watched videos - page: {}, size: {}", page, size);

        validatePagination(page, size);
        Pageable pageable = PageRequest.of(page, size);

        Page<VideoMetadataDto> videos = videoMetadataService.getRecentlyWatchedVideos(pageable);
        log.info("Found {} recently watched videos on page {}", videos.getNumberOfElements(), page);

        return ResponseEntity.ok(videos);
    }

    private void validatePagination(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("Page number must be non-negative");
        }
        if (size <= 0 || size > 100) {
            throw new IllegalArgumentException("Page size must be between 1 and 100");
        }
    }
}
