package com.tskrypko.upload.controller;

import com.tskrypko.upload.dto.*;
import com.tskrypko.upload.model.MultipartUploadSession;
import com.tskrypko.upload.service.CurrentUserService;
import com.tskrypko.upload.service.MultipartUploadService;
import com.tskrypko.upload.service.MultipartCleanupService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/upload/multipart")
@RequiredArgsConstructor
public class MultipartUploadController {

    private static final Logger logger = LoggerFactory.getLogger(MultipartUploadController.class);

    private final MultipartUploadService multipartUploadService;
    private final CurrentUserService currentUserService;
    private final MultipartCleanupService cleanupService;

    /**
     * Initialize multipart upload
     */
    @PostMapping("/initiate")
    public ResponseEntity<MultipartUploadResponse> initiateMultipartUpload(
            @Valid @RequestBody MultipartUploadRequest request) {

        try {
            String userId = currentUserService.getCurrentUserId();

            MultipartUploadResponse response = multipartUploadService.initiateMultipartUpload(request, userId);

            logger.info("Multipart upload initiated for user {}: uploadId={}", userId, response.getUploadId());
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.warn("Invalid multipart upload request: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new MultipartUploadResponse(null, null, "Validation error: " + e.getMessage(), null, null));
        } catch (Exception e) {
            logger.error("Error initiating multipart upload: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MultipartUploadResponse(null, null, "Internal server error", null, null));
        }
    }

    /**
     * Upload individual chunk
     */
    @PostMapping(value = "/upload-chunk", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ChunkUploadResponse> uploadChunk(
            @RequestParam("chunk") MultipartFile chunk,
            @RequestParam("uploadId") @NotBlank String uploadId,
            @RequestParam("partNumber") @NotNull @Min(1) Integer partNumber) {

        try {
            ChunkUploadResponse response = multipartUploadService.uploadChunk(uploadId, partNumber, chunk);

            logger.info("Chunk uploaded successfully: uploadId={}, partNumber={}, progress={}/{}",
                    uploadId, partNumber, response.getUploadedParts(), response.getTotalParts());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.warn("Invalid chunk upload request: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ChunkUploadResponse(null, partNumber, "Validation error: " + e.getMessage(), null, null));
        } catch (Exception e) {
            logger.error("Error uploading chunk: uploadId={}, partNumber={}, error={}",
                    uploadId, partNumber, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ChunkUploadResponse(null, partNumber, "Internal server error", null, null));
        }
    }

    /**
     * Complete multipart upload
     */
    @PostMapping("/complete/{uploadId}")
    public ResponseEntity<UploadResponse> completeMultipartUpload(@PathVariable String uploadId) {

        try {
            UploadResponse response = multipartUploadService.completeMultipartUpload(uploadId);

            logger.info("Multipart upload completed successfully: uploadId={}, videoId={}",
                    uploadId, response.getId());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.warn("Invalid complete request for uploadId {}: {}", uploadId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new UploadResponse(null, null, null, null, null, null, null,
                            "Validation error: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Error completing multipart upload: uploadId={}, error={}",
                    uploadId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new UploadResponse(null, null, null, null, null, null, null,
                            "Internal server error"));
        }
    }

    /**
     * Abort multipart upload
     */
    @DeleteMapping("/abort/{uploadId}")
    public ResponseEntity<String> abortMultipartUpload(@PathVariable String uploadId) {

        try {
            multipartUploadService.abortMultipartUpload(uploadId);

            logger.info("Multipart upload aborted: uploadId={}", uploadId);
            return ResponseEntity.ok("Multipart upload aborted successfully");

        } catch (Exception e) {
            logger.error("Error aborting multipart upload: uploadId={}, error={}",
                    uploadId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error aborting multipart upload");
        }
    }

    /**
     * Get multipart upload status
     */
    @GetMapping("/status/{uploadId}")
    public ResponseEntity<MultipartUploadSession> getUploadStatus(@PathVariable String uploadId) {

        try {
            MultipartUploadSession session = multipartUploadService.getUploadStatus(uploadId);

            if (session == null) {
                return ResponseEntity.notFound().build();
            }

            logger.debug("Upload status requested: uploadId={}, progress={:.2f}%",
                    uploadId, session.getProgressPercentage());

            return ResponseEntity.ok(session);

        } catch (Exception e) {
            logger.error("Error getting upload status: uploadId={}, error={}",
                    uploadId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Health check for multipart upload
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Multipart Upload Service is running");
    }

    /**
     * Get cleanup statistics - admin endpoint
     */
    @GetMapping("/admin/cleanup-stats")
    public ResponseEntity<MultipartCleanupService.CleanupStats> getCleanupStats() {
        try {
            MultipartCleanupService.CleanupStats stats = cleanupService.getCleanupStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.error("Error getting cleanup stats", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Manually trigger cleanup - admin endpoint
     */
    @PostMapping("/admin/cleanup")
    public ResponseEntity<String> triggerCleanup() {
        try {
            cleanupService.cleanupExpiredSessions();
            return ResponseEntity.ok("Cleanup triggered successfully");
        } catch (Exception e) {
            logger.error("Error triggering cleanup", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to trigger cleanup: " + e.getMessage());
        }
    }

    /**
     * Manually cleanup specific session - admin endpoint
     */
    @DeleteMapping("/admin/cleanup/{uploadId}")
    public ResponseEntity<String> cleanupSession(@PathVariable("uploadId") String uploadId) {
        try {
            boolean cleaned = cleanupService.cleanupSession(uploadId);
            if (cleaned) {
                return ResponseEntity.ok("Session cleaned successfully");
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Error cleaning session: {}", uploadId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to cleanup session: " + e.getMessage());
        }
    }
} 