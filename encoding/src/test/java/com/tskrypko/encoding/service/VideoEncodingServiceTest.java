package com.tskrypko.encoding.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.tskrypko.encoding.integration.BaseIntegrationTest;
import com.tskrypko.encoding.model.EncodingJob;
import com.tskrypko.encoding.model.EncodingStatus;
import com.tskrypko.encoding.model.VideoQuality;
import com.tskrypko.encoding.repository.EncodingJobRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.TestPropertySource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestPropertySource(properties = {
    "aws.s3.bucket.name=test-bucket",
    "encoding.temp.directory=src/test/resources/test-results",
    "encoding.cleanup.enabled=false"
})
class VideoEncodingServiceTest extends BaseIntegrationTest {

    @Autowired
    private VideoEncodingService videoEncodingService;

    @Autowired
    private EncodingJobRepository jobRepository;

    @Autowired
    private AmazonS3 amazonS3;

    private static final String BUCKET_NAME = "test-bucket";
    private static final String TEST_RESULTS_DIR = "src/test/resources/test-results";

    @BeforeAll
    void setUp() throws IOException, InterruptedException {
        // Create test results directory if it doesn't exist
        Files.createDirectories(Paths.get(TEST_RESULTS_DIR));
    }

    private void ensureBucketExists() throws InterruptedException {
        // Create test bucket if it doesn't exist
        if (!amazonS3.doesBucketExistV2(BUCKET_NAME)) {
            amazonS3.createBucket(BUCKET_NAME);
        }
        int maxAttempts = 10;
        int attempt = 0;
        while (!amazonS3.doesBucketExistV2(BUCKET_NAME) && attempt < maxAttempts) {
            Thread.sleep(500);
            attempt++;
        }
        assertTrue(amazonS3.doesBucketExistV2(BUCKET_NAME), "S3 bucket должен существовать после создания");
    }

    @Test
    void shouldCreateEncodingJob() {
        // Given
        String videoId = UUID.randomUUID().toString();
        String s3Key = "original/" + videoId + ".mp4";
        String userId = "test-user";
        String title = "Test Video";
        String originalFilename = "test.mp4";
        Long fileSize = 1024L;
        String mimeType = "video/mp4";

        // When
        EncodingJob job = new EncodingJob();
        job.setVideoId(videoId);
        job.setUserId(userId);
        job.setTitle(title);
        job.setOriginalFilename(originalFilename);
        job.setFileSize(fileSize);
        job.setMimeType(mimeType);
        job.setS3Key(s3Key);
        job = jobRepository.save(job);

        // Then
        assertNotNull(job);
        assertEquals(videoId, job.getVideoId());
        assertEquals(s3Key, job.getS3Key());
        assertNotNull(job.getCreatedAt());
        assertNull(job.getCompletedAt());
        assertEquals(EncodingStatus.PENDING, job.getStatus());
    }

    @Test
    void shouldProcessEncodingJob() throws IOException, InterruptedException {
        // Ensure bucket exists
        ensureBucketExists();

        // Given
        String videoId = UUID.randomUUID().toString();
        String s3Key = "original/" + videoId + ".mp4";
        String userId = "test-user";
        String title = "Test Video";
        String originalFilename = "test.mp4";
        String mimeType = "video/mp4";

        // Upload test video to S3
        ClassPathResource resource = new ClassPathResource("test-video.mp4");
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(resource.contentLength());
        metadata.setContentType(mimeType);
        amazonS3.putObject(
            BUCKET_NAME,
            s3Key,
            resource.getInputStream(),
            metadata
        );

        // Create encoding job
        EncodingJob job = new EncodingJob();
        job.setVideoId(videoId);
        job.setUserId(userId);
        job.setTitle(title);
        job.setOriginalFilename(originalFilename);
        job.setFileSize(resource.contentLength());
        job.setMimeType(mimeType);
        job.setS3Key(s3Key);
        job = jobRepository.save(job);

        // When
        videoEncodingService.processEncodingJobSync(job.getId().toString());

        // Then
        EncodingJob updatedJob = jobRepository.findById(job.getId()).orElseThrow();
        assertEquals(EncodingStatus.COMPLETED, updatedJob.getStatus());
        assertNotNull(updatedJob.getCompletedAt());
    }

    @Test
    void shouldSaveEncodedVideosToTestResults() throws Exception {
        // Ensure bucket exists
        ensureBucketExists();

        // Given
        String videoId = UUID.randomUUID().toString();
        String s3Key = "original/" + videoId + ".mp4";
        String userId = "test-user";
        String title = "Test Video";
        String originalFilename = "test.mp4";
        Long fileSize = 1024L;
        String mimeType = "video/mp4";

        // Upload test video to S3
        ClassPathResource resource = new ClassPathResource("test-video.mp4");
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(resource.contentLength());
        metadata.setContentType(mimeType);
        amazonS3.putObject(
            new PutObjectRequest(
                BUCKET_NAME,
                s3Key,
                resource.getInputStream(),
                metadata
            )
        );

        int maxAttempts = 10;
        int attempt = 0;
        while (!amazonS3.doesObjectExist(BUCKET_NAME, s3Key) && attempt < maxAttempts) {
            Thread.sleep(500);
            attempt++;
        }
        assertTrue(amazonS3.doesObjectExist(BUCKET_NAME, s3Key), "Test video should exist in S3");

        // Create encoding job
        EncodingJob job = new EncodingJob();
        job.setVideoId(videoId);
        job.setUserId(userId);
        job.setTitle(title);
        job.setOriginalFilename(originalFilename);
        job.setFileSize(fileSize);
        job.setMimeType(mimeType);
        job.setS3Key(s3Key);
        job = jobRepository.save(job);

        // When
        videoEncodingService.processEncodingJobSync(job.getId().toString());

        // Then
        EncodingJob updatedJob = jobRepository.findById(job.getId()).orElseThrow();
        assertEquals(EncodingStatus.COMPLETED, updatedJob.getStatus());
        assertNotNull(updatedJob.getCompletedAt());

        // Verify that encoded videos are saved in test results directory
        for (VideoQuality quality : VideoQuality.values()) {
            String qualityDir = Paths.get(TEST_RESULTS_DIR, job.getId().toString(), "encoded", quality.getFolder()).toString();
            File dir = new File(qualityDir);
            assertTrue(dir.exists(), "Directory for " + quality.getLabel() + " should exist");
            assertTrue(dir.isDirectory(), "Should be a directory");

            // Check for playlist and segment files
            File[] files = dir.listFiles();
            assertNotNull(files, "Files should exist in " + quality.getLabel() + " directory");
            assertTrue(files.length > 0, "Should have at least one file in " + quality.getLabel() + " directory");

            // Verify playlist file exists
            boolean hasPlaylist = false;
            for (File file : files) {
                if (file.getName().equals("playlist.m3u8")) {
                    hasPlaylist = true;
                    break;
                }
            }
            assertTrue(hasPlaylist, "Should have playlist.m3u8 file in " + quality.getLabel() + " directory");
        }
    }
}
