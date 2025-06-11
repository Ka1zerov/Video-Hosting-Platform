--liquibase formatted sql

--changeset TymofiiSkrypko:add-soft-delete-to-videos
ALTER TABLE videos ADD COLUMN deleted_at TIMESTAMP WITH TIME ZONE;

--changeset TymofiiSkrypko:create-soft-delete-index
CREATE INDEX idx_videos_deleted_at ON videos(deleted_at) WHERE deleted_at IS NOT NULL; 