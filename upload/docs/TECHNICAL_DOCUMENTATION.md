# Technical Documentation - Upload Service

## Technology Stack Overview

### Core Technologies

#### **PostgreSQL 15**
- **Purpose**: Primary data storage for video metadata
- **Key Features**: ACID compliance, UUID support, full-text search
- **Extensions Used**: `uuid-ossp`, `pg_stat_statements`
- **Performance Tuning**: Optimized for mixed read/write workloads

#### **Redis 7 Alpine**
- **Purpose**: Caching layer and session storage
- **Memory Policy**: `allkeys-lru` with 256MB limit
- **Persistence**: RDB snapshots + AOF for durability
- **Use Cases**: Video metadata cache, user sessions, rate limiting

#### **HAProxy Latest**
- **Purpose**: Load balancer for database connections
- **Algorithm**: Round-robin with health checks
- **Monitoring**: Built-in statistics dashboard
- **SSL/TLS**: Ready for termination (production)

#### **Spring Boot 3.4.5 with Java 21**
- **Framework**: Reactive-ready with WebFlux support
- **Data Access**: Spring Data JPA + Liquibase migrations
- **Validation**: Bean Validation (JSR-303)
- **Monitoring**: Actuator endpoints

## Database Strategies

### **1. Connection Pooling Strategy**

#### Development Configuration
```yaml
hikari:
  maximum-pool-size: 10    # Small pool for single instance
  minimum-idle: 2          # Minimal idle connections
  connection-timeout: 30s  # Quick timeout for dev
```

#### Production Configuration  
```yaml
# Write Pool (Master)
hikari:
  maximum-pool-size: 10    # Conservative for writes
  minimum-idle: 5          # Always ready connections
  idle-timeout: 5min       # Keep connections alive
  connection-timeout: 20s  # Fail fast on issues

# Read Pool (Master + Slaves)  
hikari:
  maximum-pool-size: 15    # More connections for reads
  minimum-idle: 5          # Baseline readiness
  validation-timeout: 5s   # Quick health checks
```

**Why this works:**
- Write operations are typically fewer but critical
- Read operations can scale horizontally with more connections
- Separate pools prevent read queries from blocking writes

### **2. Database Replication Strategy**

#### **Streaming Replication (Hot Standby)**

```
┌─────────────┐    WAL Stream    ┌─────────────┐
│   Master    │ ===============> │    Slave    │
│  (Write)    │   < 100ms lag    │   (Read)    │
└─────────────┘                  └─────────────┘
       │                                │
       ▼                                ▼
┌─────────────┐                  ┌─────────────┐
│  App Writes │                  │  App Reads  │
└─────────────┘                  └─────────────┘
```

**Implementation Details:**
- **WAL Level**: `replica` - minimum for physical replication
- **Max Senders**: 3 - supports multiple slaves if needed
- **WAL Keep Size**: 1GB - prevents premature WAL deletion
- **Hot Standby**: Enabled - allows read queries on slave

**Advantages:**
- Near real-time replication (< 100ms lag)
- Automatic failover capability with promotion
- Read scaling without application changes
- Consistent backup source (slave)

**Monitoring Queries:**
```sql
-- Check replication lag (run on master)
SELECT 
    client_addr,
    state,
    pg_wal_lsn_diff(pg_current_wal_lsn(), flush_lsn) AS lag_bytes,
    EXTRACT(SECONDS FROM now() - backend_start) AS connection_age
FROM pg_stat_replication;

-- Check replay lag (run on slave)
SELECT 
    pg_last_wal_receive_lsn() AS receive_lsn,
    pg_last_wal_replay_lsn() AS replay_lsn,
    EXTRACT(SECONDS FROM now() - pg_last_xact_replay_timestamp()) AS lag_seconds;
```

### **3. Load Balancing Strategy**

#### **HAProxy Configuration Logic**

```
Client Request
    │
    ▼
┌─────────────┐
│   HAProxy   │
│   Router    │
└─────────────┘
    │     │
    ▼     ▼
 Write   Read
  Port   Port
 5432   5435
    │     │
    ▼     ▼
┌─────┐ ┌─────┐
│Master│ │M+S  │ 
│ 100%│ │30/70│
└─────┘ └─────┘
```

**Routing Rules:**
- **Port 5432**: All traffic → Master (writes + critical reads)
- **Port 5435**: Traffic → Master (30%) + Slave (70%) (non-critical reads)

**Health Check Strategy:**
```haproxy
option pgsql-check user upload_user
# Checks: SELECT 1; every 2 seconds
# Removes unhealthy backends automatically
# Restores when health check passes
```

**Weight Distribution Logic:**
```
Master Weight: 30  (handles writes + some reads)
Slave Weight:  70  (dedicated to read workload)

Result: 70% of read queries go to slave
```

