package com.tskrypko.encoding.service;

import com.tskrypko.encoding.model.EncodingJob;
import com.tskrypko.encoding.model.EncodingStatus;
import com.tskrypko.encoding.repository.EncodingJobRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EncodingJobService {

    private static final Logger logger = LoggerFactory.getLogger(EncodingJobService.class);

    private final EncodingJobRepository encodingJobRepository;
    private final VideoEncodingService videoEncodingService;

    @Transactional(readOnly = true)
    public Optional<EncodingJob> getJob(UUID jobId) {
        return encodingJobRepository.findById(jobId);
    }

    @Transactional(readOnly = true)
    public Optional<EncodingJob> getJobByVideoId(UUID videoId) {
        return encodingJobRepository.findByVideoId(videoId);
    }

    @Transactional(readOnly = true)
    public List<EncodingJob> getJobsByStatus(EncodingStatus status) {
        return encodingJobRepository.findByStatus(status);
    }

    @Transactional(readOnly = true)
    public List<EncodingJob> getJobsByUserId(String userId) {
        return encodingJobRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public List<EncodingJob> getAllJobs() {
        return encodingJobRepository.findAll();
    }

    @Transactional
    public boolean retryJob(UUID jobId) {
        Optional<EncodingJob> jobOpt = encodingJobRepository.findById(jobId);
        
        if (jobOpt.isPresent()) {
            EncodingJob job = jobOpt.get();
            
            if (job.getStatus() == EncodingStatus.FAILED || job.getStatus() == EncodingStatus.RETRY) {
                job.setStatus(EncodingStatus.PENDING);
                job.setErrorMessage(null);
                job.setProgress(0);
                encodingJobRepository.save(job);
                
                // Restart encoding process
                videoEncodingService.processEncodingJob(job.getId().toString());
                
                logger.info("Job retry initiated: {}", jobId);
                return true;
            }
        }
        
        return false;
    }

    @Transactional
    public boolean cancelJob(UUID jobId) {
        Optional<EncodingJob> jobOpt = encodingJobRepository.findById(jobId);
        
        if (jobOpt.isPresent()) {
            EncodingJob job = jobOpt.get();
            
            if (job.getStatus() == EncodingStatus.PENDING || job.getStatus() == EncodingStatus.RETRY) {
                job.markAsDeleted();
                encodingJobRepository.save(job);
                
                logger.info("Job cancelled: {}", jobId);
                return true;
            }
        }
        
        return false;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getEncodingStats() {
        Map<String, Object> stats = new HashMap<>();
        
        for (EncodingStatus status : EncodingStatus.values()) {
            long count = encodingJobRepository.countByStatus(status);
            stats.put(status.name().toLowerCase(), count);
        }
        
        return stats;
    }
} 