# Upload Service

Upload Service is part of the Video Hosting Platform and handles video file upload processing.

## Features

- **Video file uploads** to Amazon S3
- **File validation** (type, size)
- **Video metadata storage** in PostgreSQL
- **Message publishing** to RabbitMQ for further processing
- **User authentication** via Gateway
- **CRUD operations** for video management
- **Database migrations** via Liquibase
- **Automatic audit** for created/modified fields
- **Soft delete** for data recovery (files still deleted from S3)
- **Redis caching** for performance optimization
- **Database replication** support for high availability
- **Multipart Upload** for large video files with chunked uploading

## 🧩 Multipart Upload Support

Support for multipart upload of large video files using S3 chunked uploading.

### ✅ **Why Multipart Upload?**

For video hosting platforms, multipart upload is **essential** because:

- 🔄 **Resumable uploads** - continue interrupted uploads
- ⚡ **Parallel chunks** - upload multiple parts simultaneously  
- 🛡️ **Reliability** - only failed parts need to be retried
- 🚀 **Performance** - faster for large files (>100MB)
- 💾 **Memory efficient** - no need to load entire file in memory

### 📊 **Technical Details**

- **Minimum file size**: 5 MB (S3 requirement)
- **Optimal chunk size**: 5-10 MB per part
- **Maximum parts**: 10,000 per upload
- **Session storage**: Redis with 24h TTL
- **Progress tracking**: Real-time upload progress
- **Error handling**: Automatic cleanup on failures

### 🔗 **API Endpoints**

```http
POST   /api/upload/multipart/initiate     # Start multipart upload
POST   /api/upload/multipart/upload-chunk # Upload single chunk
POST   /api/upload/multipart/complete/{id} # Complete upload
DELETE /api/upload/multipart/abort/{id}   # Cancel upload
GET    /api/upload/multipart/status/{id}  # Check progress
```

### 🏗️ **Architecture Enhancement**

```
Frontend ──► MultipartController ──► MultipartService ──► S3
    │              │                      │               │
    │              │                      └─► Redis ──────┘
    │              │                      │
    │              │                      └─► PostgreSQL ──► RabbitMQ
    │              │
    └─── Progress ←┘
```

### 📚 **Documentation**

For detailed multipart upload usage, examples, and integration guide, see:
**[MULTIPART_UPLOAD.md](MULTIPART_UPLOAD.md)**

## Database Schema

Database migrations are managed by Liquibase. The main entities:

### Video Entity
- **id** (UUID) - Primary key
- **title** (VARCHAR) - Video title
- **description** (TEXT) - Video description
- **original_filename** (VARCHAR) - Original file name
- **file_size** (BIGINT) - File size in bytes
- **mime_type** (VARCHAR) - MIME type
- **s3_key** (VARCHAR) - S3 object key
- **status** (VARCHAR) - Video processing status
- **uploaded_at** (TIMESTAMP) - Upload timestamp
- **user_id** (VARCHAR) - User identifier
- **created_at** (TIMESTAMP) - Record creation timestamp
- **modified_at** (TIMESTAMP) - Record modification timestamp
- **created_by** (VARCHAR) - User who created the record
- **modified_by** (VARCHAR) - User who last modified the record
- **deleted_at** (TIMESTAMP) - Soft delete timestamp (null = not deleted)

## API Endpoints

### 📋 **Upload Method Selection:**

```
Client
├── Small files (<5MB) ──► POST /api/upload/video (only option)
├── Medium files (5-100MB) ──► Any method
└── Large files (>100MB) ──► POST /api/upload/multipart/* (recommended)
```

### Upload video
```http
POST /api/upload/video
Content-Type: multipart/form-data

Parameters:
- file: video file (required)
- title: video title (required)
- description: video description (optional)
```

### Upload video with metadata
```http
POST /api/upload/video-with-metadata
Content-Type: multipart/form-data
```

### Get video by ID
```http
GET /api/upload/video/{videoId}
```

### Get user's video list
```http
GET /api/upload/videos
```

### Delete video (soft delete)
```http
DELETE /api/upload/video/{videoId}
```
Note: This performs soft delete - marks video as deleted in database but keeps metadata for recovery. S3 file is permanently deleted.

### Restore deleted video
```http
POST /api/upload/video/{videoId}/restore
```
Note: Restores a soft-deleted video. S3 file cannot be restored if it was already deleted.

### Permanently delete video
```http
DELETE /api/upload/video/{videoId}/permanent
```
Note: Permanently removes video metadata from database. Video must be soft-deleted first.

### Service health check
```http
GET /api/upload/health
```

## Docker Development Setup

### Single Instance (Development)

