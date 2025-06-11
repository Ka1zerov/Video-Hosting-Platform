--liquibase formatted sql

--changeset TymofiiSkrypko:metadata-create-playlists-table context:metadata-service
CREATE TABLE playlists
(
    id          UUID PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    user_id     VARCHAR(255) NOT NULL,
    is_public   BOOLEAN NOT NULL DEFAULT false,
    thumbnail_url VARCHAR(512),
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    modified_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by  VARCHAR(255) NOT NULL,
    modified_by VARCHAR(255) NOT NULL,
    deleted_at  TIMESTAMP WITH TIME ZONE
);

--changeset TymofiiSkrypko:metadata-create-playlist-videos-table context:metadata-service
CREATE TABLE playlist_videos
(
    id          UUID PRIMARY KEY,
    playlist_id UUID NOT NULL,
    video_id    UUID NOT NULL,
    position    INTEGER NOT NULL,
    added_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    modified_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by  VARCHAR(255) NOT NULL,
    modified_by VARCHAR(255) NOT NULL,
    deleted_at  TIMESTAMP WITH TIME ZONE,
    
    -- Foreign key references
    CONSTRAINT fk_playlist_videos_playlist_id FOREIGN KEY (playlist_id) REFERENCES playlists(id) ON DELETE CASCADE,
    CONSTRAINT fk_playlist_videos_video_id FOREIGN KEY (video_id) REFERENCES videos(id) ON DELETE CASCADE,
    
    -- Unique constraint to prevent duplicate videos in the same playlist
    CONSTRAINT uk_playlist_videos_playlist_video UNIQUE (playlist_id, video_id)
);

--changeset TymofiiSkrypko:metadata-create-playlists-indexes context:metadata-service
CREATE INDEX idx_playlists_user_id ON playlists(user_id);
CREATE INDEX idx_playlists_public ON playlists(is_public);
CREATE INDEX idx_playlists_user_public ON playlists(user_id, is_public);

CREATE INDEX idx_playlist_videos_playlist_id ON playlist_videos(playlist_id);
CREATE INDEX idx_playlist_videos_video_id ON playlist_videos(video_id);
CREATE INDEX idx_playlist_videos_position ON playlist_videos(playlist_id, position); 