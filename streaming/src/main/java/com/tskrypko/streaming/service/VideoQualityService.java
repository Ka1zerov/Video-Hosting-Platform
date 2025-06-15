package com.tskrypko.streaming.service;

import com.tskrypko.streaming.dto.VideoQualityCompletedEvent;
import com.tskrypko.streaming.model.EncodingStatus;
import com.tskrypko.streaming.model.Video;
import com.tskrypko.streaming.model.VideoQuality;
import com.tskrypko.streaming.repository.VideoQualityRepository;
import com.tskrypko.streaming.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing video qualities in streaming service
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VideoQualityService {

    private final VideoQualityRepository videoQualityRepository;
    private final VideoRepository videoRepository;
    private final CloudFrontService cloudFrontService;

    /**
     * Process completed video qualities from encoding service
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

        // Process each completed quality
        for (VideoQualityCompletedEvent.CompletedQuality completedQuality : event.getCompletedQualities()) {
            try {
                createOrUpdateVideoQuality(video, completedQuality);
            } catch (Exception e) {
                log.error("Failed to process quality {} for video {}: {}", 
                        completedQuality.getQualityName(), event.getVideoId(), e.getMessage(), e);
            }
        }

        log.info("Successfully processed {} qualities for video: {}", 
                event.getCompletedQualities().size(), event.getVideoId());
    }

    /**
     * Create or update video quality record
     */
    private void createOrUpdateVideoQuality(Video video, VideoQualityCompletedEvent.CompletedQuality completedQuality) {
        // Check if quality already exists
        Optional<VideoQuality> existingQuality = videoQualityRepository
                .findByVideoIdAndQualityName(video.getId(), completedQuality.getQualityName());

        VideoQuality videoQuality;
        if (existingQuality.isPresent()) {
            videoQuality = existingQuality.get();
            log.debug("Updating existing quality: {} for video: {}", 
                    completedQuality.getQualityName(), video.getId());
        } else {
            videoQuality = new VideoQuality();
            videoQuality.setVideo(video);
            videoQuality.setQualityName(completedQuality.getQualityName());
            log.debug("Creating new quality: {} for video: {}", 
                    completedQuality.getQualityName(), video.getId());
        }

        // Update quality properties
        videoQuality.setWidth(completedQuality.getWidth());
        videoQuality.setHeight(completedQuality.getHeight());
        videoQuality.setBitrate(completedQuality.getBitrate());
        videoQuality.setFileSize(completedQuality.getFileSize());
        videoQuality.setS3Key(completedQuality.getS3Key());
        
        // Set HLS playlist URL (convert to CDN URL if CloudFront is enabled)
        String hlsPlaylistUrl = completedQuality.getHlsPlaylistUrl();
        if (cloudFrontService.isEnabled()) {
            hlsPlaylistUrl = cloudFrontService.getCdnUrl(hlsPlaylistUrl);
        }
        videoQuality.setHlsPlaylistUrl(hlsPlaylistUrl);
        
        // Set encoding status
        if ("COMPLETED".equals(completedQuality.getStatus())) {
            videoQuality.setEncodingStatus(EncodingStatus.COMPLETED);
            videoQuality.setEncodingProgress(100);
        } else {
            videoQuality.setEncodingStatus(EncodingStatus.FAILED);
        }

        // Save quality
        videoQualityRepository.save(videoQuality);
        
        log.info("Saved video quality: {} ({}x{}) for video: {}", 
                videoQuality.getQualityName(), 
                videoQuality.getWidth(), 
                videoQuality.getHeight(), 
                video.getId());
    }

    /**
     * Get available qualities for video
     */
    @Transactional(readOnly = true)
    public boolean hasAvailableQualities(UUID videoId) {
        long count = videoQualityRepository.countByVideoIdAndEncodingStatus(videoId, EncodingStatus.COMPLETED);
        return count > 0;
    }

    /**
     * Get completed qualities count for video
     */
    @Transactional(readOnly = true)
    public long getCompletedQualitiesCount(UUID videoId) {
        return videoQualityRepository.countByVideoIdAndEncodingStatus(videoId, EncodingStatus.COMPLETED);
    }
} 