### **4. Caching Strategy**

#### **Redis Configuration**

```yaml
# Memory Management
maxmemory: 256mb              # Limit memory usage
maxmemory-policy: allkeys-lru # Evict least recently used

# Persistence Strategy
save 900 1     # Save if 1 key changed in 15 min
save 300 10    # Save if 10 keys changed in 5 min  
save 60 10000  # Save if 10k keys changed in 1 min
appendonly: yes # Enable AOF for durability
```

**Cache Patterns:**
```java
// Video metadata caching
@Cacheable(value = "videos", key = "#videoId")
public Video getVideoById(UUID videoId) { ... }

// User video list caching
@Cacheable(value = "user-videos", key = "#userId")  
public List<Video> getUserVideos(String userId) { ... }

// Cache eviction on updates
@CacheEvict(value = "videos", key = "#video.id")
public Video updateVideo(Video video) { ... }
```

**TTL Strategy:**
- Video metadata: 1 hour (frequently accessed)
- User lists: 30 minutes (moderate updates)
- Search results: 10 minutes (dynamic content)

## Security Strategies

### **1. Database Security**

#### **Authentication Configuration**
```sql
-- Connection restrictions (pg_hba.conf)
host video_platform upload_user 172.0.0.0/8 md5  # App connections
host replication replica_user   172.0.0.0/8 md5   # Replication only
local all all trust                                # Local admin access
```

#### **User Privileges**
```sql
-- Application user (minimal privileges)
GRANT CONNECT ON DATABASE video_platform TO upload_user;
GRANT USAGE ON SCHEMA upload TO upload_user;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA upload TO upload_user;

-- Replication user (replication only)
GRANT REPLICATION ON DATABASE video_platform TO replica_user;
```

### **2. Redis Security**

#### **Production Security**
```redis
# Enable password authentication
requirepass ${REDIS_PASSWORD}

# Disable dangerous commands
rename-command FLUSHDB ""
rename-command FLUSHALL ""
rename-command DEBUG ""
rename-command CONFIG "CONFIG_9a8b7c6d"
```

#### **Network Security**
```yaml
# Bind to specific interfaces
bind 127.0.0.1 ::1  # Localhost only in production

# Use TLS for connections
tls-port 6380
tls-cert-file /path/to/redis.crt  
tls-key-file /path/to/redis.key
```

## Performance Optimization

### **1. Database Performance**

#### **Index Strategy**
```sql
-- Core indexes (automatically created)
CREATE INDEX idx_videos_user_id ON videos(user_id);
CREATE INDEX idx_videos_status ON videos(status);
CREATE INDEX idx_videos_uploaded_at ON videos(uploaded_at);

-- Composite indexes for common queries
CREATE INDEX idx_videos_user_status ON videos(user_id, status);
CREATE INDEX idx_videos_user_uploaded_desc ON videos(user_id, uploaded_at DESC);

-- Partial indexes for soft delete
CREATE INDEX idx_videos_active ON videos(user_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_videos_deleted ON videos(deleted_at) WHERE deleted_at IS NOT NULL;
```

#### **Query Optimization**
```sql
-- Efficient pagination
SELECT * FROM videos 
WHERE user_id = ? AND deleted_at IS NULL 
ORDER BY uploaded_at DESC 
LIMIT 20 OFFSET ?;

-- Count optimization (avoid COUNT(*))
SELECT 
  (SELECT COUNT(*) FROM videos WHERE user_id = ? AND deleted_at IS NULL) as total,
  (SELECT SUM(file_size) FROM videos WHERE user_id = ? AND deleted_at IS NULL) as total_size;
```

### **2. Application Performance**

#### **JPA Optimizations**
```yaml
# Hibernate settings
hibernate:
  jdbc:
    batch_size: 25                    # Batch inserts/updates
    fetch_size: 50                    # JDBC fetch size
  cache:
    use_second_level_cache: true      # Enable L2 cache
    region:
      factory_class: org.hibernate.cache.jcache.JCacheRegionFactory
```

#### **Connection Pool Tuning**
```java
// Optimal pool sizing formula:
// pool_size = Tn × (Cm - 1) + 1
// Where:
// Tn = Number of threads
// Cm = Number of concurrent connections per thread

// For 8 thread application:
// Write pool: 8 × (1.25 - 1) + 1 = 3 (conservative)
// Read pool:  8 × (2.0 - 1) + 1 = 9 (more reads)
```

## Monitoring and Observability

### **1. Application Metrics**

#### **Spring Boot Actuator Endpoints**
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

**Key Metrics to Monitor:**
```java
// Database metrics
hikaricp.connections.active
hikaricp.connections.idle  
hikaricp.connections.pending

// Application metrics
http.server.requests (response times, status codes)
jvm.memory.used
jvm.gc.pause

// Custom business metrics
@Counter("videos.uploaded.total")
@Timer("video.processing.time")
@Gauge("videos.storage.size.bytes")
```

