--liquibase formatted sql

--changeset TymofiiSkrypko:upload-add-soft-delete-to-videos context:upload-service
ALTER TABLE videos ADD COLUMN deleted_at TIMESTAMP WITH TIME ZONE;

--changeset TymofiiSkrypko:upload-create-soft-delete-index context:upload-service
CREATE INDEX idx_videos_deleted_at ON videos(deleted_at) WHERE deleted_at IS NOT NULL; 