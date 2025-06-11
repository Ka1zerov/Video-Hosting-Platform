# Quick Start Guide - Upload Service

## 🚀 **5-Minute Setup**

### **Development Environment**
```bash
# 1. Clone and navigate
cd upload

# 2. Start infrastructure (auto-downloads and configures everything)
./scripts/docker-dev.sh start

# 3. Verify services are running
./scripts/docker-dev.sh status
# Expected: postgres and redis containers running

# 4. Run application
./gradlew bootRun

# 5. Test endpoint
curl http://localhost:8082/api/upload/health
# Expected: "Upload Service is running"
```

### **With Admin Tools**
```bash
# Start with monitoring dashboards
./scripts/docker-dev.sh start-admin

# Access dashboards:
echo "pgAdmin: http://localhost:5051 (admin/admin)"
echo "Redis Commander: http://localhost:8085"
echo "Application: http://localhost:8082"
```

## 🎯 **What Happens Automatically**

### **Infrastructure Setup**
- ✅ PostgreSQL 15 starts on port 5433
- ✅ Liquibase runs database migrations
- ✅ Redis 7 starts on port 6380 with persistence
- ✅ Docker networks and volumes created
- ✅ Health checks ensure services are ready

### **Database Initialization**
```sql
-- Automatically created:
- Extension: uuid-ossp (UUID generation)
- Extension: pg_stat_statements (query monitoring)
- Schema: upload (application tables)
- User: upload_user (application access)
- Indexes: Optimized for common queries
```

### **Application Features**
- ✅ Soft delete implementation
- ✅ UUID-based entities with audit fields
- ✅ Exception handling with structured responses
- ✅ File validation (type, size limits)
- ✅ User isolation (X-User-Id header)

## 📋 **Essential Commands**

### **Daily Development**
```bash
# Start/stop services
./scripts/docker-dev.sh start
./scripts/docker-dev.sh stop

# View logs
./scripts/docker-dev.sh logs          # All services
./scripts/docker-dev.sh logs postgres # Specific service

# Database access
./scripts/docker-dev.sh shell-db      # PostgreSQL shell
./scripts/docker-dev.sh shell-redis   # Redis CLI

# Service status
./scripts/docker-dev.sh status
```

### **Database Operations**
```bash
# Backup database
./scripts/docker-dev.sh backup-db
# Creates: backup_YYYYMMDD_HHMMSS.sql

# Restore database
./scripts/docker-dev.sh restore-db backup_20231201_120000.sql

# Reset everything (DELETE ALL DATA!)
./scripts/docker-dev.sh clean
```

## 🔧 **Testing Your Setup**

### **1. Upload a Video**
```bash
# Create test file
echo "fake video content" > test.mp4

# Upload via API
curl -X POST http://localhost:8082/api/upload/video \
  -H "X-User-Id: test-user-123" \
  -F "file=@test.mp4" \
  -F "title=Test Video" \
  -F "description=Test upload"

# Expected: JSON response with video ID and metadata
```

### **2. Retrieve Videos**
```bash
# Get all user videos
curl -H "X-User-Id: test-user-123" \
  http://localhost:8082/api/upload/videos

# Get specific video
curl -H "X-User-Id: test-user-123" \
  http://localhost:8082/api/upload/video/{VIDEO_ID}
```

### **3. Test Soft Delete**
```bash
# Soft delete video
curl -X DELETE -H "X-User-Id: test-user-123" \
  http://localhost:8082/api/upload/video/{VIDEO_ID}

# Restore video
curl -X POST -H "X-User-Id: test-user-123" \
  http://localhost:8082/api/upload/video/{VIDEO_ID}/restore

# Permanent delete
curl -X DELETE -H "X-User-Id: test-user-123" \
  http://localhost:8082/api/upload/video/{VIDEO_ID}/permanent
```

## 🏭 **Production Testing**

