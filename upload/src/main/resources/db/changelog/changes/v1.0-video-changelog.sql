--liquibase formatted sql

--changeset TymofiiSkrypko:create-videos-table
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
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    modified_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by          VARCHAR(255) NOT NULL,
    modified_by         VARCHAR(255) NOT NULL
);

--changeset TymofiiSkrypko:create-videos-indexes
CREATE INDEX idx_videos_user_id ON videos(user_id);
CREATE INDEX idx_videos_status ON videos(status);
CREATE INDEX idx_videos_uploaded_at ON videos(uploaded_at);
CREATE INDEX idx_videos_user_status ON videos(user_id, status); 