package com.tskrypko.encoding.service;

import com.tskrypko.encoding.dto.VideoQualityCompletedEvent;
import com.tskrypko.encoding.model.EncodingJob;
import com.tskrypko.encoding.model.EncodingStatus;
import com.tskrypko.encoding.model.VideoQuality;
import com.tskrypko.encoding.model.VideoStatus;
import com.tskrypko.encoding.repository.EncodingJobRepository;
import com.tskrypko.encoding.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.job.FFmpegJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VideoEncodingService {

    private static final Logger logger = LoggerFactory.getLogger(VideoEncodingService.class);

    private final EncodingJobRepository encodingJobRepository;
    private final S3Service s3Service;
    private final FFmpegService ffmpegService;
    private final TransactionTemplate transactionTemplate;
    private final VideoRepository videoRepository;
    private final StreamingNotificationService streamingNotificationService;

    @Value("${encoding.temp.directory:/tmp/encoding}")
    private String tempDirectory;

    @Value("${encoding.hls.segment.duration:10}")
    private int hlsSegmentDuration;

    @Value("${encoding.cleanup.enabled:true}")
    private boolean cleanupEnabled;

    @Value("${aws.s3.bucket.name}")
    private String s3BucketName;

    @Value("${aws.region}")
    private String awsRegion;

    /**
     * Processes a video encoding job asynchronously.
     *
     * <p>This method is used in production to handle video encoding without blocking
     * the calling thread. The actual processing is delegated to {@link #processEncodingJobSync(String)}.
     *
     * @param jobId the UUID string of the encoding job to process
     * @see #processEncodingJobSync(String) for synchronous processing (used in tests)
     */
    @Async
    public void processEncodingJob(String jobId) {
        processEncodingJobSync(jobId);
    }

    /**
     * Processes a video encoding job synchronously.
     *
     * <p>This method performs the complete video encoding workflow:
     * <ol>
     *   <li>Downloads the original video from S3</li>
     *   <li>Encodes video in multiple qualities (1080p, 720p, 480p)</li>
     *   <li>Generates HLS playlists and segments</li>
     *   <li>Creates thumbnails for each quality</li>
     *   <li>Uploads all encoded content back to S3</li>
     *   <li>Updates job status and optionally cleans up temporary files</li>
     *   <li>Notifies streaming service about completed qualities</li>
     * </ol>
     *
     * <p><strong>Usage in different environments:</strong>
     * <ul>
     *   <li><strong>Production</strong>: Called via {@link #processEncodingJob(String)} for async processing</li>
     *   <li><strong>Testing</strong>: Called directly to ensure synchronous execution and prevent
     *       race conditions with TestContainers lifecycle</li>
     * </ul>
     *
     * <p><strong>Error handling:</strong> If any step fails, the job status is set to FAILED
     * and temporary files are cleaned up (if cleanup is enabled).
     *
     * @param jobId the UUID string of the encoding job to process
     * @throws IllegalArgumentException if the job with given ID is not found
     * @see #processEncodingJob(String) for asynchronous version
     * @see EncodingStatus for possible job statuses
     */
    public void processEncodingJobSync(String jobId) {
        try {
            EncodingJob job = encodingJobRepository.findById(java.util.UUID.fromString(jobId))
                    .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));

            logger.info("Starting encoding job: {}", job);

            // Update encoding job status
            updateJobStatus(job, EncodingStatus.PROCESSING, LocalDateTime.now(), null);

            // Update video status to PROCESSING using TransactionTemplate
            updateVideoStatusTransactional(UUID.fromString(String.valueOf(job.getVideoId())), VideoStatus.PROCESSING);

            String localInputFile = downloadVideoFromS3(job);
            long videoDurationNs = getVideoDurationNs(localInputFile);
            Long durationSeconds = videoDurationNs > 0 ? videoDurationNs / 1_000_000_000L : null;

            // Track completed qualities for streaming service notification
            List<VideoQualityCompletedEvent.CompletedQuality> completedQualities = new ArrayList<>();

            // Process each quality
            for (VideoQuality quality : VideoQuality.values()) {
                VideoQualityCompletedEvent.CompletedQuality completedQuality = processQuality(job, localInputFile, quality, videoDurationNs);
                if (completedQuality != null) {
                    completedQualities.add(completedQuality);
                }
            }

            // Generate thumbnails
            generateThumbnails(job, localInputFile);

            // Update encoding job status
            updateJobStatus(job, EncodingStatus.COMPLETED, null, LocalDateTime.now());

            // Update video status to READY with duration using TransactionTemplate
            updateVideoAfterEncodingTransactional(UUID.fromString(String.valueOf(job.getVideoId())),
                                                VideoStatus.READY,
                                                durationSeconds);

            // Notify streaming service about completed qualities
            if (!completedQualities.isEmpty()) {
                VideoQualityCompletedEvent event = new VideoQualityCompletedEvent(
                    UUID.fromString(String.valueOf(job.getVideoId())),
                    "VIDEO_QUALITIES_COMPLETED",
                    java.time.Instant.now(),
                    completedQualities
                );
                streamingNotificationService.notifyVideoQualitiesCompleted(event);
            }

            if (cleanupEnabled) {
                cleanupTempFiles(job);
            }

            logger.info("Encoding job completed successfully: {}", job.getId());

        } catch (Exception e) {
            logger.error("Error processing encoding job {}: {}", jobId, e.getMessage(), e);
            handleJobError(jobId, e.getMessage());
        }
    }

    private VideoQualityCompletedEvent.CompletedQuality processQuality(EncodingJob job, String inputFile, VideoQuality quality, long videoDurationNs) throws IOException {
        logger.info("Encoding {} quality for job {}", quality.getLabel(), job.getId());

        String outputDir = createOutputDirectory(job, quality);
        String playlistFile = Paths.get(outputDir, "playlist.m3u8").toString();

        FFmpegBuilder builder = new FFmpegBuilder()
                .setInput(inputFile)
                .addOutput(playlistFile)
                .setVideoCodec("libx264")
                .setVideoFrameRate(24)
                .setVideoResolution(quality.getWidth(), quality.getHeight())
                .setVideoBitRate(quality.getBitrateKbps() * 1000L)
                .setAudioCodec("aac")
                .setAudioBitRate(128_000)
                .setFormat("hls")
                .addExtraArgs("-hls_time", String.valueOf(hlsSegmentDuration))
                .addExtraArgs("-hls_list_size", "0")
                .addExtraArgs("-hls_segment_filename", Paths.get(outputDir, "segment_%03d.ts").toString())
                .done();

        FFmpegExecutor executor = new FFmpegExecutor(ffmpegService.getFfmpeg(), ffmpegService.getFfprobe());

        FFmpegJob ffmpegJob = executor.createJob(builder, progress -> {
            if (videoDurationNs > 0) {
                int percent = (int) Math.min(100, Math.max(0, (progress.out_time_ns * 100 / videoDurationNs)));
                transactionTemplate.execute(status -> {
                    job.setProgress(percent);
                    encodingJobRepository.save(job);
                    return null;
                });
            }
        });

        ffmpegJob.run();

        // Upload encoded files to S3 and get info for streaming service
        return uploadQualityToS3(job, outputDir, quality);
    }

    private void generateThumbnails(EncodingJob job, String inputFile) throws IOException {
        logger.info("Generating thumbnails for job {}", job.getId());

        String thumbnailDir = createThumbnailDirectory(job);

        for (VideoQuality quality : VideoQuality.values()) {
            String thumbnailFile = Paths.get(thumbnailDir, "thumbnail_" + quality.getLabel() + ".jpg").toString();

            FFmpegBuilder builder = new FFmpegBuilder()
                    .setInput(inputFile)
                    .addOutput(thumbnailFile)
                    .setVideoFilter("scale=" + quality.getWidth() + ":" + quality.getHeight())
                    .addExtraArgs("-ss", "00:00:10")
                    .addExtraArgs("-vframes", "1")
                    .done();

            FFmpegExecutor executor = new FFmpegExecutor(ffmpegService.getFfmpeg(), ffmpegService.getFfprobe());
            executor.createJob(builder).run();
        }

        // Upload thumbnails to S3
        uploadThumbnailsToS3(job, thumbnailDir);
    }

    private String downloadVideoFromS3(EncodingJob job) throws IOException {
        String tempDir = createTempDirectory(job);
        String localFile = Paths.get(tempDir, "input_" + job.getOriginalFilename()).toString();

        s3Service.downloadFile(job.getS3Key(), localFile);
        logger.info("Downloaded video from S3: {} -> {}", job.getS3Key(), localFile);

        return localFile;
    }

    private String createTempDirectory(EncodingJob job) throws IOException {
        Path tempPath = Paths.get(tempDirectory, job.getId().toString());
        Files.createDirectories(tempPath);
        return tempPath.toString();
    }

    private String createOutputDirectory(EncodingJob job, VideoQuality quality) throws IOException {
        Path outputPath = Paths.get(tempDirectory, job.getId().toString(), "encoded", quality.getFolder());
        Files.createDirectories(outputPath);
        return outputPath.toString();
    }

    private String createThumbnailDirectory(EncodingJob job) throws IOException {
        Path thumbnailPath = Paths.get(tempDirectory, job.getId().toString(), "thumbnails");
        Files.createDirectories(thumbnailPath);
        return thumbnailPath.toString();
    }

    private VideoQualityCompletedEvent.CompletedQuality uploadQualityToS3(EncodingJob job, String outputDir, VideoQuality quality) throws IOException {
        String s3Prefix = "encoded/" + job.getVideoId() + "/" + quality.getFolder() + "/";
        String playlistS3Key = s3Prefix + "playlist.m3u8";
        String hlsPlaylistUrl = buildS3Url(playlistS3Key);
        
        File dir = new File(outputDir);
        File[] files = dir.listFiles();
        long totalFileSize = 0;

        if (files != null) {
            for (File file : files) {
                String s3Key = s3Prefix + file.getName();
                s3Service.uploadFile(file.getAbsolutePath(), s3Key);
                totalFileSize += file.length();
                logger.info("Uploaded {} to S3: {}", file.getName(), s3Key);
            }
        }

        // Return completed quality info for streaming service
        return new VideoQualityCompletedEvent.CompletedQuality(
            quality.getLabel(),
            quality.getWidth(),
            quality.getHeight(),
            quality.getBitrateKbps(),
            hlsPlaylistUrl,
            playlistS3Key,
            totalFileSize,
            "COMPLETED"
        );
    }

    private void uploadThumbnailsToS3(EncodingJob job, String thumbnailDir) throws IOException {
        String s3Prefix = "thumbnails/" + job.getVideoId() + "/";

        File dir = new File(thumbnailDir);
        File[] files = dir.listFiles();

        if (files != null) {
            for (File file : files) {
                String s3Key = s3Prefix + file.getName();
                s3Service.uploadFile(file.getAbsolutePath(), s3Key);
                logger.info("Uploaded thumbnail to S3: {}", s3Key);
            }
        }
    }

    private void cleanupTempFiles(EncodingJob job) {
        Path tempPath = Paths.get(tempDirectory, job.getId().toString());
        if (Files.exists(tempPath)) {
            try (var walk = Files.walk(tempPath)) {
                walk.map(Path::toFile)
                    .forEach(file -> {
                        if (file.isFile() && !file.delete()) {
                            logger.warn("Failed to delete file: {}", file.getAbsolutePath());
                        }
                    });
            } catch (IOException e) {
                logger.warn("Failed to cleanup temp files for job {}: {}", job.getId(), e.getMessage());
            }
            try {
                Files.deleteIfExists(tempPath);
                logger.info("Cleaned up temp files for job: {}", job.getId());
            } catch (IOException e) {
                logger.warn("Failed to delete temp directory for job {}: {}", job.getId(), e.getMessage());
            }
        }
    }

    protected void updateJobStatus(EncodingJob job, EncodingStatus status, LocalDateTime startedAt, LocalDateTime completedAt) {
        transactionTemplate.execute(tx -> {
            job.setStatus(status);
            if (startedAt != null) {
                job.setStartedAt(startedAt);
            }
            if (completedAt != null) {
                job.setCompletedAt(completedAt);
            }
            encodingJobRepository.save(job);
            return null;
        });
    }

    protected void handleJobError(String jobId, String errorMessage) {
        try {
            EncodingJob job = encodingJobRepository.findById(java.util.UUID.fromString(jobId))
                    .orElse(null);

            if (job != null) {
                transactionTemplate.execute(tx -> {
                    job.setStatus(EncodingStatus.FAILED);
                    job.setErrorMessage(errorMessage);
                    job.setRetryCount(job.getRetryCount() + 1);
                    encodingJobRepository.save(job);
                    if (cleanupEnabled) {
                        cleanupTempFiles(job);
                    }
                    return null;
                });

                // Update video status to FAILED using TransactionTemplate
                updateVideoStatusTransactional(UUID.fromString(String.valueOf(job.getVideoId())), VideoStatus.FAILED);
            }
        } catch (Exception e) {
            logger.error("Error handling job error for {}: {}", jobId, e.getMessage(), e);
        }
    }

    private long getVideoDurationNs(String inputFile) {
        try {
            var probeResult = ffmpegService.getFfprobe().probe(inputFile);
            double seconds = probeResult.getFormat().duration;
            return (long) (seconds * 1_000_000_000L);
        } catch (Exception e) {
            logger.warn("Could not get video duration for {}: {}", inputFile, e.getMessage());
            return 0;
        }
    }

    public EncodingJob getJobByS3Key(String s3Key) {
        return encodingJobRepository.findByS3Key(s3Key)
                .orElseThrow(() -> new IllegalArgumentException("Job not found for S3 key: " + s3Key));
    }

    /**
     * Updates video status using TransactionTemplate to avoid self-invocation issues.
     * This method is used internally when called from other methods in the same class.
     */
    private void updateVideoStatusTransactional(UUID videoId, VideoStatus status) {
        transactionTemplate.execute(tx -> {
            try {
                videoRepository.updateStatus(videoId, status);
                logger.info("Updated video status: videoId={}, status={}", videoId, status);
            } catch (Exception e) {
                logger.error("Failed to update video status: videoId={}, status={}", videoId, status, e);
                throw new RuntimeException("Failed to update video status", e);
            }
            return null;
        });
    }

    /**
     * Updates video after encoding using TransactionTemplate to avoid self-invocation issues.
     * This method is used internally when called from other methods in the same class.
     */
    private void updateVideoAfterEncodingTransactional(UUID videoId, VideoStatus status, Long duration) {
        transactionTemplate.execute(tx -> {
            try {
                videoRepository.updateVideoAfterEncoding(videoId, status, duration);
                logger.info("Updated video after encoding: videoId={}, status={}, duration={}",
                          videoId, status, duration);
                return null;
            } catch (Exception e) {
                logger.error("Failed to update video after encoding: videoId={}, error={}", videoId, e.getMessage());
                throw new RuntimeException("Failed to update video after encoding", e);
            }
        });
    }

    /**
     * Public method for updating video status. Can be used by external callers.
     * Uses @Transactional since it's called from outside the class.
     */
    @Transactional
    public void updateVideoStatus(UUID videoId, VideoStatus status) {
        try {
            videoRepository.updateStatus(videoId, status);
            logger.info("Updated video status: videoId={}, status={}", videoId, status);
        } catch (Exception e) {
            logger.error("Failed to update video status: videoId={}, status={}, error={}", videoId, status, e.getMessage());
            throw new RuntimeException("Failed to update video status", e);
        }
    }

    /**
     * Public method for updating video after encoding. Can be used by external callers.
     * Uses @Transactional since it's called from outside the class.
     */
    @Transactional
    public void updateVideoAfterEncoding(UUID videoId, VideoStatus status, Long duration) {
        try {
            videoRepository.updateVideoAfterEncoding(videoId, status, duration);
            logger.info("Updated video after encoding: videoId={}, status={}, duration={}", videoId, status, duration);
        } catch (Exception e) {
            logger.error("Failed to update video after encoding: videoId={}, error={}", videoId, e.getMessage());
            throw new RuntimeException("Failed to update video after encoding", e);
        }
    }

    private String buildS3Url(String s3Key) {
        return String.format("https://%s.s3.%s.amazonaws.com/%s", s3BucketName, awsRegion, s3Key);
    }
}