### **Start Replication Environment**
```bash
# 1. Start master-slave setup
docker-compose -f docker-compose-replica.yml up -d

# 2. Wait for initialization (30-60 seconds)
docker-compose -f docker-compose-replica.yml logs -f

# 3. Verify replication
docker-compose -f docker-compose-replica.yml exec postgres-master \
  psql -U upload_user -d video_platform -c "SELECT * FROM pg_stat_replication;"

# 4. Check HAProxy stats
open http://localhost:8404
```

### **Test Load Balancing**
```bash
# Run application with replica config
java -jar build/libs/upload-service.jar \
  --spring.profiles.active=replica

# Writes go to master (port 5435)
# Reads distributed to master+slave (port 5436)
```

## ⚠️ **Common Issues**

### **Port Conflicts**
```bash
# Check if ports are in use
lsof -i :5433  # PostgreSQL
lsof -i :6380  # Redis
lsof -i :8082  # Application

# Solution: Stop conflicting services or change ports
```

### **Docker Volume Issues**
```bash
# Reset volumes if data corruption
./scripts/docker-dev.sh clean

# Manual volume cleanup
docker volume ls | grep upload
docker volume rm upload_postgres_data upload_redis_data
```

### **Database Connection Errors**
```bash
# Check database status
./scripts/docker-dev.sh logs postgres

# Common solutions:
1. Wait for health check to pass (10-30 seconds)
2. Verify credentials in application.yml
3. Check Docker network connectivity
```

## 📊 **Monitoring During Development**

### **Application Health**
```bash
# Health check
curl http://localhost:8082/actuator/health

# Metrics
curl http://localhost:8082/actuator/metrics

# Database connection pool
curl http://localhost:8082/actuator/metrics/hikaricp.connections.active
```

### **Database Queries**
```sql
-- Connect to database
./scripts/docker-dev.sh shell-db

-- Check recent uploads
SELECT id, title, status, uploaded_at, user_id FROM videos 
ORDER BY uploaded_at DESC LIMIT 5;

-- Monitor query performance
SELECT query, mean_time, calls FROM pg_stat_statements 
ORDER BY mean_time DESC LIMIT 5;
```

### **Redis Cache**
```bash
# Connect to Redis
./scripts/docker-dev.sh shell-redis

# Check cached data
KEYS *
INFO memory
INFO stats
```

## 🔄 **Development Workflow**

### **Making Changes**
```bash
# 1. Code changes
vim src/main/java/...

# 2. Hot reload (if using dev tools)
# Application automatically restarts

# 3. Database schema changes
# Add new changeset to: src/main/resources/db/changelog/changes/
# Liquibase applies automatically on restart

# 4. Test changes
curl -X POST ...
```

### **Debugging Database Issues**
```bash
# 1. Check application logs
./gradlew bootRun | grep SQL

# 2. Check database logs  
./scripts/docker-dev.sh logs postgres

# 3. Manual SQL testing
./scripts/docker-dev.sh shell-db
# Run queries manually
```

### **Environment Variables**
```bash
# Override defaults during development
export DB_USERNAME=custom_user
export S3_BUCKET_NAME=dev-bucket
export REDIS_PASSWORD=dev-password

./gradlew bootRun
```

## 📝 **Next Steps**

### **Integration with Other Services**
```yaml
# Add to your application.yml for full platform integration:
external-services:
  gateway: http://localhost:8080
  auth-service: http://localhost:8081
  streaming-service: http://localhost:8083
  encoding-service: http://localhost:8084
```

### **Production Checklist**
- [ ] Configure AWS S3 credentials
- [ ] Set up RabbitMQ connection
- [ ] Enable Redis authentication
- [ ] Configure SSL/TLS certificates
- [ ] Set up monitoring and alerting
- [ ] Configure backup schedules
- [ ] Test failover procedures

This guide gets you productive quickly while the full technical documentation provides depth when needed.