### **2. Database Monitoring**

#### **PostgreSQL Metrics**
```sql
-- Active connections
SELECT count(*) FROM pg_stat_activity WHERE state = 'active';

-- Slow queries
SELECT query, mean_time, calls 
FROM pg_stat_statements 
ORDER BY mean_time DESC LIMIT 10;

-- Table sizes
SELECT 
  schemaname,
  tablename,
  pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) as size
FROM pg_tables 
WHERE schemaname = 'upload'
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;

-- Replication lag
SELECT 
  slot_name,
  pg_size_pretty(pg_wal_lsn_diff(pg_current_wal_lsn(), restart_lsn)) as lag_size
FROM pg_replication_slots;
```

#### **Redis Monitoring**
```bash
# Memory usage
redis-cli INFO memory | grep used_memory_human

# Hit rate
redis-cli INFO stats | grep keyspace_hits
redis-cli INFO stats | grep keyspace_misses

# Slow queries
redis-cli SLOWLOG GET 10
```

## Disaster Recovery

### **1. Backup Strategy**

#### **PostgreSQL Backups**
```bash
# Automated daily backups
#!/bin/bash
BACKUP_DIR="/backups/postgres"
DATE=$(date +%Y%m%d_%H%M%S)

# Full backup from slave (no load on master)
pg_dump -h postgres-slave -U upload_user video_platform > 
  ${BACKUP_DIR}/full_backup_${DATE}.sql

# WAL archiving (continuous backup)
archive_command = 'rsync %p backup-server:/wal-archive/%f'
```

#### **Redis Backups**
```bash
# RDB snapshots (automatic)
save 900 1
save 300 10  
save 60 10000

# Manual snapshot
redis-cli BGSAVE

# AOF backup
cp /data/appendonly.aof /backups/redis/aof_backup_$(date +%Y%m%d).aof
```

### **2. Failover Procedures**

#### **PostgreSQL Master Failover**
```bash
# 1. Promote slave to master
docker exec upload-postgres-slave su - postgres -c \
  "pg_ctl promote -D /var/lib/postgresql/data"

# 2. Update HAProxy config
# Point write traffic to new master

# 3. Rebuild old master as new slave
docker stop upload-postgres-master
docker volume rm upload_postgres_master_data
# Update configs and restart as slave
```

#### **Redis Failover**
```bash
# Redis Sentinel (production setup)
sentinel monitor mymaster redis-master 6379 2
sentinel down-after-milliseconds mymaster 5000
sentinel failover-timeout mymaster 10000
```

## Development Workflow

### **1. Local Development Setup**
```bash
# 1. Start infrastructure
cd upload
./scripts/docker-dev.sh start

# 2. Run application
./gradlew bootRun

# 3. Access services
echo "App: http://localhost:8082"
echo "DB:  localhost:5433" 
echo "Redis: localhost:6380"
```

### **2. Testing with Replication**
```bash
# 1. Start replica environment
docker-compose -f docker-compose-replica.yml up -d

# 2. Test write operations
curl -X POST http://localhost:8082/api/upload/video \
  -F "file=@test.mp4" \
  -F "title=Test Video"

# 3. Verify replication
./scripts/docker-dev.sh shell-db
# Run: SELECT * FROM pg_stat_replication;

# 4. Test read scaling
# Configure app to use port 5436 for reads
```

### **3. Production Deployment**
```bash
# 1. Build application
./gradlew build

# 2. Start infrastructure
docker-compose -f docker-compose-replica.yml up -d

# 3. Run with production profile
java -jar build/libs/upload-service.jar \
  --spring.profiles.active=replica \
  --server.port=8082
```

## Troubleshooting Guide

### **Common Issues and Solutions**

#### **Replication Lag**
```bash
# Check lag
SELECT pg_wal_lsn_diff(pg_current_wal_lsn(), flush_lsn) FROM pg_stat_replication;

# Solutions:
1. Increase wal_buffers
2. Check network latency  
3. Monitor disk I/O on slave
4. Consider synchronous replication for critical data
```

#### **Connection Pool Exhaustion**
```bash
# Symptoms: "Connection is not available" errors
# Solutions:
1. Increase maximum-pool-size
2. Reduce connection-timeout
3. Check for connection leaks
4. Monitor active vs idle connections
```

#### **Redis Memory Issues**
```bash
# Check memory usage
redis-cli INFO memory

# Solutions:
1. Increase maxmemory limit
2. Optimize cache TTL values
3. Review cache hit rates
4. Consider cache eviction policies
```

This documentation covers the technical foundations and operational aspects of the Upload Service infrastructure. 