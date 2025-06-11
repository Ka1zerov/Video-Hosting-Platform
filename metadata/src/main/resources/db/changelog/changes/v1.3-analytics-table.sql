--liquibase formatted sql

--changeset TymofiiSkrypko:metadata-create-video-views-table context:metadata-service
CREATE TABLE video_views
(
    id          UUID PRIMARY KEY,
    video_id    UUID NOT NULL,
    user_id     VARCHAR(255),
    ip_address  INET,
    user_agent  TEXT,
    viewed_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    watch_duration_seconds INTEGER DEFAULT 0,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    modified_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by  VARCHAR(255) NOT NULL,
    modified_by VARCHAR(255) NOT NULL,
    deleted_at  TIMESTAMP WITH TIME ZONE,
    
    -- Foreign key reference to videos table
    CONSTRAINT fk_video_views_video_id FOREIGN KEY (video_id) REFERENCES videos(id) ON DELETE CASCADE
);

--changeset TymofiiSkrypko:metadata-create-video-ratings-table context:metadata-service
CREATE TABLE video_ratings
(
    id          UUID PRIMARY KEY,
    video_id    UUID NOT NULL,
    user_id     VARCHAR(255) NOT NULL,
    rating      INTEGER NOT NULL CHECK (rating >= 1 AND rating <= 5),
    rated_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    modified_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by  VARCHAR(255) NOT NULL,
    modified_by VARCHAR(255) NOT NULL,
    deleted_at  TIMESTAMP WITH TIME ZONE,
    
    -- Foreign key reference to videos table
    CONSTRAINT fk_video_ratings_video_id FOREIGN KEY (video_id) REFERENCES videos(id) ON DELETE CASCADE,
    
    -- Unique constraint - one rating per user per video
    CONSTRAINT uk_video_ratings_user_video UNIQUE (user_id, video_id)
);

--changeset TymofiiSkrypko:metadata-create-video-comments-table context:metadata-service
CREATE TABLE video_comments
(
    id          UUID PRIMARY KEY,
    video_id    UUID NOT NULL,
    user_id     VARCHAR(255) NOT NULL,
    parent_id   UUID,
    content     TEXT NOT NULL,
    is_edited   BOOLEAN DEFAULT false,
    edited_at   TIMESTAMP WITH TIME ZONE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    modified_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by  VARCHAR(255) NOT NULL,
    modified_by VARCHAR(255) NOT NULL,
    deleted_at  TIMESTAMP WITH TIME ZONE,
    
    -- Foreign key references
    CONSTRAINT fk_video_comments_video_id FOREIGN KEY (video_id) REFERENCES videos(id) ON DELETE CASCADE,
    CONSTRAINT fk_video_comments_parent_id FOREIGN KEY (parent_id) REFERENCES video_comments(id)
);

--changeset TymofiiSkrypko:metadata-create-analytics-indexes context:metadata-service
CREATE INDEX idx_video_views_video_id ON video_views(video_id);
CREATE INDEX idx_video_views_user_id ON video_views(user_id);
CREATE INDEX idx_video_views_viewed_at ON video_views(viewed_at);
CREATE INDEX idx_video_views_video_date ON video_views(video_id, viewed_at);

CREATE INDEX idx_video_ratings_video_id ON video_ratings(video_id);
CREATE INDEX idx_video_ratings_user_id ON video_ratings(user_id);
CREATE INDEX idx_video_ratings_rating ON video_ratings(rating);

CREATE INDEX idx_video_comments_video_id ON video_comments(video_id);
CREATE INDEX idx_video_comments_user_id ON video_comments(user_id);
CREATE INDEX idx_video_comments_parent_id ON video_comments(parent_id);
CREATE INDEX idx_video_comments_created_at ON video_comments(created_at); 