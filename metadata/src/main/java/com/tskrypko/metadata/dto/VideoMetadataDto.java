package com.tskrypko.metadata.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VideoMetadataDto {
    
    private UUID id;
    private String title;
    private String description;
    private Long duration; // Duration in seconds
    private String thumbnailUrl;
    private LocalDateTime uploadedAt;
    private Long viewsCount;
    private LocalDateTime lastAccessed;
} 