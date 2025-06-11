--liquibase formatted sql

--changeset TymofiiSkrypko:metadata-create-video-tags-table context:metadata-service
CREATE TABLE video_tags
(
    id          UUID PRIMARY KEY,
    video_id    UUID NOT NULL,
    tag_name    VARCHAR(100) NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    modified_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by  VARCHAR(255) NOT NULL,
    modified_by VARCHAR(255) NOT NULL,
    deleted_at  TIMESTAMP WITH TIME ZONE,
    
    -- Foreign key reference to videos table from upload service
    CONSTRAINT fk_video_tags_video_id FOREIGN KEY (video_id) REFERENCES videos(id) ON DELETE CASCADE
);

--changeset TymofiiSkrypko:metadata-create-video-analytics-table context:metadata-service
CREATE TABLE video_analytics
(
    id              UUID PRIMARY KEY,
    video_id        UUID NOT NULL UNIQUE,
    view_count      BIGINT NOT NULL DEFAULT 0,
    like_count      BIGINT NOT NULL DEFAULT 0,
    dislike_count   BIGINT NOT NULL DEFAULT 0,
    comment_count   BIGINT NOT NULL DEFAULT 0,
    share_count     BIGINT NOT NULL DEFAULT 0,
    average_rating  DECIMAL(3,2) DEFAULT 0.00,
    last_viewed_at  TIMESTAMP WITH TIME ZONE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    modified_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(255) NOT NULL,
    modified_by     VARCHAR(255) NOT NULL,
    deleted_at      TIMESTAMP WITH TIME ZONE,
    
    -- Foreign key reference to videos table from upload service
    CONSTRAINT fk_video_analytics_video_id FOREIGN KEY (video_id) REFERENCES videos(id) ON DELETE CASCADE
);

--changeset TymofiiSkrypko:metadata-create-video-metadata-table context:metadata-service
CREATE TABLE video_metadata
(
    id              UUID PRIMARY KEY,
    video_id        UUID NOT NULL UNIQUE,
    duration_seconds INTEGER,
    thumbnail_url   VARCHAR(512),
    language        VARCHAR(10),
    privacy_level   VARCHAR(20) NOT NULL DEFAULT 'PUBLIC',
    age_restriction VARCHAR(10),
    location        VARCHAR(255),
    is_monetizable  BOOLEAN DEFAULT false,
    search_vector   TSVECTOR,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    modified_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(255) NOT NULL,
    modified_by     VARCHAR(255) NOT NULL,
    deleted_at      TIMESTAMP WITH TIME ZONE,
    
    -- Foreign key reference to videos table from upload service
    CONSTRAINT fk_video_metadata_video_id FOREIGN KEY (video_id) REFERENCES videos(id) ON DELETE CASCADE
);

--changeset TymofiiSkrypko:metadata-create-video-metadata-indexes context:metadata-service
CREATE INDEX idx_video_tags_video_id ON video_tags(video_id);
CREATE INDEX idx_video_tags_tag_name ON video_tags(tag_name);
CREATE INDEX idx_video_tags_video_tag ON video_tags(video_id, tag_name);

CREATE INDEX idx_video_analytics_video_id ON video_analytics(video_id);
CREATE INDEX idx_video_analytics_view_count ON video_analytics(view_count DESC);
CREATE INDEX idx_video_analytics_rating ON video_analytics(average_rating DESC);

CREATE INDEX idx_video_metadata_video_id ON video_metadata(video_id);
CREATE INDEX idx_video_metadata_privacy ON video_metadata(privacy_level);
CREATE INDEX idx_video_metadata_language ON video_metadata(language);
CREATE INDEX idx_video_metadata_search ON video_metadata USING gin(search_vector); 