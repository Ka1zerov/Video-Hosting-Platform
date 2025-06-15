-- PostgreSQL Master Replication Setup
-- This script configures the master database for streaming replication

-- Create replication user
CREATE USER replicator WITH REPLICATION ENCRYPTED PASSWORD 'replicator_pass';

-- Grant necessary privileges
GRANT CONNECT ON DATABASE video_platform TO replicator;

-- Create replication slot for slave
SELECT pg_create_physical_replication_slot('replica_slot');

-- Enable logging for replication monitoring
ALTER SYSTEM SET log_replication_commands = on;
ALTER SYSTEM SET log_min_messages = info;

-- Configure pg_hba.conf for replication
\! echo "# Replication connections" >> /var/lib/postgresql/data/pg_hba.conf
\! echo "host replication replicator 0.0.0.0/0 md5" >> /var/lib/postgresql/data/pg_hba.conf
\! echo "host replication replicator ::0/0 md5" >> /var/lib/postgresql/data/pg_hba.conf

-- Reload configuration
SELECT pg_reload_conf();

-- Create monitoring view for replication status
CREATE OR REPLACE VIEW replication_status AS
SELECT 
    client_addr,
    usename,
    application_name,
    state,
    sent_lsn,
    write_lsn,
    flush_lsn,
    replay_lsn,
    write_lag,
    flush_lag,
    replay_lag,
    sync_state
FROM pg_stat_replication; 