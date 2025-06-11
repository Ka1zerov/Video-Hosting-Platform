-- Initialize Upload Service Database

-- Create extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_stat_statements";

-- Create schema for upload service
CREATE SCHEMA IF NOT EXISTS upload;

-- Set default search path
ALTER DATABASE video_platform SET search_path TO upload, public;

-- Grant permissions
GRANT ALL PRIVILEGES ON DATABASE video_platform TO upload_user;
GRANT ALL PRIVILEGES ON SCHEMA upload TO upload_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA upload TO upload_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA upload TO upload_user;

-- Performance tuning
ALTER SYSTEM SET shared_buffers = '256MB';
ALTER SYSTEM SET effective_cache_size = '1GB';
ALTER SYSTEM SET maintenance_work_mem = '64MB';
ALTER SYSTEM SET checkpoint_completion_target = 0.9;
ALTER SYSTEM SET wal_buffers = '16MB';
ALTER SYSTEM SET default_statistics_target = 100;
ALTER SYSTEM SET random_page_cost = 1.1;

-- Logging for monitoring
ALTER SYSTEM SET log_destination = 'stderr';
ALTER SYSTEM SET log_statement = 'mod';
ALTER SYSTEM SET log_min_duration_statement = 1000;
ALTER SYSTEM SET log_checkpoints = on;
ALTER SYSTEM SET log_connections = on;
ALTER SYSTEM SET log_disconnections = on;

-- Apply settings
SELECT pg_reload_conf(); 