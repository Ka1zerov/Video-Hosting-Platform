package com.tskrypko.metadata.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service for generating video URLs dynamically in metadata service
 */
@Slf4j
@Service
public class VideoUrlService {

    @Value("${aws.s3.bucket.name}")
    private String s3BucketName;

    @Value("${aws.region}")
    private String awsRegion;

    /**
     * Build thumbnail URL for video (1080p quality for metadata service)
     */
    public String buildThumbnailUrl(String videoId) {
        return String.format("https://%s.s3.%s.amazonaws.com/thumbnails/%s/thumbnail_1080p.jpg",
                s3BucketName, awsRegion, videoId);
    }
} 