# PostgreSQL Master-Slave Replication Guide

This document describes the PostgreSQL master-slave replication setup for the Video Hosting Platform.

## Overview

The infrastructure uses **PostgreSQL streaming replication** with **HAProxy load balancing** to provide:
- **High Availability**: Automatic failover capabilities
- **Read Scaling**: Distribute read queries across multiple servers
- **Performance**: Improved query performance through load distribution

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Applications                             │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐        │
│  │   Upload    │  │  Metadata   │  │  Streaming  │  ...   │
│  │   Service   │  │   Service   │  │   Service   │        │
│  └─────────────┘  └─────────────┘  └─────────────┘        │
│         │                │                │                │
│    Write │           Read │           Read │                │
│         ▼                ▼                ▼                │
├─────────────────────────────────────────────────────────────┤
│              HAProxy Load Balancer                          │
│  ┌─────────────────────┐    ┌─────────────────────────────┐ │
│  │   Write Port 5435   │    │    Read Port 5436           │ │
│  │   (Master Only)     │    │    (Master + Slave)         │ │
│  └─────────────────────┘    └─────────────────────────────┘ │
│         │                              │                   │
├─────────┼──────────────────────────────┼───────────────────┤
│         ▼                              ▼                   │
│  ┌─────────────────────┐    ┌─────────────────────────────┐ │
│  │  Master Database    │───▶│   Slave Database            │ │
│  │  Port: 5433         │    │   Port: 5434                │ │
│  │  (Read/Write)       │    │   (Read Only)               │ │
│  └─────────────────────┘    └─────────────────────────────┘ │
│          │                              ▲                   │
│          └──── Streaming Replication ───┘                   │
└─────────────────────────────────────────────────────────────┘
```

## Port Configuration

| Service | Port | Purpose | Used By |
|---------|------|---------|---------|
| **postgres-video-master** | 5433 | Master database (direct access) | Infrastructure only |
| **postgres-video-slave** | 5434 | Slave database (direct access) | Infrastructure only |
| **HAProxy Write** | **5435** | **Write operations to master** | **Applications** |
| **HAProxy Read** | **5436** | **Read operations (load balanced)** | **Applications** |
| HAProxy Stats | 8404 | Monitoring and statistics | Administrators |

## Application Configuration

### Development Mode
For development, use regular `application.yml` with direct connection to master:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5433/video_platform
```

### Production Mode (Recommended)
For production, use `application-replica.yml` with HAProxy:

```yaml
spring:
  # Write operations - HAProxy routes to Master
  datasource:
    url: jdbc:postgresql://localhost:5435/video_platform
    username: ${DB_USERNAME:upload_user}
    password: ${DB_PASSWORD:upload_pass}
    driver-class-name: org.postgresql.Driver
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5

  # Read-only operations - HAProxy routes to Master + Slave
  datasource-readonly:
    url: jdbc:postgresql://localhost:5436/video_platform
    username: ${DB_USERNAME:upload_user}
    password: ${DB_PASSWORD:upload_pass}
    driver-class-name: org.postgresql.Driver
    hikari:
      maximum-pool-size: 15
      minimum-idle: 5
```

## Starting Services

### 1. Start Infrastructure
```bash
cd infrastructure
./scripts/platform.sh start video
```

### 2. Wait for Replication Setup
The slave needs time to sync with the master. Monitor the setup:
```bash
./scripts/platform.sh replication-status
```

### 3. Start Applications
```bash
# Development mode
cd upload && ./gradlew bootRun

# Production mode with replication
cd upload && ./gradlew bootRun --spring.profiles.active=replica
```

## Monitoring

### 1. HAProxy Statistics
Access the HAProxy stats page to monitor database connections:
```bash
./scripts/platform.sh haproxy-stats
# Opens: http://localhost:8404/stats
```

Key metrics to monitor:
- **Backend Status**: GREEN means healthy, RED means down
- **Session Count**: Number of active connections
- **Response Time**: Average response time per backend

### 2. Replication Status
Check PostgreSQL replication health:
```bash
./scripts/platform.sh replication-status
```

This shows:
- **Replication Lag**: How far behind the slave is
- **Connection Status**: Whether replication is active
- **LSN Positions**: Log sequence numbers for sync tracking

### 3. Database Logs
Monitor database logs for issues:
```bash
# Master logs
./scripts/platform.sh logs postgres-video-master

# Slave logs  
./scripts/platform.sh logs postgres-video-slave

# HAProxy logs
./scripts/platform.sh logs haproxy-postgres
```

## Troubleshooting

### Replication Not Working

1. **Check if both databases are running**:
   ```bash
   ./scripts/platform.sh status
   ```

2. **Verify replication user exists**:
   ```bash
   ./scripts/platform.sh shell-video
   # In PostgreSQL shell:
   SELECT * FROM pg_user WHERE usename = 'replicator';
   ```

