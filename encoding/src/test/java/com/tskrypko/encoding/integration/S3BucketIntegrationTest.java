package com.tskrypko.encoding.integration;

import com.amazonaws.services.s3.AmazonS3;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

@TestPropertySource(properties = {
    "aws.s3.bucket=test-bucket",
    "aws.s3.region=us-east-1"
})
class S3BucketIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private AmazonS3 amazonS3;

    private static final String BUCKET_NAME = "test-bucket";

    @BeforeEach
    void setUp() {
        // Create bucket if it doesn't exist
        if (!amazonS3.doesBucketExistV2(BUCKET_NAME)) {
            amazonS3.createBucket(BUCKET_NAME);
        }
    }

    @Test
    void shouldCreateAndAccessTestBucket() {
        // Given
        String bucketName = BUCKET_NAME;

        // When
        boolean bucketExists = amazonS3.doesBucketExistV2(bucketName);
        
        // Then
        assertTrue(bucketExists, "Bucket should exist");
        
        // Verify we can list objects in the bucket
        assertDoesNotThrow(() -> amazonS3.listObjects(bucketName),
            "Should be able to list objects in the bucket");
    }
} 