```bash
# Start PostgreSQL and Redis
./scripts/docker-dev.sh start

# Start with admin tools (pgAdmin, Redis Commander)
./scripts/docker-dev.sh start-admin

# View logs
./scripts/docker-dev.sh logs

# Stop services
./scripts/docker-dev.sh stop

# Clean up (removes all data!)
./scripts/docker-dev.sh clean
```

**Services:**
- **PostgreSQL**: `localhost:5433` (auth service uses 5432)
- **Redis**: `localhost:6380`
- **pgAdmin**: `http://localhost:5051` (admin/admin)
- **Redis Commander**: `http://localhost:8085` (upload service uses 8082)

### Database Replication Setup

```bash
# Start Master-Slave PostgreSQL replication
docker-compose -f docker-compose-replica.yml up -d

# Check replication status
docker-compose -f docker-compose-replica.yml exec postgres-master \
  psql -U upload_user -d video_platform -c "SELECT * FROM pg_stat_replication;"

# Stop replication setup
docker-compose -f docker-compose-replica.yml down
```

**Replication Services:**
- **Write Operations**: `localhost:5435` (via HAProxy → Master)
- **Read Operations**: `localhost:5436` (via HAProxy → Master + Slave)
- **Master Direct**: `localhost:5433`
- **Slave Direct**: `localhost:5434`
- **HAProxy Stats**: `http://localhost:8404`

**Port Allocation Summary:**
```
Auth Service:     localhost:8081
Upload Service:   localhost:8082
Streaming Service: localhost:8083
Gateway:          localhost:8080

Auth PostgreSQL:  localhost:5432
Upload PostgreSQL: localhost:5433
Upload PostgreSQL Master: localhost:5433
Upload PostgreSQL Slave:  localhost:5434
Upload HAProxy Write:     localhost:5435
Upload HAProxy Read:      localhost:5436

Upload Redis:     localhost:6380
pgAdmin:          localhost:5051
Redis Commander:  localhost:8085
HAProxy Stats:    localhost:8404
```

## Database Replication Analysis

### 🔄 **Types of PostgreSQL Replication**

#### **1. Streaming Replication (Hot Standby)**
```
Master ----WAL Stream----> Slave
```

**Плюсы:**
- ✅ Простота настройки и управления
- ✅ Минимальная задержка репликации (< 1s)
- ✅ Автоматическое восстановление после сбоев
- ✅ Возможность чтения с replica
- ✅ Встроенная поддержка в PostgreSQL

**Минусы:**
- ❌ Асинхронная репликация (возможна потеря данных)
- ❌ Single point of failure (master)
- ❌ Ручное переключение на slave при сбое master

#### **2. Synchronous Replication**
```yaml
# В postgresql.conf master
synchronous_standby_names = 'slave1'
synchronous_commit = on
```

**Плюсы:**
- ✅ Гарантированная консистентность данных
- ✅ Нет потери данных при сбое master

**Минусы:**
- ❌ Значительно медленнее (ждет подтверждения от slave)
- ❌ При недоступности slave блокируются записи

#### **3. Logical Replication**
```sql
-- На master
CREATE PUBLICATION video_pub FOR TABLE videos;

-- На slave  
CREATE SUBSCRIPTION video_sub 
CONNECTION 'host=master port=5432 user=replica_user dbname=video_platform' 
PUBLICATION video_pub;
```

**Плюсы:**
- ✅ Селективная репликация (только нужные таблицы)
- ✅ Возможность фильтрации данных
- ✅ Репликация между разными версиями PostgreSQL
- ✅ Bi-directional replication

**Минусы:**
- ❌ Больше нагрузки на master
- ❌ Сложнее настройка и мониторинг

### ⚖️ **Сравнение подходов для Upload Service**

| Критерий | Streaming | Synchronous | Logical |
|----------|-----------|-------------|---------|
| **Простота** | 🟢 High | 🟡 Medium | 🔴 Low |
| **Производительность** | 🟢 High | 🔴 Low | 🟡 Medium |
| **Консистентность** | 🟡 Eventual | 🟢 Strong | 🟡 Eventual |
| **Failover** | 🟡 Manual | 🟡 Manual | 🔴 Complex |
| **Ресурсы** | 🟢 Low | 🟡 Medium | 🔴 High |

### 🎯 **Рекомендация для Upload Service**

**Для разработки/тестирования:**
```bash
./scripts/docker-dev.sh start  # Простая схема
```

**Для продакшена:**
```bash
docker-compose -f docker-compose-replica.yml up -d  # Streaming replication
```

### 📊 **Мониторинг репликации**

