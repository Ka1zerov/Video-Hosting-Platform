--liquibase formatted sql

--changeset TymofiiSkrypko:upload-create-videos-table context:upload-service
CREATE TABLE videos
(
    id                  UUID PRIMARY KEY,
    title               VARCHAR(255) NOT NULL,
    description         TEXT,
    original_filename   VARCHAR(255) NOT NULL,
    file_size           BIGINT NOT NULL,
    mime_type           VARCHAR(100),
    s3_key              VARCHAR(512),
    status              VARCHAR(50) NOT NULL DEFAULT 'UPLOADED',
    uploaded_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    user_id             VARCHAR(255) NOT NULL,
    -- Streaming-specific fields
    duration            BIGINT,
    thumbnail_url       VARCHAR(500),
    hls_manifest_url    VARCHAR(500),
    dash_manifest_url   VARCHAR(500),
    views_count         BIGINT DEFAULT 0,
    last_accessed       TIMESTAMP WITH TIME ZONE,
    -- Audit fields
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    modified_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by          VARCHAR(255) NOT NULL,
    modified_by         VARCHAR(255) NOT NULL,
    deleted_at          TIMESTAMP WITH TIME ZONE
);

COMMENT ON TABLE videos IS 'Video files with upload and streaming metadata';
COMMENT ON COLUMN videos.duration IS 'Video duration in seconds';
COMMENT ON COLUMN videos.thumbnail_url IS 'URL to video thumbnail image';
COMMENT ON COLUMN videos.hls_manifest_url IS 'URL to HLS manifest file (.m3u8)';
COMMENT ON COLUMN videos.dash_manifest_url IS 'URL to DASH manifest file (.mpd)';
COMMENT ON COLUMN videos.views_count IS 'Total number of views';
COMMENT ON COLUMN videos.last_accessed IS 'Last time video was accessed for streaming';

--changeset TymofiiSkrypko:upload-create-videos-indexes context:upload-service
CREATE INDEX idx_videos_user_id ON videos(user_id);
CREATE INDEX idx_videos_status ON videos(status);
CREATE INDEX idx_videos_uploaded_at ON videos(uploaded_at);
CREATE INDEX idx_videos_user_status ON videos(user_id, status);
-- Additional streaming-optimized indexes
CREATE INDEX idx_videos_status_created_at ON videos(status, created_at DESC);
CREATE INDEX idx_videos_status_views_count ON videos(status, views_count DESC);
CREATE INDEX idx_videos_status_last_accessed ON videos(status, last_accessed DESC);
CREATE INDEX idx_videos_title_status ON videos(title, status);
-- Soft delete index
CREATE INDEX idx_videos_deleted_at ON videos(deleted_at) WHERE deleted_at IS NOT NULL; 