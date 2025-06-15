#!/bin/bash
# PostgreSQL Slave Replication Setup
# This script configures the slave database for streaming replication

set -e

echo "Starting PostgreSQL slave replication setup..."

# Wait for master to be ready
echo "Waiting for master database to be ready..."
until pg_isready -h ${POSTGRES_MASTER_HOST} -p ${POSTGRES_MASTER_PORT} -U ${POSTGRES_USER}; do
    echo "Master database is not ready yet. Waiting..."
    sleep 2
done

echo "Master database is ready. Starting slave setup..."

# Stop PostgreSQL if running
pg_ctl stop -D /var/lib/postgresql/data -m fast || true

# Remove existing data directory
rm -rf /var/lib/postgresql/data/*

# Create base backup from master
echo "Creating base backup from master..."
PGPASSWORD=${POSTGRES_REPLICATION_PASSWORD} pg_basebackup \
    -h ${POSTGRES_MASTER_HOST} \
    -p ${POSTGRES_MASTER_PORT} \
    -U ${POSTGRES_REPLICATION_USER} \
    -D /var/lib/postgresql/data \
    -Fp -Xs -v -P -R

# Create standby.signal file to indicate this is a standby server
touch /var/lib/postgresql/data/standby.signal

# Configure recovery settings in postgresql.conf
cat >> /var/lib/postgresql/data/postgresql.conf << EOF

# Standby server configuration
primary_conninfo = 'host=${POSTGRES_MASTER_HOST} port=${POSTGRES_MASTER_PORT} user=${POSTGRES_REPLICATION_USER} password=${POSTGRES_REPLICATION_PASSWORD} application_name=slave1'
primary_slot_name = 'replica_slot'
promote_trigger_file = '/tmp/postgresql.trigger'

# Hot standby configuration
hot_standby = on
max_standby_streaming_delay = 30s
max_standby_archive_delay = 30s

# Logging configuration
log_destination = 'stderr'
log_statement = 'none'
log_replication_commands = on
EOF

# Set correct permissions
chown -R postgres:postgres /var/lib/postgresql/data
chmod 700 /var/lib/postgresql/data

echo "Slave setup completed successfully!"

# Start PostgreSQL
exec postgres 