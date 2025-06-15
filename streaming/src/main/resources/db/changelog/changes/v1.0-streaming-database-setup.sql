-- liquibase formatted sql

-- changeset TymofiiSkrypko:001-create-video-qualities-table context:streaming-service
CREATE TABLE video_qualities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    video_id UUID NOT NULL,
    quality_name VARCHAR(20) NOT NULL,
    width INTEGER NOT NULL,
    height INTEGER NOT NULL,
    bitrate INTEGER NOT NULL,
    file_size BIGINT,
    s3_key VARCHAR(500),
    hls_playlist_url VARCHAR(500),
    dash_representation_id VARCHAR(100),
    encoding_status VARCHAR(20) DEFAULT 'PENDING' NOT NULL,
    encoding_progress INTEGER DEFAULT 0,
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    modified_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

COMMENT ON TABLE video_qualities IS 'Different quality versions of videos (480p, 720p, 1080p)';
COMMENT ON COLUMN video_qualities.video_id IS 'Reference to videos table';
COMMENT ON COLUMN video_qualities.quality_name IS 'Quality identifier: 480p, 720p, 1080p';
COMMENT ON COLUMN video_qualities.encoding_status IS 'PENDING, IN_PROGRESS, COMPLETED, FAILED, CANCELLED';

-- changeset TymofiiSkrypko:002-create-view-sessions-table context:streaming-service
CREATE TABLE view_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    video_id UUID NOT NULL,
    user_id VARCHAR(100),
    session_id VARCHAR(100) NOT NULL UNIQUE,
    ip_address VARCHAR(45),
    user_agent TEXT,
    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    last_heartbeat TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    watch_duration BIGINT DEFAULT 0,
    max_position BIGINT DEFAULT 0,
    quality VARCHAR(20) DEFAULT 'AUTO',
    is_complete BOOLEAN DEFAULT false,
    ended_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    modified_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

COMMENT ON TABLE view_sessions IS 'Video viewing sessions for analytics';
COMMENT ON COLUMN view_sessions.user_id IS 'User ID from gateway header (can be null for anonymous)';
COMMENT ON COLUMN view_sessions.session_id IS 'Unique session identifier';
COMMENT ON COLUMN view_sessions.watch_duration IS 'Total watch time in seconds';
COMMENT ON COLUMN view_sessions.max_position IS 'Maximum position reached in seconds';

-- changeset TymofiiSkrypko:003-create-video-qualities-indexes context:streaming-service
CREATE INDEX idx_video_qualities_video_id ON video_qualities(video_id);
CREATE INDEX idx_video_qualities_status ON video_qualities(encoding_status);
CREATE INDEX idx_video_qualities_video_status ON video_qualities(video_id, encoding_status);
CREATE INDEX idx_video_qualities_quality_name ON video_qualities(video_id, quality_name);

-- changeset TymofiiSkrypko:004-create-view-sessions-indexes context:streaming-service
CREATE INDEX idx_view_sessions_video_id ON view_sessions(video_id);
CREATE INDEX idx_view_sessions_user_id ON view_sessions(user_id);
CREATE INDEX idx_view_sessions_session_id ON view_sessions(session_id);
CREATE INDEX idx_view_sessions_started_at ON view_sessions(started_at);
CREATE INDEX idx_view_sessions_last_heartbeat ON view_sessions(last_heartbeat);
CREATE INDEX idx_view_sessions_ended_at ON view_sessions(ended_at);
CREATE INDEX idx_view_sessions_ip_address ON view_sessions(ip_address);

-- changeset TymofiiSkrypko:005-add-video-qualities-constraints context:streaming-service
-- Foreign key to videos table (created by upload service)
ALTER TABLE video_qualities 
ADD CONSTRAINT fk_video_qualities_video_id 
FOREIGN KEY (video_id) REFERENCES videos(id) 
ON DELETE CASCADE ON UPDATE CASCADE;

-- Unique constraint for video quality combination
ALTER TABLE video_qualities 
ADD CONSTRAINT uq_video_qualities_video_quality 
UNIQUE (video_id, quality_name);

-- changeset TymofiiSkrypko:006-add-view-sessions-constraints context:streaming-service
-- Foreign key to videos table
ALTER TABLE view_sessions 
ADD CONSTRAINT fk_view_sessions_video_id 
FOREIGN KEY (video_id) REFERENCES videos(id) 
ON DELETE CASCADE ON UPDATE CASCADE;

-- changeset TymofiiSkrypko:007-add-check-constraints context:streaming-service
-- Video qualities check constraints
ALTER TABLE video_qualities 
ADD CONSTRAINT chk_video_qualities_encoding_status 
CHECK (encoding_status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'FAILED', 'CANCELLED'));

ALTER TABLE video_qualities 
ADD CONSTRAINT chk_video_qualities_encoding_progress 
CHECK (encoding_progress >= 0 AND encoding_progress <= 100);

ALTER TABLE video_qualities 
ADD CONSTRAINT chk_video_qualities_dimensions 
CHECK (width > 0 AND height > 0 AND bitrate > 0);

-- View sessions check constraints
ALTER TABLE view_sessions 
ADD CONSTRAINT chk_view_sessions_quality 
CHECK (quality IN ('AUTO', 'Q_480P', 'Q_720P', 'Q_1080P'));

ALTER TABLE view_sessions 
ADD CONSTRAINT chk_view_sessions_durations 
CHECK (watch_duration >= 0 AND max_position >= 0); 