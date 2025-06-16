package com.tskrypko.streaming.service;

import com.tskrypko.streaming.dto.VideoQualityCompletedEvent;
import com.tskrypko.streaming.model.Video;
import com.tskrypko.streaming.model.VideoStatus;
import com.tskrypko.streaming.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Simplified service for managing video qualities in MVP
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VideoQualityService {

    private final VideoRepository videoRepository;

    /**
     * Process completed video qualities from encoding service
     * For MVP: just mark video as READY for streaming
     */
    @Transactional
    public void processCompletedQualities(VideoQualityCompletedEvent event) {
        log.info("Processing completed qualities for video: {} with {} qualities", 
                event.getVideoId(), event.getCompletedQualities().size());

        // Verify video exists
        Optional<Video> videoOpt = videoRepository.findById(event.getVideoId());
        if (videoOpt.isEmpty()) {
            log.warn("Video not found for ID: {}", event.getVideoId());
            return;
        }

        Video video = videoOpt.get();

        // For MVP: simply mark video as READY for streaming
        // All videos will have 1080p, 720p, 480p qualities available
        video.setStatus(VideoStatus.READY);
        videoRepository.save(video);

        log.info("Successfully marked video {} as READY for streaming with {} qualities", 
                event.getVideoId(), event.getCompletedQualities().size());
    }
} 