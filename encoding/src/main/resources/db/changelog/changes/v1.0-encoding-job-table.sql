--liquibase formatted sql

--changeset TymofiiSkrypko:encoding-create-encoding-jobs-table context:encoding-service
CREATE TABLE encoding_jobs
(
    id                  UUID PRIMARY KEY,
    video_id            UUID NOT NULL,
    user_id             VARCHAR(255) NOT NULL,
    title               VARCHAR(255) NOT NULL,
    original_filename   VARCHAR(255) NOT NULL,
    file_size           BIGINT NOT NULL,
    mime_type           VARCHAR(100),
    s3_key              VARCHAR(512) NOT NULL,
    status              VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    started_at          TIMESTAMP WITH TIME ZONE,
    completed_at        TIMESTAMP WITH TIME ZONE,
    error_message       TEXT,
    retry_count         INTEGER DEFAULT 0,
    progress            INTEGER DEFAULT 0,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    modified_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by          VARCHAR(255) NOT NULL,
    modified_by         VARCHAR(255) NOT NULL,
    deleted_at          TIMESTAMP WITH TIME ZONE
);

--changeset TymofiiSkrypko:encoding-create-encoding-jobs-indexes context:encoding-service
CREATE INDEX idx_encoding_jobs_video_id ON encoding_jobs(video_id);
CREATE INDEX idx_encoding_jobs_user_id ON encoding_jobs(user_id);
CREATE INDEX idx_encoding_jobs_status ON encoding_jobs(status);
CREATE INDEX idx_encoding_jobs_created_at ON encoding_jobs(created_at);
CREATE INDEX idx_encoding_jobs_deleted_at ON encoding_jobs(deleted_at) WHERE deleted_at IS NOT NULL;

--changeset TymofiiSkrypko:encoding-add-foreign-key-constraints context:encoding-service
-- Foreign key to videos table (created by upload service)
ALTER TABLE encoding_jobs 
ADD CONSTRAINT fk_encoding_jobs_video_id 
FOREIGN KEY (video_id) REFERENCES videos(id) 
ON DELETE CASCADE ON UPDATE CASCADE; 