3. **Check replication slots**:
   ```bash
   ./scripts/platform.sh shell-video
   # In PostgreSQL shell:
   SELECT * FROM pg_replication_slots;
   ```

### High Replication Lag

1. **Check system resources**:
   ```bash
   docker stats
   ```

2. **Monitor network connectivity**:
   ```bash
   docker compose exec postgres-video-slave ping postgres-video-master
   ```

3. **Check WAL sender processes**:
   ```bash
   ./scripts/platform.sh shell-video
   # In PostgreSQL shell:
   SELECT * FROM pg_stat_replication;
   ```

### HAProxy Issues

1. **Check HAProxy configuration**:
   ```bash
   docker compose exec haproxy-postgres haproxy -c -f /usr/local/etc/haproxy/haproxy.cfg
   ```

2. **Verify backend health**:
   ```bash
   curl -s http://localhost:8404/stats | grep postgres
   ```

3. **Test connections**:
   ```bash
   # Test write connection (should go to master)
   psql -h localhost -p 5435 -U upload_user -d video_platform -c "SELECT 'WRITE TEST';"
   
   # Test read connection (should balance between master and slave)
   psql -h localhost -p 5436 -U upload_user -d video_platform -c "SELECT 'READ TEST';"
   ```

## Emergency Procedures

### Slave Failure
If the slave fails, the system continues to work using only the master:
1. HAProxy automatically detects the failure
2. All read traffic is routed to the master
3. Performance may be impacted but service continues

### Master Failure
If the master fails, promote the slave to master:

```bash
# EMERGENCY ONLY - promotes slave to master
./scripts/platform.sh promote-slave
```

**Important**: After promoting slave to master:
1. Update application configurations to point to the new master
2. Set up a new slave for replication
3. Consider data loss from any uncommitted transactions

### Split-Brain Prevention
The current setup uses asynchronous replication. In production, consider:
- **Synchronous replication** for critical data
- **Proper monitoring** and alerting
- **Automated failover** solutions like Patroni or repmgr

## Performance Tuning

### Master Database
Optimize for write performance:
```sql
-- Increase WAL buffers
ALTER SYSTEM SET wal_buffers = '16MB';

-- Tune checkpoint behavior
ALTER SYSTEM SET checkpoint_completion_target = 0.9;
ALTER SYSTEM SET max_wal_size = '1GB';

-- Reload configuration
SELECT pg_reload_conf();
```

### Slave Database
Optimize for read performance:
```sql
-- Increase shared buffers for read queries
ALTER SYSTEM SET shared_buffers = '256MB';

-- Tune for read-heavy workloads
ALTER SYSTEM SET effective_cache_size = '1GB';

-- Reload configuration
SELECT pg_reload_conf();
```

### HAProxy Tuning
Edit `infrastructure/config/haproxy/haproxy.cfg`:
```
# Increase connection limits
maxconn 2000

# Tune health check intervals
check inter 2000ms fall 2 rise 3

# Optimize for database connections
timeout connect 10s
timeout client 60s
timeout server 60s
```

## Security Considerations

### Replication User
The replication user has minimal privileges:
- Only `REPLICATION` privilege
- Cannot access application data
- Cannot modify database schema

### Network Security
- All services communicate within Docker network
- No external access to raw database ports
- HAProxy provides controlled access point

### Authentication
- Use strong passwords for database users
- Consider certificate-based authentication for production
- Rotate credentials regularly

## Backup Strategy

### Regular Backups
```bash
# Backup from master (recommended)
./scripts/platform.sh backup-video
```

### Point-in-Time Recovery
For production, consider:
- **WAL archiving** to external storage
- **Continuous archiving** for point-in-time recovery
- **Regular testing** of backup restoration

### Disaster Recovery
- Keep backups in multiple locations
- Test restore procedures regularly
- Document recovery procedures
- Consider cross-region replication

## Maintenance

### Planned Maintenance
For maintenance requiring downtime:

1. **Stop applications** (to prevent new connections)
2. **Wait for replication sync** (`./scripts/platform.sh replication-status`)
3. **Perform maintenance** on master
4. **Restart applications**

### Upgrading PostgreSQL
1. Stop all services
2. Backup all data
3. Update Docker images in `docker-compose.yml`
4. Restart services
5. Test replication setup

### Scaling
To add more read replicas:
1. Add new slave service to `docker-compose.yml`
2. Update HAProxy configuration
3. Restart HAProxy service

## Best Practices

### Application Development
- **Use write datasource** for CREATE, UPDATE, DELETE operations
- **Use read datasource** for SELECT operations
- **Handle connection failures** gracefully
- **Avoid long-running transactions** that could impact replication

### Monitoring
- Set up alerts for replication lag > 5 seconds
- Monitor connection counts and query performance
- Track backup success/failure
- Monitor disk space on both master and slave

### Testing
- Regularly test failover procedures
- Verify backup/restore processes
- Test application behavior during database maintenance
- Load test with realistic traffic patterns 