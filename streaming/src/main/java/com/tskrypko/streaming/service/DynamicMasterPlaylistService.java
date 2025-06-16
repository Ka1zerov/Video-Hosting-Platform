package com.tskrypko.streaming.service;

import com.tskrypko.streaming.model.VideoQualityEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;

/**
 * Service for dynamically generating master.m3u8 playlists with signed URLs
 * This solves the problem of having static master playlists in CDN that can't contain signed URLs
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DynamicMasterPlaylistService {

    private final CloudFrontService cloudFrontService;
    private final VideoUrlService videoUrlService;

    /**
     * Log CloudFront configuration on startup
     */
    @PostConstruct
    public void init() {
        cloudFrontService.logConfiguration();
        
        if (!cloudFrontService.isSigningEnabled()) {
            log.warn("⚠️ CloudFront URL signing is DISABLED. Video URLs will be unsigned!");
            log.warn("   This means videos may be accessible without proper authentication.");
            log.warn("   To enable signing, configure CloudFront key pair and enable signing in application.yml");
        } else {
            log.info("✅ CloudFront URL signing is ENABLED. Videos will use signed URLs for security.");
        }
    }

    /**
     * Generate dynamic master.m3u8 playlist with signed URLs for video qualities
     */
    public ResponseEntity<String> generateMasterPlaylist(String videoId, LocalDateTime expiryTime) {
        log.info("Generating dynamic master playlist for video: {} with expiry: {}", videoId, expiryTime);

        StringBuilder content = new StringBuilder();
        content.append("#EXTM3U\n");
        content.append("#EXT-X-VERSION:3\n");
        content.append("#EXT-X-INDEPENDENT-SEGMENTS\n");

        int signedUrlCount = 0;
        int totalUrlCount = 0;

        // Add each quality as a stream variant with signed URLs
        for (VideoQualityEnum quality : VideoQualityEnum.values()) {
            totalUrlCount++;
            
            // Add stream info line
            content.append("#EXT-X-STREAM-INF:BANDWIDTH=")
                    .append(quality.getBitrate() * 1000) // Convert kbps to bps
                    .append(",RESOLUTION=")
                    .append(quality.getWidth())
                    .append("x")
                    .append(quality.getHeight())
                    .append(",CODECS=\"avc1.42e01e,mp4a.40.2\"")
                    .append("\n");

            // Generate signed URL for quality playlist
            String qualityPlaylistUrl = videoUrlService.buildHlsPlaylistUrl(videoId, quality);
            
            if (cloudFrontService.isEnabled()) {
                String originalUrl = qualityPlaylistUrl;
                qualityPlaylistUrl = cloudFrontService.getSignedCdnUrl(qualityPlaylistUrl, expiryTime);
                
                // Check if URL was actually signed (signed URLs contain query parameters)
                if (qualityPlaylistUrl.contains("Expires=") && qualityPlaylistUrl.contains("Signature=")) {
                    signedUrlCount++;
                    log.debug("✅ Successfully signed URL for {}: {}", quality.getQualityName(), qualityPlaylistUrl);
                } else {
                    log.warn("⚠️ URL signing failed for {}, using unsigned URL: {}", quality.getQualityName(), qualityPlaylistUrl);
                }
            } else {
                log.debug("CloudFront disabled, using S3 URL for {}: {}", quality.getQualityName(), qualityPlaylistUrl);
            }
            
            content.append(qualityPlaylistUrl).append("\n");
        }

        String playlistContent = content.toString();
        
        // Log signing results
        if (cloudFrontService.isSigningEnabled()) {
            if (signedUrlCount == totalUrlCount) {
                log.info("✅ All {} quality URLs successfully signed for video: {}", signedUrlCount, videoId);
            } else {
                log.warn("⚠️ Only {}/{} quality URLs were signed for video: {}", signedUrlCount, totalUrlCount, videoId);
            }
        } else {
            log.info("ℹ️ Generated master playlist with {} unsigned URLs for video: {}", totalUrlCount, videoId);
        }
        
        log.debug("Generated master playlist for video {}: \n{}", videoId, playlistContent);

        // Return with proper HLS content type and caching headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.apple.mpegurl"));
        headers.setCacheControl("no-cache, no-store, must-revalidate");
        headers.setPragma("no-cache");
        headers.setExpires(0);
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(playlistContent);
    }

    /**
     * Generate master playlist with default 2-hour expiration
     */
    public ResponseEntity<String> generateMasterPlaylist(String videoId) {
        LocalDateTime expiryTime = LocalDateTime.now().plusHours(2);
        return generateMasterPlaylist(videoId, expiryTime);
    }

    /**
     * Generate master playlist for user with custom expiration
     */
    public ResponseEntity<String> generateMasterPlaylistForUser(String videoId, String userId, int expirationHours) {
        LocalDateTime expiryTime = LocalDateTime.now().plusHours(expirationHours);
        
        log.info("Generating personalized master playlist for user: {} video: {} expiry: {} hours", 
                userId, videoId, expirationHours);
        
        return generateMasterPlaylist(videoId, expiryTime);
    }
} 