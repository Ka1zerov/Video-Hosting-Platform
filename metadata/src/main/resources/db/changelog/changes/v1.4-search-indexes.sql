--liquibase formatted sql

--changeset TymofiiSkrypko:metadata-create-search-indexes-and-triggers context:metadata-service
-- Full-text search configuration for video metadata
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_videos_title_description_search 
ON videos USING gin(to_tsvector('english', coalesce(title, '') || ' ' || coalesce(description, '')));

-- Update search_vector in video_metadata when data changes
CREATE OR REPLACE FUNCTION update_video_metadata_search_vector()
RETURNS TRIGGER AS $$
BEGIN
    -- Get video title and description from videos table
    SELECT 
        to_tsvector('english', coalesce(v.title, '') || ' ' || coalesce(v.description, '') || ' ' || coalesce(NEW.location, ''))
    INTO NEW.search_vector
    FROM videos v
    WHERE v.id = NEW.video_id;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger to automatically update search vector
CREATE TRIGGER trigger_update_video_metadata_search_vector
    BEFORE INSERT OR UPDATE ON video_metadata
    FOR EACH ROW
    EXECUTE FUNCTION update_video_metadata_search_vector();

--changeset TymofiiSkrypko:metadata-create-materialized-view-for-search context:metadata-service
-- Materialized view for fast search combining multiple tables
CREATE MATERIALIZED VIEW video_search_view AS
SELECT 
    v.id,
    v.title,
    v.description,
    v.user_id,
    v.status,
    v.uploaded_at,
    vm.duration_seconds,
    vm.thumbnail_url,
    vm.privacy_level,
    vm.language,
    va.view_count,
    va.average_rating,
    va.like_count,
    ARRAY_AGG(DISTINCT vt.tag_name) FILTER (WHERE vt.tag_name IS NOT NULL) AS tags,
    ARRAY_AGG(DISTINCT c.name) FILTER (WHERE c.name IS NOT NULL) AS categories,
    to_tsvector('english', 
        coalesce(v.title, '') || ' ' || 
        coalesce(v.description, '') || ' ' ||
        coalesce(string_agg(DISTINCT vt.tag_name, ' '), '') || ' ' ||
        coalesce(string_agg(DISTINCT c.name, ' '), '')
    ) AS search_vector
FROM videos v
LEFT JOIN video_metadata vm ON v.id = vm.video_id
LEFT JOIN video_analytics va ON v.id = va.video_id
LEFT JOIN video_tags vt ON v.id = vt.video_id AND vt.deleted_at IS NULL
LEFT JOIN video_categories vc ON v.id = vc.video_id AND vc.deleted_at IS NULL
LEFT JOIN categories c ON vc.category_id = c.id AND c.deleted_at IS NULL
WHERE v.deleted_at IS NULL
GROUP BY v.id, v.title, v.description, v.user_id, v.status, v.uploaded_at,
         vm.duration_seconds, vm.thumbnail_url, vm.privacy_level, vm.language,
         va.view_count, va.average_rating, va.like_count;

-- Index on the materialized view for fast search
CREATE INDEX idx_video_search_view_search_vector ON video_search_view USING gin(search_vector);
CREATE INDEX idx_video_search_view_user_id ON video_search_view(user_id);
CREATE INDEX idx_video_search_view_status ON video_search_view(status);
CREATE INDEX idx_video_search_view_privacy ON video_search_view(privacy_level);
CREATE INDEX idx_video_search_view_uploaded_at ON video_search_view(uploaded_at);
CREATE INDEX idx_video_search_view_view_count ON video_search_view(view_count DESC);
CREATE INDEX idx_video_search_view_rating ON video_search_view(average_rating DESC);

--changeset TymofiiSkrypko:metadata-create-search-functions context:metadata-service
-- Function to refresh search materialized view
CREATE OR REPLACE FUNCTION refresh_video_search_view()
RETURNS VOID AS $$
BEGIN
    REFRESH MATERIALIZED VIEW CONCURRENTLY video_search_view;
END;
$$ LANGUAGE plpgsql;

-- Function for weighted search ranking
CREATE OR REPLACE FUNCTION search_videos(
    search_query TEXT,
    user_id_filter VARCHAR(255) DEFAULT NULL,
    category_filter VARCHAR(100) DEFAULT NULL,
    limit_count INTEGER DEFAULT 20,
    offset_count INTEGER DEFAULT 0
)
RETURNS TABLE (
    video_id UUID,
    title VARCHAR(255),
    description TEXT,
    user_id VARCHAR(255),
    thumbnail_url VARCHAR(512),
    duration_seconds INTEGER,
    view_count BIGINT,
    average_rating DECIMAL(3,2),
    tags TEXT[],
    categories TEXT[],
    rank REAL
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        vsv.id,
        vsv.title,
        vsv.description,
        vsv.user_id,
        vsv.thumbnail_url,
        vsv.duration_seconds,
        vsv.view_count,
        vsv.average_rating,
        vsv.tags,
        vsv.categories,
        ts_rank(vsv.search_vector, plainto_tsquery('english', search_query)) AS rank
    FROM video_search_view vsv
    WHERE 
        vsv.search_vector @@ plainto_tsquery('english', search_query)
        AND vsv.privacy_level = 'PUBLIC'
        AND vsv.status = 'UPLOADED'
        AND (user_id_filter IS NULL OR vsv.user_id = user_id_filter)
        AND (category_filter IS NULL OR category_filter = ANY(vsv.categories))
    ORDER BY 
        ts_rank(vsv.search_vector, plainto_tsquery('english', search_query)) DESC,
        vsv.view_count DESC,
        vsv.uploaded_at DESC
    LIMIT limit_count OFFSET offset_count;
END;
$$ LANGUAGE plpgsql; 