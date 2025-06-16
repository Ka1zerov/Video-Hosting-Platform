package com.tskrypko.streaming.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.services.cloudfront.CloudFrontUtilities;
import software.amazon.awssdk.services.cloudfront.model.CannedSignerRequest;
import software.amazon.awssdk.services.cloudfront.url.SignedUrl;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Service for CloudFront CDN operations with signed URL generation
 * Uses AWS SDK v2 CloudFrontUtilities for proper URL signing
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CloudFrontService {

    @Value("${aws.cloudfront.domain:}")
    private String cloudFrontDomain;

    @Value("${aws.cloudfront.enabled:false}")
    private boolean cloudFrontEnabled;

    @Value("${aws.cloudfront.signing.enabled:false}")
    private boolean signingEnabled;

    @Value("${aws.cloudfront.signing.key-pair-id:}")
    private String keyPairId;

    @Value("${aws.cloudfront.signing.private-key-path:}")
    private String privateKeyPath;

    @Value("${aws.cloudfront.signing.default-expiration-hours:2}")
    private int defaultExpirationHours;

    @Value("${aws.s3.bucket.name}")
    private String s3BucketName;

    @Value("${streaming.cdn.cache-control:public, max-age=31536000}")
    private String defaultCacheControl;

    @Value("${streaming.cdn.manifest-cache-control:public, max-age=60}")
    private String manifestCacheControl;

    private CloudFrontUtilities cloudFrontUtilities;

    /**
     * Initialize CloudFront utilities (lazy initialization)
     */
    private CloudFrontUtilities getCloudFrontUtilities() {
        if (cloudFrontUtilities == null) {
            cloudFrontUtilities = CloudFrontUtilities.create();
        }
        return cloudFrontUtilities;
    }

    /**
     * Check if CloudFront CDN is enabled
     */
    public boolean isEnabled() {
        return cloudFrontEnabled && StringUtils.hasText(cloudFrontDomain);
    }

    /**
     * Check if CloudFront URL signing is enabled and properly configured
     */
    public boolean isSigningEnabled() {
        return isEnabled() && signingEnabled && 
               StringUtils.hasText(keyPairId) && 
               StringUtils.hasText(privateKeyPath);
    }

    /**
     * Get CDN URL for S3 object (without signing)
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
     * Get signed CDN URL with expiration for secure content
     * This implements real CloudFront URL signing using AWS SDK v2
     */
    public String getSignedCdnUrl(String s3Url, LocalDateTime expiryTime) {
        if (!isSigningEnabled()) {
            log.debug("CloudFront signing not enabled, returning unsigned CDN URL");
            return getCdnUrl(s3Url);
        }

        try {
            // Convert to CDN URL first
            String cdnUrl = getCdnUrl(s3Url);
            
            // Convert LocalDateTime to Instant
            Instant expirationInstant = expiryTime.toInstant(ZoneOffset.UTC);
            
            // Get private key path
            Path privateKey = getPrivateKeyPath();
            if (privateKey == null) {
                log.error("Private key path not found: {}", privateKeyPath);
                return cdnUrl;
            }

            // Create canned signer request
            CannedSignerRequest cannedRequest = CannedSignerRequest.builder()
                    .resourceUrl(cdnUrl)
                    .privateKey(privateKey)
                    .keyPairId(keyPairId)
                    .expirationDate(expirationInstant)
                    .build();

            // Generate signed URL using CloudFront utilities
            SignedUrl signedUrl = getCloudFrontUtilities().getSignedUrlWithCannedPolicy(cannedRequest);
            String finalSignedUrl = signedUrl.url();

            log.debug("Generated signed CDN URL: {} -> {}", s3Url, finalSignedUrl);
            return finalSignedUrl;

        } catch (Exception e) {
            log.error("Error generating signed CDN URL for: {}", s3Url, e);
            // Fallback to unsigned CDN URL
            return getCdnUrl(s3Url);
        }
    }

    /**
     * Get signed CDN URL with default expiration
     */
    public String getSignedCdnUrl(String s3Url) {
        LocalDateTime expiryTime = LocalDateTime.now().plusHours(defaultExpirationHours);
        return getSignedCdnUrl(s3Url, expiryTime);
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
     * Get private key path, handle both classpath and file system paths
     */
    private Path getPrivateKeyPath() {
        try {
            if (privateKeyPath.startsWith("classpath:")) {
                // Handle classpath resource
                String resourcePath = privateKeyPath.substring("classpath:".length());
                URL resource = getClass().getClassLoader().getResource(resourcePath);
                if (resource != null) {
                    return Paths.get(resource.toURI());
                } else {
                    log.error("Classpath resource not found: {}", resourcePath);
                    return null;
                }
            } else {
                // Handle file system path
                return Paths.get(privateKeyPath);
            }
        } catch (Exception e) {
            log.error("Error resolving private key path: {}", privateKeyPath, e);
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
            // TODO: Implement CloudFront invalidation using AWS SDK v2
            // This would require createInvalidation() API call
            log.info("CloudFront cache invalidation requested for {} paths", paths.length);

        } catch (Exception e) {
            log.error("Error invalidating CloudFront cache", e);
        }
    }

    /**
     * Log current configuration for debugging
     */
    public void logConfiguration() {
        log.info("CloudFront Configuration:");
        log.info("  - Enabled: {}", cloudFrontEnabled);
        log.info("  - Domain: {}", cloudFrontDomain);
        log.info("  - Signing Enabled: {}", signingEnabled);
        log.info("  - Key Pair ID: {}", keyPairId != null ? keyPairId.substring(0, Math.min(keyPairId.length(), 8)) + "..." : "not set");
        log.info("  - Private Key Path: {}", privateKeyPath);
        log.info("  - Default Expiration: {} hours", defaultExpirationHours);
        log.info("  - Is Fully Configured: {}", isSigningEnabled());
    }
}

