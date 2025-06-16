-- liquibase formatted sql

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

-- changeset TymofiiSkrypko:004-create-view-sessions-indexes context:streaming-service
CREATE INDEX idx_view_sessions_video_id ON view_sessions(video_id);
CREATE INDEX idx_view_sessions_user_id ON view_sessions(user_id);
CREATE INDEX idx_view_sessions_session_id ON view_sessions(session_id);
CREATE INDEX idx_view_sessions_started_at ON view_sessions(started_at);
CREATE INDEX idx_view_sessions_last_heartbeat ON view_sessions(last_heartbeat);
CREATE INDEX idx_view_sessions_ended_at ON view_sessions(ended_at);
CREATE INDEX idx_view_sessions_ip_address ON view_sessions(ip_address);

-- changeset TymofiiSkrypko:006-add-view-sessions-constraints context:streaming-service
-- Foreign key to videos table
ALTER TABLE view_sessions 
ADD CONSTRAINT fk_view_sessions_video_id 
FOREIGN KEY (video_id) REFERENCES videos(id) 
ON DELETE CASCADE ON UPDATE CASCADE;

-- changeset TymofiiSkrypko:007-add-check-constraints context:streaming-service
-- View sessions check constraints
ALTER TABLE view_sessions 
ADD CONSTRAINT chk_view_sessions_quality 
CHECK (quality IN ('AUTO', 'Q_480P', 'Q_720P', 'Q_1080P'));

ALTER TABLE view_sessions 
ADD CONSTRAINT chk_view_sessions_durations 
CHECK (watch_duration >= 0 AND max_position >= 0); 