```sql
-- Проверка статуса репликации на master
SELECT client_addr, state, sent_lsn, write_lsn, flush_lsn, replay_lsn 
FROM pg_stat_replication;

-- Проверка задержки репликации на slave
SELECT EXTRACT(EPOCH FROM (now() - pg_last_xact_replay_timestamp())) AS lag_seconds;

-- Размер WAL файлов
SELECT pg_size_pretty(pg_wal_lsn_diff(pg_current_wal_lsn(), replay_lsn)) AS lag_size
FROM pg_stat_replication;
```

### 🛠️ **Управление репликацией**

```bash
# Promote slave to master (при сбое master)
docker-compose -f docker-compose-replica.yml exec postgres-slave \
  su - postgres -c "pg_ctl promote -D /var/lib/postgresql/data"

# Rebuild slave после failover
docker-compose -f docker-compose-replica.yml stop postgres-slave
docker volume rm upload_postgres_slave_data
docker-compose -f docker-compose-replica.yml up -d postgres-slave
```

## Soft Delete Behavior

- **Soft Delete**: `DELETE /api/upload/video/{videoId}`
  - Marks video as deleted in database (`deleted_at` timestamp)
  - Sets status to `DELETED`
  - **Permanently deletes file from S3** (for cost efficiency)
  - Video metadata remains in database for recovery

- **Restore**: `POST /api/upload/video/{videoId}/restore`
  - Removes `deleted_at` timestamp
  - Sets status back to `UPLOADED`
  - **Note**: S3 file cannot be restored and must be re-uploaded

- **Permanent Delete**: `DELETE /api/upload/video/{videoId}/permanent`
  - Physically removes record from database
  - Only works on already soft-deleted videos

## Configuration

### Environment Variables

#### Database
- `DB_USERNAME` - PostgreSQL username (default: upload_user)
- `DB_PASSWORD` - PostgreSQL password (default: upload_pass)

#### AWS S3
- `AWS_ACCESS_KEY` - AWS access key
- `AWS_SECRET_KEY` - AWS secret key
- `AWS_REGION` - AWS region (default: us-east-1)
- `S3_BUCKET_NAME` - S3 bucket name
- `S3_BUCKET_PREFIX` - file prefix (default: videos/)

#### Redis
- `REDIS_HOST` - Redis host (default: localhost)
- `REDIS_PORT` - Redis port (default: 6379)
- `REDIS_PASSWORD` - Redis password (optional)

#### RabbitMQ
- `RABBITMQ_HOST` - RabbitMQ host (default: localhost)
- `RABBITMQ_PORT` - RabbitMQ port (default: 5672)
- `RABBITMQ_USERNAME` - RabbitMQ username (default: guest)
- `RABBITMQ_PASSWORD` - RabbitMQ password (default: guest)

## Supported Video Formats

- MP4 (video/mp4)
- AVI (video/avi)
- MOV (video/mov)
- WMV (video/wmv)
- FLV (video/flv)
- WebM (video/webm)
- MKV (video/mkv)
- M4V (video/m4v)

## Limitations

- **Maximum file size**: 2GB
- **Authentication required** for all operations (except health check)
- **User isolation** - each user sees only their own videos
- **S3 files cannot be restored** after deletion (only metadata)

## Architecture

Upload Service integrates with:

1. **Amazon S3** - for video file storage
2. **PostgreSQL** - for metadata storage (with optional replication)
3. **Redis** - for caching and session storage
4. **RabbitMQ** - for sending messages to Encoding Service
5. **Gateway** - for user authentication via X-User-Id header

## JPA Configuration

Current JPA settings provide:

- **`ddl-auto: validate`** - Validates database schema matches entities without modifying it (production-safe)
- **`show-sql: true`** - Shows SQL queries in logs for debugging
- **`open-in-view: false`** - Prevents lazy loading outside transaction boundaries (avoids N+1 problems)

## Running

### Development (Local)
```bash
# Start infrastructure
./scripts/docker-dev.sh start

# Build and run application
./gradlew bootRun
```

### Production (with Replication)
```bash
# Start infrastructure with replication
docker-compose -f docker-compose-replica.yml up -d

# Build and run application
./gradlew build
java -jar build/libs/upload-0.0.1-SNAPSHOT.jar
```

Service will be available on port 8082.

## Database Migrations

Database schema is managed by Liquibase. Migration files are located in:
- `src/main/resources/db/changelog/db.changelog-master.yaml` - Master changelog
- `src/main/resources/db/changelog/changes/` - Individual migration files

To run migrations manually:
```bash
./gradlew update
```

## Monitoring

Available monitoring endpoints:
- `/actuator/health` - application health
- `/actuator/info` - application information
- `/actuator/metrics` - application metrics 