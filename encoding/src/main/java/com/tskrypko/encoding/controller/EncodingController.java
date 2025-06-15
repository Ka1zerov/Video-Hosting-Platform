package com.tskrypko.encoding.controller;

import com.tskrypko.encoding.model.EncodingJob;
import com.tskrypko.encoding.model.EncodingStatus;
import com.tskrypko.encoding.service.EncodingJobService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/encoding")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class EncodingController {

    private static final Logger logger = LoggerFactory.getLogger(EncodingController.class);

    private final EncodingJobService encodingJobService;

    @GetMapping("/job/{jobId}")
    public ResponseEntity<EncodingJob> getJob(@PathVariable UUID jobId) {
        try {
            Optional<EncodingJob> job = encodingJobService.getJob(jobId);
            return job.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
        } catch (Exception e) {
            logger.error("Error getting job {}: {}", jobId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/job/video/{videoId}")
    public ResponseEntity<EncodingJob> getJobByVideoId(@PathVariable UUID videoId) {
        try {
            Optional<EncodingJob> job = encodingJobService.getJobByVideoId(videoId);
            return job.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
        } catch (Exception e) {
            logger.error("Error getting job for video {}: {}", videoId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/jobs")
    public ResponseEntity<List<EncodingJob>> getJobs(
            @RequestParam(required = false) EncodingStatus status,
            @RequestParam(required = false) String userId) {
        try {
            List<EncodingJob> jobs;
            
            if (status != null) {
                jobs = encodingJobService.getJobsByStatus(status);
            } else if (userId != null) {
                jobs = encodingJobService.getJobsByUserId(userId);
            } else {
                jobs = encodingJobService.getAllJobs();
            }
            
            return ResponseEntity.ok(jobs);
        } catch (Exception e) {
            logger.error("Error getting jobs: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/job/{jobId}/retry")
    public ResponseEntity<String> retryJob(@PathVariable UUID jobId) {
        try {
            boolean retried = encodingJobService.retryJob(jobId);
            if (retried) {
                return ResponseEntity.ok("Job retry initiated");
            } else {
                return ResponseEntity.badRequest().body("Job cannot be retried");
            }
        } catch (Exception e) {
            logger.error("Error retrying job {}: {}", jobId, e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Error retrying job");
        }
    }

    @DeleteMapping("/job/{jobId}")
    public ResponseEntity<String> cancelJob(@PathVariable UUID jobId) {
        try {
            boolean cancelled = encodingJobService.cancelJob(jobId);
            if (cancelled) {
                return ResponseEntity.ok("Job cancelled");
            } else {
                return ResponseEntity.badRequest().body("Job cannot be cancelled");
            }
        } catch (Exception e) {
            logger.error("Error cancelling job {}: {}", jobId, e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Error cancelling job");
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<Object> getStats() {
        try {
            var stats = encodingJobService.getEncodingStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.error("Error getting encoding stats: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Encoding Service is running");
    }
} 