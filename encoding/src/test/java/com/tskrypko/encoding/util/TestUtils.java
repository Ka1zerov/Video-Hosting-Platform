package com.tskrypko.encoding.util;

import com.tskrypko.encoding.model.EncodingJob;
import com.tskrypko.encoding.model.EncodingStatus;
import com.tskrypko.encoding.model.VideoQuality;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class TestUtils {
    
    public static String getTestVideoContent() throws IOException {
        ClassPathResource resource = new ClassPathResource("test-video.mp4");
        return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
    }
    
    public static EncodingJob createTestJob() {
        EncodingJob job = new EncodingJob();
        job.setVideoId(UUID.randomUUID().toString());
        job.setS3Key("original/" + job.getVideoId() + ".mp4");
        job.setStatus(EncodingStatus.PENDING);
        return job;
    }
    
    public static String getEncodedPath(String videoId, VideoQuality quality) {
        return String.format("encoded/%s/%s.mp4", quality.name().toLowerCase(), videoId);
    }
} 