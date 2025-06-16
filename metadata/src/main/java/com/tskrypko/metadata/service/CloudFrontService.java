package com.tskrypko.metadata.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service for CloudFront CDN URL handling in metadata service
 */
@Slf4j
@Service
public class CloudFrontService {

    @Value("${aws.cloudfront.domain}")
    private String cloudFrontDomain;

    @Value("${aws.cloudfront.enabled:false}")
    private boolean cloudFrontEnabled;

    /**
     * Check if CloudFront is enabled
     */
    public boolean isEnabled() {
        return cloudFrontEnabled && cloudFrontDomain != null && !cloudFrontDomain.trim().isEmpty();
    }

    /**
     * Convert S3 URL to CloudFront URL
     */
    public String getCdnUrl(String s3Url) {
        if (!isEnabled() || s3Url == null || s3Url.isEmpty()) {
            return s3Url;
        }

        try {
            // Replace S3 domain with CloudFront domain
            // Example: https://bucket.s3.region.amazonaws.com/path -> https://cloudfront.domain/path
            if (s3Url.contains(".s3.") && s3Url.contains(".amazonaws.com")) {
                int pathStart = s3Url.indexOf(".amazonaws.com/") + ".amazonaws.com/".length();
                if (pathStart < s3Url.length()) {
                    String path = s3Url.substring(pathStart);
                    String cdnUrl = String.format("https://%s/%s", cloudFrontDomain, path);
                    log.debug("Converted S3 URL to CDN: {} -> {}", s3Url, cdnUrl);
                    return cdnUrl;
                }
            }
        } catch (Exception e) {
            log.warn("Failed to convert S3 URL to CDN URL: {}, error: {}", s3Url, e.getMessage());
        }

        return s3Url;
    }
} 