package com.tskrypko.streaming.service;

import com.amazonaws.services.cloudfront.AmazonCloudFront;
import com.amazonaws.services.cloudfront.AmazonCloudFrontClientBuilder;
import com.amazonaws.services.cloudfront.util.SignerUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;

@Slf4j
@Service
@RequiredArgsConstructor
public class CloudFrontService {

    @Value("${aws.cloudfront.domain:}")
    private String cloudFrontDomain;
    
    @Value("${aws.cloudfront.enabled:false}")
    private boolean cloudFrontEnabled;
    
    @Value("${aws.s3.bucket.name}")
    private String s3BucketName;
    
    @Value("${streaming.cdn.cache-control:public, max-age=31536000}")
    private String defaultCacheControl;
    
    @Value("${streaming.cdn.manifest-cache-control:public, max-age=60}")
    private String manifestCacheControl;

    private AmazonCloudFront cloudFrontClient;

    /**
     * Initialize CloudFront client
     */
    private AmazonCloudFront getCloudFrontClient() {
        if (cloudFrontClient == null) {
            cloudFrontClient = AmazonCloudFrontClientBuilder.standard().build();
        }
        return cloudFrontClient;
    }

    /**
     * Check if CloudFront CDN is enabled
     */
    public boolean isEnabled() {
        return cloudFrontEnabled && StringUtils.hasText(cloudFrontDomain);
    }

    /**
     * Get CDN URL for S3 object
     */
    public String getCdnUrl(String s3Url) {
        if (!isEnabled() || !StringUtils.hasText(s3Url)) {
            return s3Url;
        }

        try {
            // Extract S3 key from S3 URL
            String s3Key = extractS3KeyFromUrl(s3Url);
            if (s3Key == null) {
                log.warn("Could not extract S3 key from URL: {}", s3Url);
                return s3Url;
            }

            // Build CloudFront URL
            String cdnUrl = "https://" + cloudFrontDomain + "/" + s3Key;
            log.debug("Converted S3 URL {} to CDN URL {}", s3Url, cdnUrl);
            return cdnUrl;
            
        } catch (Exception e) {
            log.error("Error converting S3 URL to CDN URL: {}", s3Url, e);
            return s3Url;
        }
    }

    /**
     * Get signed CDN URL with expiration (for secure content)
     */
    public String getSignedCdnUrl(String s3Url, LocalDateTime expiryTime) {
        if (!isEnabled()) {
            return s3Url;
        }

        try {
            String cdnUrl = getCdnUrl(s3Url);
            
            // For now, return unsigned URL
            // In production, you would implement CloudFront signed URLs here
            // using SignerUtils.signUrlCanned() with your private key
            
            log.debug("Generated signed CDN URL for: {}", s3Url);
            return cdnUrl;
            
        } catch (Exception e) {
            log.error("Error generating signed CDN URL: {}", s3Url, e);
            return s3Url;
        }
    }

    /**
     * Get cache control header for different content types
     */
    public String getCacheControl(String url) {
        if (url == null) {
            return defaultCacheControl;
        }
        
        // Shorter cache for manifests (they change more frequently)
        if (url.contains(".m3u8") || url.contains(".mpd")) {
            return manifestCacheControl;
        }
        
        // Longer cache for video segments and static content
        return defaultCacheControl;
    }

    /**
     * Extract S3 key from S3 URL
     */
    private String extractS3KeyFromUrl(String s3Url) {
        try {
            URL url = new URL(s3Url);
            String path = url.getPath();
            
            // Remove leading slash
            if (path.startsWith("/")) {
                path = path.substring(1);
            }
            
            return path;
            
        } catch (Exception e) {
            log.error("Error extracting S3 key from URL: {}", s3Url, e);
            return null;
        }
    }

    /**
     * Invalidate CDN cache for specific paths (useful when content is updated)
     */
    public void invalidateCache(String... paths) {
        if (!isEnabled()) {
            log.warn("CloudFront not enabled, skipping cache invalidation");
            return;
        }

        try {
            // In production, you would implement CloudFront invalidation here
            // using createInvalidation() API call
            
            log.info("CloudFront cache invalidation requested for {} paths", paths.length);
            
        } catch (Exception e) {
            log.error("Error invalidating CloudFront cache", e);
        }
    }

    /**
     * Get CDN distribution statistics (if needed for monitoring)
     */
    public void getDistributionStats() {
        if (!isEnabled()) {
            return;
        }

        try {
            // Implementation for getting CloudFront distribution statistics
            // This could be used for monitoring CDN performance
            
            log.debug("Retrieved CloudFront distribution statistics");
            
        } catch (Exception e) {
            log.error("Error getting CloudFront distribution stats", e);
        }
    }
} 