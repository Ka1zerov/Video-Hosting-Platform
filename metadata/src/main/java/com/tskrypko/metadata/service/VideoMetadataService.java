package com.tskrypko.metadata.service;

import com.tskrypko.metadata.dto.VideoMetadataDto;
import com.tskrypko.metadata.model.Video;
import com.tskrypko.metadata.model.VideoStatus;
import com.tskrypko.metadata.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class VideoMetadataService {

    private final VideoRepository videoRepository;

    /**
     * Получить все готовые видео (основной метод MVP)
     */
    public Page<VideoMetadataDto> getAllReadyVideos(Pageable pageable) {
        log.debug("Getting all ready videos, page: {}, size: {}", pageable.getPageNumber(), pageable.getPageSize());
        
        Page<Video> videos = videoRepository.findByStatusAndDeletedAtIsNullOrderByUploadedAtDesc(
                VideoStatus.READY, pageable);
        
        return videos.map(this::convertToDto);
    }

    /**
     * Получить видео по ID
     */
    public Optional<VideoMetadataDto> getVideoById(UUID videoId) {
        log.debug("Getting video by id: {}", videoId);
        
        return videoRepository.findByIdAndStatusAndDeletedAtIsNull(videoId, VideoStatus.READY)
                .map(this::convertToDto);
    }

    /**
     * Поиск видео по названию
     */
    public Page<VideoMetadataDto> searchVideos(String title, Pageable pageable) {
        log.debug("Searching videos by title: '{}', page: {}, size: {}", 
                title, pageable.getPageNumber(), pageable.getPageSize());
        
        Page<Video> videos = videoRepository.searchByTitleAndStatus(title, VideoStatus.READY, pageable);
        
        return videos.map(this::convertToDto);
    }

    /**
     * Получить популярные видео
     */
    public Page<VideoMetadataDto> getPopularVideos(Pageable pageable) {
        log.debug("Getting popular videos, page: {}, size: {}", pageable.getPageNumber(), pageable.getPageSize());
        
        Page<Video> videos = videoRepository.findByStatusAndDeletedAtIsNullOrderByViewsCountDescUploadedAtDesc(
                VideoStatus.READY, pageable);
        
        return videos.map(this::convertToDto);
    }

    /**
     * Получить недавно просмотренные видео
     */
    public Page<VideoMetadataDto> getRecentlyWatchedVideos(Pageable pageable) {
        log.debug("Getting recently watched videos, page: {}, size: {}", pageable.getPageNumber(), pageable.getPageSize());
        
        Page<Video> videos = videoRepository.findByStatusAndDeletedAtIsNullAndLastAccessedIsNotNullOrderByLastAccessedDesc(
                VideoStatus.READY, pageable);
        
        return videos.map(this::convertToDto);
    }

    /**
     * Конвертация Video в DTO
     */
    private VideoMetadataDto convertToDto(Video video) {
        return new VideoMetadataDto(
                video.getId(),
                video.getTitle(),
                video.getDescription(),
                video.getDuration(),
                video.getThumbnailUrl(),
                video.getUploadedAt(),
                video.getViewsCount(),
                video.getLastAccessed()
        );
    }
} 