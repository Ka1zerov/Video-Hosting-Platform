package com.tskrypko.streaming.service;

import com.tskrypko.streaming.model.VideoQualityEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service for generating video URLs dynamically
 */
@Slf4j
@Service
public class VideoUrlService {

    @Value("${aws.s3.bucket.name}")
    private String s3BucketName;

    @Value("${aws.region}")
    private String awsRegion;

    /**
     * Build thumbnail URL for video
     */
    public String buildThumbnailUrl(String videoId) {
        return String.format("https://%s.s3.%s.amazonaws.com/thumbnails/%s/thumbnail_720p.jpg",
                s3BucketName, awsRegion, videoId);
    }

    /**
     * Build HLS playlist URL for specific quality
     */
    public String buildHlsPlaylistUrl(String videoId, VideoQualityEnum quality) {
        return String.format("https://%s.s3.%s.amazonaws.com/encoded/%s/%s/playlist.m3u8",
                s3BucketName, awsRegion, videoId, quality.getQualityName());
    }

    /**
     * Build master HLS manifest URL (not used in MVP but kept for compatibility)
     */
    public String buildMasterHlsManifestUrl(String videoId) {
        return String.format("https://%s.s3.%s.amazonaws.com/encoded/%s/master.m3u8",
                s3BucketName, awsRegion, videoId);
    }

    /**
     * Build DASH manifest URL (not used in MVP but kept for compatibility)
     */
    public String buildDashManifestUrl(String videoId) {
        return String.format("https://%s.s3.%s.amazonaws.com/encoded/%s/manifest.mpd",
                s3BucketName, awsRegion, videoId);
    }
} 