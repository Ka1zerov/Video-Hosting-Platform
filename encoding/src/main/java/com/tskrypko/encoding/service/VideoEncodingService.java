package com.tskrypko.encoding.service;

import com.tskrypko.encoding.model.EncodingJob;
import com.tskrypko.encoding.model.EncodingStatus;
import com.tskrypko.encoding.model.VideoQuality;
import com.tskrypko.encoding.repository.EncodingJobRepository;
import lombok.RequiredArgsConstructor;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.job.FFmpegJob;
import net.bramp.ffmpeg.progress.Progress;
import net.bramp.ffmpeg.progress.ProgressListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class VideoEncodingService {

    private static final Logger logger = LoggerFactory.getLogger(VideoEncodingService.class);

    private final EncodingJobRepository encodingJobRepository;
    private final S3Service s3Service;
    private final FFmpegService ffmpegService;

    @Value("${encoding.temp.directory:/tmp/encoding}")
    private String tempDirectory;

    @Value("${encoding.hls.segment.duration:10}")
    private int hlsSegmentDuration;

    @Async
    @Transactional
    public void processEncodingJob(String jobId) {
        try {
            EncodingJob job = encodingJobRepository.findById(java.util.UUID.fromString(jobId))
                    .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));

            logger.info("Starting encoding job: {}", job);

            updateJobStatus(job, EncodingStatus.PROCESSING, LocalDateTime.now(), null);

            String localInputFile = downloadVideoFromS3(job);
            
            // Process each quality
            for (VideoQuality quality : VideoQuality.values()) {
                processQuality(job, localInputFile, quality);
            }

            // Generate thumbnails
            generateThumbnails(job, localInputFile);

            updateJobStatus(job, EncodingStatus.COMPLETED, null, LocalDateTime.now());
            cleanupTempFiles(job);

            logger.info("Encoding job completed successfully: {}", job.getId());

        } catch (Exception e) {
            logger.error("Error processing encoding job {}: {}", jobId, e.getMessage(), e);
            handleJobError(jobId, e.getMessage());
        }
    }

    private void processQuality(EncodingJob job, String inputFile, VideoQuality quality) throws IOException {
        logger.info("Encoding {} quality for job {}", quality.getLabel(), job.getId());

        String outputDir = createOutputDirectory(job, quality);
        String playlistFile = Paths.get(outputDir, "playlist.m3u8").toString();

        FFmpegBuilder builder = new FFmpegBuilder()
                .setInput(inputFile)
                .addOutput(playlistFile)
                .setVideoCodec("libx264")
                .setVideoFrameRate(24)
                .setVideoResolution(quality.getWidth(), quality.getHeight())
                .setVideoBitRate(quality.getBitrateKbps() * 1000)
                .setAudioCodec("aac")
                .setAudioBitRate(128_000)
                .setFormat("hls")
                .addExtraArgs("-hls_time", String.valueOf(hlsSegmentDuration))
                .addExtraArgs("-hls_list_size", "0")
                .addExtraArgs("-hls_segment_filename", Paths.get(outputDir, "segment_%03d.ts").toString())
                .done();

        FFmpegExecutor executor = new FFmpegExecutor(ffmpegService.getFFmpeg(), ffmpegService.getFFprobe());

        FFmpegJob ffmpegJob = executor.createJob(builder, new ProgressListener() {
            @Override
            public void progress(Progress progress) {
                double percentage = progress.out_time_ns / (double) progress.total_time_ns * 100;
                updateJobProgress(job, (int) percentage);
            }
        });

        ffmpegJob.run();

        // Upload encoded files to S3
        uploadQualityToS3(job, outputDir, quality);
    }

    private void generateThumbnails(EncodingJob job, String inputFile) throws IOException {
        logger.info("Generating thumbnails for job {}", job.getId());

        String thumbnailDir = createThumbnailDirectory(job);

        for (VideoQuality quality : VideoQuality.values()) {
            String thumbnailFile = Paths.get(thumbnailDir, "thumbnail_" + quality.getLabel() + ".jpg").toString();

            FFmpegBuilder builder = new FFmpegBuilder()
                    .setInput(inputFile)
                    .addOutput(thumbnailFile)
                    .setVideoFrames(1)
                    .setVideoFilter("scale=" + quality.getWidth() + ":" + quality.getHeight())
                    .addExtraArgs("-ss", "00:00:10")
                    .done();

            FFmpegExecutor executor = new FFmpegExecutor(ffmpegService.getFFmpeg(), ffmpegService.getFFprobe());
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

    private void uploadQualityToS3(EncodingJob job, String outputDir, VideoQuality quality) throws IOException {
        String s3Prefix = "encoded/" + job.getVideoId() + "/" + quality.getFolder() + "/";
        
        File dir = new File(outputDir);
        File[] files = dir.listFiles();
        
        if (files != null) {
            for (File file : files) {
                String s3Key = s3Prefix + file.getName();
                s3Service.uploadFile(file.getAbsolutePath(), s3Key);
                logger.info("Uploaded {} to S3: {}", file.getName(), s3Key);
            }
        }
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
        try {
            Path tempPath = Paths.get(tempDirectory, job.getId().toString());
            if (Files.exists(tempPath)) {
                Files.walk(tempPath)
                        .map(Path::toFile)
                        .forEach(File::delete);
                Files.delete(tempPath);
                logger.info("Cleaned up temp files for job: {}", job.getId());
            }
        } catch (IOException e) {
            logger.warn("Failed to cleanup temp files for job {}: {}", job.getId(), e.getMessage());
        }
    }

    @Transactional
    protected void updateJobStatus(EncodingJob job, EncodingStatus status, LocalDateTime startedAt, LocalDateTime completedAt) {
        job.setStatus(status);
        if (startedAt != null) {
            job.setStartedAt(startedAt);
        }
        if (completedAt != null) {
            job.setCompletedAt(completedAt);
        }
        encodingJobRepository.save(job);
    }

    @Transactional
    protected void updateJobProgress(EncodingJob job, int progress) {
        job.setProgress(Math.min(100, Math.max(0, progress)));
        encodingJobRepository.save(job);
    }

    @Transactional
    protected void handleJobError(String jobId, String errorMessage) {
        try {
            EncodingJob job = encodingJobRepository.findById(java.util.UUID.fromString(jobId))
                    .orElse(null);
            
            if (job != null) {
                job.setStatus(EncodingStatus.FAILED);
                job.setErrorMessage(errorMessage);
                job.setRetryCount(job.getRetryCount() + 1);
                encodingJobRepository.save(job);
                
                cleanupTempFiles(job);
            }
        } catch (Exception e) {
            logger.error("Error handling job error for {}: {}", jobId, e.getMessage(), e);
        }
    }
} 