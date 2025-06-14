package com.tskrypko.upload.service;

import com.tskrypko.upload.dto.UploadRequest;
import com.tskrypko.upload.dto.UploadResponse;
import com.tskrypko.upload.dto.VideoUploadRequest;
import com.tskrypko.upload.model.Video;
import com.tskrypko.upload.model.VideoStatus;
import com.tskrypko.upload.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class VideoUploadService extends BaseVideoService {

    private static final Logger logger = LoggerFactory.getLogger(VideoUploadService.class);

    private final S3Service s3Service;
    private final TransactionTemplate transactionTemplate;

    public VideoUploadService(VideoRepository videoRepository, MessagePublisher messagePublisher,
                            S3Service s3Service, TransactionTemplate transactionTemplate) {
        super(videoRepository, messagePublisher);
        this.s3Service = s3Service;
        this.transactionTemplate = transactionTemplate;
    }

    public UploadResponse uploadVideo(MultipartFile file, UploadRequest request, String userId) {
        logger.info("Starting video upload for user {}: {}", userId, request.getTitle());

        // Use inherited validation
        validateVideoFile(file);

        String s3Key = null;
        Video savedVideo = null;
        
        try {
            s3Key = s3Service.uploadFile(file, userId);
            logger.info("File uploaded to S3: {}", s3Key);

            // Use inherited method to create video record
            Video video = createVideoRecord(
                request.getTitle(),
                request.getDescription(),
                file.getOriginalFilename(),
                file.getSize(),
                file.getContentType(),
                userId,
                s3Key
            );

            savedVideo = transactionTemplate.execute(status -> {
                Video saved = videoRepository.save(video);
                logger.info("Video record saved to database: ID={}", saved.getId());
                return saved;
            });

            logger.info("Video successfully uploaded: ID={}, S3Key={}", savedVideo.getId(), s3Key);

        } catch (Exception e) {
            logger.error("Error uploading video: {}", e.getMessage(), e);

            if (s3Key != null) {
                try {
                    s3Service.deleteFile(s3Key);
                    logger.info("S3 file cleaned up after error: {}", s3Key);
                } catch (Exception cleanupEx) {
                    logger.error("Failed to cleanup S3 file: {}", s3Key, cleanupEx);
                }
            }

            throw new RuntimeException("Failed to upload video: " + e.getMessage(), e);
        }

        // IMPORTANT: Send message AFTER transaction is committed
        // This prevents data inconsistency if message sending fails
        if (savedVideo != null) {
            sendToEncodingQueueSafely(savedVideo, "Regular");
        }
        
        // Use inherited method to create response
        return createUploadResponse(savedVideo, "Video successfully uploaded and sent for processing");
    }

    // Alternative method using unified DTO
    public UploadResponse uploadVideo(MultipartFile file, VideoUploadRequest request, String userId) {
        // Convert to old format for backward compatibility
        UploadRequest oldRequest = new UploadRequest(request.getTitle(), request.getDescription());
        return uploadVideo(file, oldRequest, userId);
    }

    @Transactional(readOnly = true)
    public Optional<Video> getVideo(UUID videoId, String userId) {
        return videoRepository.findByIdAndUserId(videoId, userId);
    }

    @Transactional(readOnly = true)
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
}
