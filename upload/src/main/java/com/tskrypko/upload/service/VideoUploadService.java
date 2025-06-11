package com.tskrypko.upload.service;

import com.tskrypko.upload.dto.UploadRequest;
import com.tskrypko.upload.dto.UploadResponse;
import com.tskrypko.upload.model.Video;
import com.tskrypko.upload.model.VideoStatus;
import com.tskrypko.upload.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class VideoUploadService {

    private static final Logger logger = LoggerFactory.getLogger(VideoUploadService.class);

    private static final List<String> ALLOWED_VIDEO_TYPES = Arrays.asList(
            "video/mp4", "video/avi", "video/mov", "video/wmv", "video/flv",
            "video/webm", "video/mkv", "video/m4v"
    );

    private static final long MAX_FILE_SIZE = 2L * 1024 * 1024 * 1024;

    private final VideoRepository videoRepository;
    private final S3Service s3Service;
    private final MessagePublisher messagePublisher;

    public UploadResponse uploadVideo(MultipartFile file, UploadRequest request, String userId) {

        logger.info("Starting video upload for user {}: {}", userId, request.getTitle());

        validateVideoFile(file);

        try {
            String s3Key = s3Service.uploadFile(file, userId);
            Video video = createVideoRecord(file, request, userId, s3Key);
            Video savedVideo = videoRepository.save(video);
            sendEncodingMessage(savedVideo);

            logger.info("Video successfully uploaded: ID={}, S3Key={}", savedVideo.getId(), s3Key);

            return createUploadResponse(savedVideo);

        } catch (Exception e) {
            logger.error("Error uploading video: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to upload video: " + e.getMessage(), e);
        }
    }

    public Optional<Video> getVideo(UUID videoId, String userId) {
        return videoRepository.findByIdAndUserId(videoId, userId);
    }

    public List<Video> getUserVideos(String userId) {
        return videoRepository.findByUserIdOrderByUploadedAtDesc(userId);
    }

    @Transactional
    public boolean deleteVideo(UUID videoId, String userId) {
        Optional<Video> videoOpt = videoRepository.findByIdAndUserIdIncludingDeleted(videoId, userId);

        if (videoOpt.isPresent()) {
            Video video = videoOpt.get();

            if (video.isDeleted()) {
                logger.warn("Video already deleted: ID={}", videoId);
                return false;
            }

            try {
                if (video.getS3Key() != null) {
                    s3Service.deleteFile(video.getS3Key());
                    logger.info("S3 file deleted: {}", video.getS3Key());
                }

                video.markAsDeleted();
                video.setStatus(VideoStatus.DELETED);
                videoRepository.save(video);

                logger.info("Video soft deleted: ID={}", videoId);
                return true;

            } catch (Exception e) {
                logger.error("Error deleting video ID={}: {}", videoId, e.getMessage(), e);
                return false;
            }
        }

        return false;
    }

    private void validateVideoFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds maximum allowed size (2GB)");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_VIDEO_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Unsupported file type. Allowed: " +
                                             String.join(", ", ALLOWED_VIDEO_TYPES));
        }

        String filename = file.getOriginalFilename();
        if (filename == null || filename.trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid filename");
        }
    }

    private Video createVideoRecord(MultipartFile file, UploadRequest request, String userId, String s3Key) {
        Video video = new Video();
        video.setTitle(request.getTitle());
        video.setDescription(request.getDescription());
        video.setOriginalFilename(file.getOriginalFilename());
        video.setFileSize(file.getSize());
        video.setMimeType(file.getContentType());
        video.setUserId(userId);
        video.setS3Key(s3Key);
        video.setStatus(VideoStatus.UPLOADED);

        return video;
    }

    private void sendEncodingMessage(Video video) {
        try {
            messagePublisher.publishVideoUploadedMessage(video);
            logger.info("Video upload message sent to queue: videoId={}", video.getId());
        } catch (Exception e) {
            logger.error("Error sending message to queue: {}", e.getMessage(), e);
        }
    }

    private UploadResponse createUploadResponse(Video video) {
        return new UploadResponse(
                video.getId(),
                video.getTitle(),
                video.getDescription(),
                video.getOriginalFilename(),
                video.getFileSize(),
                video.getStatus(),
                video.getUploadedAt(),
                "Video successfully uploaded and sent for processing"
        );
    }
}
