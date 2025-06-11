package com.tskrypko.metadata.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "video_metadata")
@Getter
@Setter
public class VideoMetadata extends BaseEntity {

    @Column(name = "video_id", nullable = false, unique = true)
    private UUID videoId;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "thumbnail_url", length = 512)
    private String thumbnailUrl;

    @Column(name = "language", length = 10)
    private String language;

    @Column(name = "privacy_level", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private PrivacyLevel privacyLevel = PrivacyLevel.PUBLIC;

    @Column(name = "age_restriction", length = 10)
    @Enumerated(EnumType.STRING)
    private AgeRestriction ageRestriction;

    @Column(name = "location")
    private String location;

    @Column(name = "is_monetizable")
    private Boolean isMonetizable = false;

    // Note: search_vector is handled by database triggers, not JPA
} 