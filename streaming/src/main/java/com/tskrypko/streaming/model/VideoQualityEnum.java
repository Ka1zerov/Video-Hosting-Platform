package com.tskrypko.streaming.model;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Enum for video quality levels in MVP
 */
@Getter
public enum VideoQualityEnum {
    Q_480P("480p", 854, 480, 1000),
    Q_720P("720p", 1280, 720, 2500),
    Q_1080P("1080p", 1920, 1080, 4000);

    private final String qualityName;
    private final int width;
    private final int height;
    private final int bitrate; // in kbps

    VideoQualityEnum(String qualityName, int width, int height, int bitrate) {
        this.qualityName = qualityName;
        this.width = width;
        this.height = height;
        this.bitrate = bitrate;
    }

    /**
     * Get quality enum by name
     */
    public static VideoQualityEnum fromQualityName(String qualityName) {
        for (VideoQualityEnum quality : values()) {
            if (quality.qualityName.equals(qualityName)) {
                return quality;
            }
        }
        return null;
    }

    /**
     * Build HLS playlist URL for this quality
     * Note: This method uses fallback values. For production, use VideoQualityUrlBuilder service.
     */
    public String buildHlsPlaylistUrl(String videoId) {
        String bucketName = System.getenv("S3_BUCKET_NAME");
        String region = System.getenv("AWS_REGION");
        
        // Use defaults if environment variables are not set
        if (bucketName == null || bucketName.isEmpty()) {
            bucketName = "video-hosting-thesis";
        }
        if (region == null || region.isEmpty()) {
            region = "eu-north-1";
        }
        
        return String.format("https://%s.s3.%s.amazonaws.com/encoded/%s/%s/playlist.m3u8",
                bucketName, region, videoId, qualityName);
    }

    /**
     * Build thumbnail URL for this quality
     * Note: This method uses fallback values. For production, use VideoQualityUrlBuilder service.
     */
    public String buildThumbnailUrl(String videoId) {
        String bucketName = System.getenv("S3_BUCKET_NAME");
        String region = System.getenv("AWS_REGION");
        
        // Use defaults if environment variables are not set
        if (bucketName == null || bucketName.isEmpty()) {
            bucketName = "video-hosting-thesis";
        }
        if (region == null || region.isEmpty()) {
            region = "eu-north-1";
        }
        
        return String.format("https://%s.s3.%s.amazonaws.com/thumbnails/%s/thumbnail_%s.jpg",
                bucketName, region, videoId, qualityName);
    }

    /**
     * Utility component for building URLs with proper Spring configuration
     */
    @Component
    public static class VideoQualityUrlBuilder {
        
        @Value("${aws.s3.bucket.name}")
        private String s3BucketName;
        
        @Value("${aws.region}")
        private String awsRegion;
        
        public String buildHlsPlaylistUrl(VideoQualityEnum quality, String videoId) {
            return String.format("https://%s.s3.%s.amazonaws.com/encoded/%s/%s/playlist.m3u8",
                    s3BucketName, awsRegion, videoId, quality.getQualityName());
        }
        
        public String buildThumbnailUrl(VideoQualityEnum quality, String videoId) {
            return String.format("https://%s.s3.%s.amazonaws.com/thumbnails/%s/thumbnail_%s.jpg",
                    s3BucketName, awsRegion, videoId, quality.getQualityName());
        }
    }
} 