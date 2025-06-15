package com.tskrypko.encoding.integration;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.tskrypko.encoding.model.EncodingJob;
import com.tskrypko.encoding.model.EncodingStatus;
import com.tskrypko.encoding.repository.EncodingJobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@TestPropertySource(properties = {
    "aws.s3.bucket.name=test-bucket",
    "aws.s3.region=us-east-1",
    "rabbitmq.exchange.video=video.exchange",
    "rabbitmq.queue.encoding=video.encoding.queue",
    "rabbitmq.routing.key.encoding=video.encoding"
})
class RabbitMQIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private EncodingJobRepository jobRepository;

    @Autowired
    private AmazonS3 amazonS3;

    private static final String BUCKET_NAME = "test-bucket";

    @BeforeEach
    void setUp() throws IOException {
        // Create test bucket if it doesn't exist
        if (!amazonS3.doesBucketExistV2(BUCKET_NAME)) {
            amazonS3.createBucket(BUCKET_NAME);
        }
    }

    @Test
    void shouldReceiveEncodingJobFromQueue() throws Exception {
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

        // Create and send message
        String message = String.format(
            "{\"videoId\":\"%s\",\"userId\":\"%s\",\"title\":\"%s\",\"originalFilename\":\"%s\",\"s3Key\":\"%s\",\"fileSize\":%d,\"mimeType\":\"%s\"}",
            videoId, userId, title, originalFilename, s3Key, fileSize, mimeType
        );

        rabbitTemplate.convertAndSend("video.exchange", "video.encoding", message);

        EncodingJob job = null;
        int maxWaitMs = 30000;
        int waited = 0;
        int pollInterval = 500;
        while (waited < maxWaitMs) {
            job = jobRepository.findByVideoId(UUID.fromString(videoId)).orElse(null);
            if (job != null && (job.getStatus() == EncodingStatus.COMPLETED || job.getStatus() == EncodingStatus.FAILED)) {
                break;
            }
            Thread.sleep(pollInterval);
            waited += pollInterval;
        }
        assertNotNull(job, "EncodingJob должен быть создан");
        if (job.getStatus() == EncodingStatus.FAILED) {
            fail("Encoding завершился с ошибкой: " + job.getErrorMessage());
        }
        assertEquals(EncodingStatus.COMPLETED, job.getStatus());
        assertNotNull(job.getCompletedAt());
    }
}
