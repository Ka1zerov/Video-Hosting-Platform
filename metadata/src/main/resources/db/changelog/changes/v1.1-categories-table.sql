--liquibase formatted sql

--changeset TymofiiSkrypko:metadata-create-categories-table context:metadata-service
CREATE TABLE categories
(
    id          UUID PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    parent_id   UUID,
    is_active   BOOLEAN NOT NULL DEFAULT true,
    sort_order  INTEGER DEFAULT 0,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    modified_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by  VARCHAR(255) NOT NULL,
    modified_by VARCHAR(255) NOT NULL,
    deleted_at  TIMESTAMP WITH TIME ZONE,
    
    -- Self-referencing foreign key for hierarchical categories
    CONSTRAINT fk_categories_parent_id FOREIGN KEY (parent_id) REFERENCES categories(id)
);

--changeset TymofiiSkrypko:metadata-create-video-categories-table context:metadata-service
CREATE TABLE video_categories
(
    id          UUID PRIMARY KEY,
    video_id    UUID NOT NULL,
    category_id UUID NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    modified_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by  VARCHAR(255) NOT NULL,
    modified_by VARCHAR(255) NOT NULL,
    deleted_at  TIMESTAMP WITH TIME ZONE,
    
    -- Foreign key references
    CONSTRAINT fk_video_categories_video_id FOREIGN KEY (video_id) REFERENCES videos(id) ON DELETE CASCADE,
    CONSTRAINT fk_video_categories_category_id FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE CASCADE,
    
    -- Unique constraint to prevent duplicate category assignments
    CONSTRAINT uk_video_categories_video_category UNIQUE (video_id, category_id)
);

--changeset TymofiiSkrypko:metadata-create-categories-indexes context:metadata-service
CREATE INDEX idx_categories_name ON categories(name);
CREATE INDEX idx_categories_parent_id ON categories(parent_id);
CREATE INDEX idx_categories_active ON categories(is_active);
CREATE INDEX idx_categories_sort_order ON categories(sort_order);

CREATE INDEX idx_video_categories_video_id ON video_categories(video_id);
CREATE INDEX idx_video_categories_category_id ON video_categories(category_id);

--changeset TymofiiSkrypko:metadata-insert-default-categories context:metadata-service
INSERT INTO categories (id, name, description, parent_id, is_active, sort_order, created_by, modified_by) VALUES
    (gen_random_uuid(), 'Entertainment', 'Entertainment videos', null, true, 1, 'system', 'system'),
    (gen_random_uuid(), 'Education', 'Educational content', null, true, 2, 'system', 'system'),
    (gen_random_uuid(), 'Technology', 'Technology and programming', null, true, 3, 'system', 'system'),
    (gen_random_uuid(), 'Music', 'Music and audio content', null, true, 4, 'system', 'system'),
    (gen_random_uuid(), 'Sports', 'Sports and fitness content', null, true, 5, 'system', 'system'),
    (gen_random_uuid(), 'Gaming', 'Gaming content and reviews', null, true, 6, 'system', 'system'),
    (gen_random_uuid(), 'News', 'News and current events', null, true, 7, 'system', 'system'),
    (gen_random_uuid(), 'Travel', 'Travel and tourism', null, true, 8, 'system', 'system'); 