package com.tskrypko.upload.service;

import com.tskrypko.upload.model.Video;
import com.tskrypko.upload.model.VideoStatus;
import com.tskrypko.upload.repository.VideoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class VideoManagementService {

    private static final Logger logger = LoggerFactory.getLogger(VideoManagementService.class);

    @Autowired
    private VideoRepository videoRepository;

    public boolean restoreVideo(UUID videoId, String userId) {
        Optional<Video> videoOpt = videoRepository.findByIdAndUserIdIncludingDeleted(videoId, userId);
        
        if (videoOpt.isPresent()) {
            Video video = videoOpt.get();
            
            if (!video.isDeleted()) {
                logger.warn("Video is not deleted: ID={}", videoId);
                return false;
            }
            
            try {
                video.setDeletedAt(null);
                video.setStatus(VideoStatus.UPLOADED);
                videoRepository.save(video);
                
                logger.info("Video restored: ID={}", videoId);
                return true;
                
            } catch (Exception e) {
                logger.error("Error restoring video ID={}: {}", videoId, e.getMessage(), e);
                return false;
            }
        }
        
        return false;
    }

    public boolean permanentlyDeleteVideo(UUID videoId, String userId) {
        Optional<Video> videoOpt = videoRepository.findByIdAndUserIdIncludingDeleted(videoId, userId);
        
        if (videoOpt.isPresent()) {
            Video video = videoOpt.get();
            
            if (!video.isDeleted()) {
                logger.warn("Video must be soft deleted first: ID={}", videoId);
                return false;
            }
            
            try {
                videoRepository.delete(video);
                logger.info("Video permanently deleted: ID={}", videoId);
                return true;
                
            } catch (Exception e) {
                logger.error("Error permanently deleting video ID={}: {}", videoId, e.getMessage(), e);
                return false;
            }
        }
        
        return false;
    }
} 