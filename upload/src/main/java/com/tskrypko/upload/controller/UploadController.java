package com.tskrypko.upload.controller;

import com.tskrypko.upload.dto.UploadRequest;
import com.tskrypko.upload.dto.UploadResponse;
import com.tskrypko.upload.model.Video;
import com.tskrypko.upload.service.CurrentUserService;
import com.tskrypko.upload.service.VideoUploadService;
import com.tskrypko.upload.service.VideoManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class UploadController {

    private static final Logger logger = LoggerFactory.getLogger(UploadController.class);

    private final VideoUploadService videoUploadService;
    private final VideoManagementService videoManagementService;
    private final CurrentUserService currentUserService;

    @PostMapping(value = "/video", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UploadResponse> uploadVideo(
            @RequestParam("file") MultipartFile file,
            @Valid @ModelAttribute UploadRequest request) {

        try {
            String userId = currentUserService.getCurrentUserId();

            UploadResponse response = videoUploadService.uploadVideo(file, request, userId);

            logger.info("Video with metadata successfully uploaded by user {}: {}",
                       userId, request.getTitle());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.warn("Invalid video upload request: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new UploadResponse(null, null, null, null, null, null, null,
                                           "Validation error: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Error uploading video: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new UploadResponse(null, null, null, null, null, null, null,
                                           "Internal server error"));
        }
    }

    @GetMapping("/video/{videoId}")
    public ResponseEntity<Video> getVideo(@PathVariable UUID videoId) {
        try {
            String userId = currentUserService.getCurrentUserId();

            Optional<Video> video = videoUploadService.getVideo(videoId, userId);

            return video.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());

        } catch (Exception e) {
            logger.error("Error getting video ID={}: {}", videoId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/videos")
    public ResponseEntity<List<Video>> getUserVideos() {
        try {
            String userId = currentUserService.getCurrentUserId();

            List<Video> videos = videoUploadService.getUserVideos(userId);

            return ResponseEntity.ok(videos);

        } catch (Exception e) {
            logger.error("Error getting user videos: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/video/{videoId}")
    public ResponseEntity<String> deleteVideo(@PathVariable UUID videoId) {
        try {
            String userId = currentUserService.getCurrentUserId();

            boolean deleted = videoUploadService.deleteVideo(videoId, userId);

            if (deleted) {
                return ResponseEntity.ok("Video successfully deleted");
            } else {
                return ResponseEntity.notFound().build();
            }

        } catch (Exception e) {
            logger.error("Error deleting video ID={}: {}", videoId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error deleting video");
        }
    }

    @PostMapping("/video/{videoId}/restore")
    public ResponseEntity<String> restoreVideo(@PathVariable UUID videoId) {
        try {
            String userId = currentUserService.getCurrentUserId();

            boolean restored = videoManagementService.restoreVideo(videoId, userId);

            if (restored) {
                return ResponseEntity.ok("Video successfully restored");
            } else {
                return ResponseEntity.badRequest()
                        .body("Video cannot be restored (not found or not deleted)");
            }

        } catch (Exception e) {
            logger.error("Error restoring video ID={}: {}", videoId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error restoring video");
        }
    }

    @DeleteMapping("/video/{videoId}/permanent")
    public ResponseEntity<String> permanentlyDeleteVideo(@PathVariable UUID videoId) {
        try {
            String userId = currentUserService.getCurrentUserId();

            boolean deleted = videoManagementService.permanentlyDeleteVideo(videoId, userId);

            if (deleted) {
                return ResponseEntity.ok("Video permanently deleted");
            } else {
                return ResponseEntity.badRequest()
                        .body("Video cannot be permanently deleted (not found or not soft deleted)");
            }

        } catch (Exception e) {
            logger.error("Error permanently deleting video ID={}: {}", videoId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error permanently deleting video");
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Upload Service is running");
    }
}
