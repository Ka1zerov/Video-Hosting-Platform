package com.tskrypko.streaming.service;

import com.tskrypko.streaming.dto.PlaybackRequest;
import com.tskrypko.streaming.dto.VideoStreamResponse;
import com.tskrypko.streaming.exception.VideoAccessDeniedException;
import com.tskrypko.streaming.exception.VideoNotFoundException;
import com.tskrypko.streaming.model.Video;
import com.tskrypko.streaming.model.VideoQualityEnum;
import com.tskrypko.streaming.model.VideoStatus;
import com.tskrypko.streaming.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoStreamingService {

    private final VideoRepository videoRepository;
    private final CloudFrontService cloudFrontService;
    private final SessionManagementService sessionManagementService;
    private final CurrentUserService currentUserService;
    private final VideoUrlService videoUrlService;

    /**
     * Get video streaming information with access control
     */
    @Transactional(readOnly = true)
    public VideoStreamResponse getVideoStream(PlaybackRequest request, String ipAddress) {
        log.info("Getting video stream for video ID: {}", request.getVideoId());
        
        // Find video that's ready for streaming
        Video video = videoRepository.findByIdAndStatusAndDeletedAtIsNull(request.getVideoId(), VideoStatus.READY)
                .orElseThrow(() -> new VideoNotFoundException("Video not found or not ready for streaming: " + request.getVideoId()));

        // Check access rights
        String currentUserId = currentUserService.getCurrentUserIdOrNull();
        validateVideoAccess(video, currentUserId);

        // Generate session ID for analytics
        String sessionId = sessionManagementService.generateSessionId(video.getId(), currentUserId, ipAddress);

        // Build response
        VideoStreamResponse response = new VideoStreamResponse();
        response.setId(video.getId());
        response.setTitle(video.getTitle());
        response.setDescription(video.getDescription());
        response.setDuration(video.getDuration());
        response.setThumbnailUrl(videoUrlService.buildThumbnailUrl(video.getId().toString()));
        response.setViewsCount(video.getViewsCount());
        
        // Use dynamic master playlist instead of static S3 file
        String dynamicMasterUrl = buildDynamicMasterPlaylistUrl(video.getId().toString());
        response.setHlsManifestUrl(dynamicMasterUrl);
        response.setDashManifestUrl(videoUrlService.buildDashManifestUrl(video.getId().toString()));

        // For MVP: All videos have all qualities available (1080p, 720p, 480p)
        List<VideoStreamResponse.QualityOption> qualityOptions = Arrays.stream(VideoQualityEnum.values())
                .map(quality -> mapToQualityOption(quality, video.getId().toString()))
                .collect(Collectors.toList());
        response.setQualities(qualityOptions);

        // Add CDN URLs if CloudFront is enabled
        if (cloudFrontService.isEnabled()) {
            VideoStreamResponse.StreamUrls cdnUrls = new VideoStreamResponse.StreamUrls();
            
            // Use signed URLs for secure access (2 hours expiration)
            LocalDateTime expiryTime = LocalDateTime.now().plusHours(2);
            
            // For now, use regular CDN URLs without signing
            // Dynamic master playlist will handle signed URLs for individual quality playlists
            cdnUrls.setHlsUrl(dynamicMasterUrl); // Keep dynamic master URL (no CDN for this)
            cdnUrls.setDashUrl(cloudFrontService.getCdnUrl(response.getDashManifestUrl()));
            cdnUrls.setThumbnailUrl(cloudFrontService.getCdnUrl(response.getThumbnailUrl()));
            
            cdnUrls.setCdnEnabled(true);
            response.setCdnUrls(cdnUrls);
        }

        // Add session ID for analytics tracking
        response.setStreamToken(sessionId); // Reusing this field for session ID
        response.setTokenExpiresAt(LocalDateTime.now().plusHours(24)); // Session expires in 24 hours

        log.info("Successfully prepared stream for video: {} with {} qualities", video.getId(), qualityOptions.size());
        return response;
    }

    /**
     * Validate video access - public videos or user's own videos
     */
    private void validateVideoAccess(Video video, String userId) {
        // TODO: Add public/private field to Video model in future
        // For now, check if user is the owner of the video
        if (userId == null) {
            // Anonymous user - for now allow all videos
            // In production, only public videos should be accessible
            log.debug("Anonymous access to video: {}", video.getId());
            return;
        }

        // Check if user is the owner of the video
        if (!video.getUserId().equals(userId)) {
            // TODO: Check if video is public when public/private field is added
            log.warn("User {} attempted to access video {} owned by {}", userId, video.getId(), video.getUserId());
            throw new VideoAccessDeniedException("Access denied to video: " + video.getId());
        }

        log.debug("User {} has access to video: {}", userId, video.getId());
    }

    /**
     * Increment view count for video
     */
    @Transactional
    public void incrementViewCount(UUID videoId) {
        log.debug("Incrementing view count for video: {}", videoId);
        videoRepository.incrementViewCount(videoId, LocalDateTime.now());
    }

    /**
     * Get available videos for streaming (public videos only for now)
     */
    @Transactional(readOnly = true)
    public Page<VideoStreamResponse> getAvailableVideos(Pageable pageable) {
        log.info("Getting available videos for streaming, page: {}", pageable.getPageNumber());
        
        Page<Video> videos = videoRepository.findByStatusAndDeletedAtIsNullOrderByCreatedAtDesc(VideoStatus.READY, pageable);
        
        return videos.map(this::mapToBasicResponse);
    }

    /**
     * Search videos by title
     */
    @Transactional(readOnly = true)
    public Page<VideoStreamResponse> searchVideos(String title, Pageable pageable) {
        log.info("Searching videos by title: {}", title);
        
        Page<Video> videos = videoRepository.findByTitleContainingIgnoreCaseAndStatusAndDeletedAtIsNull(title, VideoStatus.READY, pageable);
        
        return videos.map(this::mapToBasicResponse);
    }

    /**
     * Get popular videos (most viewed)
     */
    @Transactional(readOnly = true)
    public Page<VideoStreamResponse> getPopularVideos(Pageable pageable) {
        log.info("Getting popular videos");
        
        Page<Video> videos = videoRepository.findByStatusAndDeletedAtIsNullOrderByViewsCountDescCreatedAtDesc(VideoStatus.READY, pageable);
        
        return videos.map(this::mapToBasicResponse);
    }

    /**
     * Get recently accessed videos
     */
    @Transactional(readOnly = true)
    public Page<VideoStreamResponse> getRecentlyAccessedVideos(Pageable pageable) {
        log.info("Getting recently accessed videos");
        
        Page<Video> videos = videoRepository.findByStatusAndLastAccessedIsNotNullAndDeletedAtIsNullOrderByLastAccessedDesc(VideoStatus.READY, pageable);
        
        return videos.map(this::mapToBasicResponse);
    }

    /**
     * Get user's videos (only if authenticated)
     */
    @Transactional(readOnly = true)
    public Page<VideoStreamResponse> getUserVideos(String userId, Pageable pageable) {
        log.info("Getting videos for user: {}", userId);
        
        // Verify that current user is requesting their own videos or has admin rights
        String currentUserId = currentUserService.getCurrentUserIdOrNull();
        if (currentUserId == null || !currentUserId.equals(userId)) {
            throw new VideoAccessDeniedException("Cannot access videos for user: " + userId);
        }
        
        Page<Video> videos = videoRepository.findByUserIdAndStatusAndDeletedAtIsNullOrderByCreatedAtDesc(userId, VideoStatus.READY, pageable);
        
        return videos.map(this::mapToBasicResponse);
    }

    private VideoStreamResponse.QualityOption mapToQualityOption(VideoQualityEnum quality, String videoId) {
        VideoStreamResponse.QualityOption option = new VideoStreamResponse.QualityOption();
        option.setQualityName(quality.getQualityName());
        option.setWidth(quality.getWidth());
        option.setHeight(quality.getHeight());
        option.setBitrate(quality.getBitrate());
        
        // Build HLS playlist URL using VideoUrlService
        String hlsPlaylistUrl = videoUrlService.buildHlsPlaylistUrl(videoId, quality);
        if (cloudFrontService.isEnabled()) {
            // Use signed URL with 2 hours expiration (will be handled by dynamic master playlist)
            LocalDateTime expiryTime = LocalDateTime.now().plusHours(2);
            hlsPlaylistUrl = cloudFrontService.getSignedCdnUrl(hlsPlaylistUrl, expiryTime);
        }
        option.setHlsPlaylistUrl(hlsPlaylistUrl);
        
        // For MVP: all qualities are always available
        option.setAvailable(true);
        
        log.debug("Mapped quality option: {} ({}x{}) - available: {}, playlist: {}", 
                quality.getQualityName(), 
                quality.getWidth(), 
                quality.getHeight(), 
                option.getAvailable(),
                hlsPlaylistUrl);
        
        return option;
    }

    private VideoStreamResponse mapToBasicResponse(Video video) {
        VideoStreamResponse response = new VideoStreamResponse();
        response.setId(video.getId());
        response.setTitle(video.getTitle());
        response.setDescription(video.getDescription());
        response.setDuration(video.getDuration());
        
        // Build thumbnail URL with CDN if enabled
        String thumbnailUrl = videoUrlService.buildThumbnailUrl(video.getId().toString());
        if (cloudFrontService.isEnabled()) {
            thumbnailUrl = cloudFrontService.getCdnUrl(thumbnailUrl);
        }
        response.setThumbnailUrl(thumbnailUrl);
        response.setViewsCount(video.getViewsCount());
        
        // For MVP: All videos have all qualities available
        List<VideoStreamResponse.QualityOption> qualityOptions = Arrays.stream(VideoQualityEnum.values())
                .map(quality -> mapToQualityOption(quality, video.getId().toString()))
                .collect(Collectors.toList());
        response.setQualities(qualityOptions);
        
        return response;
    }

    /**
     * Build URL for dynamic master playlist endpoint
     * Returns relative URL to work through Gateway routing
     */
    private String buildDynamicMasterPlaylistUrl(String videoId) {
        // Return relative URL that will be handled by Gateway routing
        // Gateway will proxy /api/streaming/* to streaming service
        return String.format("/api/streaming/playlist/%s/master.m3u8", videoId);
    }
} 