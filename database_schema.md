# Database and Infrastructure Documentation

## General Information
- **Database**: PostgreSQL (Master-Slave replication)
- **Schema Management**: Liquibase
- **Message Broker**: RabbitMQ
- **Cache**: Redis
- **Object Storage**: AWS S3
- **CDN**: AWS CloudFront

## PostgreSQL Database Schema

### Tables

#### users
**Source**: `authentication/src/main/resources/db/changelog/changes/v1.0-userauth-changelog.sql`
- **id**: UUID (Primary Key)
- **username**: VARCHAR(32) NOT NULL (unique constraint removed in v2.0)
- **password**: VARCHAR(128) NOT NULL
- **email**: VARCHAR(128) NOT NULL
- **provider**: VARCHAR(30) (OAuth2 provider, added in v2.0)
- **provider_id**: VARCHAR(255) (OAuth2 provider ID, added in v2.0)
- **created_at**: TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
- **modified_at**: TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()

#### roles
**Source**: `authentication/src/main/resources/db/changelog/changes/v1.0-userauth-changelog.sql`
- **id**: UUID (Primary Key)
- **name**: VARCHAR(64) NOT NULL UNIQUE
- **created_at**: TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
- **modified_at**: TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
- **created_by**: UUID NOT NULL
- **modified_by**: UUID NOT NULL

#### user_roles
**Source**: `authentication/src/main/resources/db/changelog/changes/v1.0-userauth-changelog.sql`
- **id**: UUID (Primary Key)
- **user_id**: UUID NOT NULL (References users.id)
- **role_id**: UUID NOT NULL (References roles.id)
- **created_at**: TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
- **modified_at**: TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
- **created_by**: UUID NOT NULL
- **modified_by**: UUID NOT NULL

#### permissions
**Source**: `authentication/src/main/resources/db/changelog/changes/v1.0-userauth-changelog.sql`
- **id**: UUID (Primary Key)
- **name**: VARCHAR(64) NOT NULL UNIQUE
- **description**: VARCHAR(256)
- **created_at**: TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
- **modified_at**: TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
- **created_by**: UUID NOT NULL
- **modified_by**: UUID NOT NULL

#### role_permissions
**Source**: `authentication/src/main/resources/db/changelog/changes/v1.0-userauth-changelog.sql`
- **id**: UUID (Primary Key)
- **role_id**: UUID NOT NULL (References roles.id)
- **permission_id**: UUID NOT NULL (References permissions.id)
- **level**: VARCHAR(128) NOT NULL
- **created_at**: TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
- **modified_at**: TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
- **created_by**: UUID NOT NULL
- **modified_by**: UUID NOT NULL

#### videos
**Source**: `upload/src/main/resources/db/changelog/changes/v1.0-video-changelog.sql`
- **id**: UUID (Primary Key)
- **title**: VARCHAR(255) NOT NULL
- **description**: TEXT
- **original_filename**: VARCHAR(255) NOT NULL
- **file_size**: BIGINT NOT NULL
- **mime_type**: VARCHAR(100)
- **s3_key**: VARCHAR(512) (S3 object key)
- **status**: VARCHAR(50) NOT NULL DEFAULT 'UPLOADED'
- **uploaded_at**: TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
- **user_id**: VARCHAR(255) NOT NULL
- **duration**: BIGINT (Video duration in seconds)
- **views_count**: BIGINT DEFAULT 0 (Total number of views)
- **last_accessed**: TIMESTAMP WITH TIME ZONE (Last streaming access)
- **created_at**: TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
- **modified_at**: TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
- **created_by**: VARCHAR(255) NOT NULL
- **modified_by**: VARCHAR(255) NOT NULL
- **deleted_at**: TIMESTAMP WITH TIME ZONE (Soft delete)

**Indexes**:
- idx_videos_user_id ON videos(user_id)
- idx_videos_status ON videos(status)
- idx_videos_uploaded_at ON videos(uploaded_at)
- idx_videos_user_status ON videos(user_id, status)
- idx_videos_status_created_at ON videos(status, created_at DESC)
- idx_videos_status_views_count ON videos(status, views_count DESC)
- idx_videos_status_last_accessed ON videos(status, last_accessed DESC)
- idx_videos_title_status ON videos(title, status)
- idx_videos_deleted_at ON videos(deleted_at) WHERE deleted_at IS NOT NULL

#### encoding_jobs
**Source**: `encoding/src/main/resources/db/changelog/changes/v1.0-encoding-job-table.sql`
- **id**: UUID (Primary Key)
- **video_id**: UUID NOT NULL (References videos.id with CASCADE)
- **user_id**: VARCHAR(255) NOT NULL
- **title**: VARCHAR(255) NOT NULL
- **original_filename**: VARCHAR(255) NOT NULL
- **file_size**: BIGINT NOT NULL
- **mime_type**: VARCHAR(100)
- **s3_key**: VARCHAR(512) NOT NULL
- **status**: VARCHAR(50) NOT NULL DEFAULT 'PENDING'
- **started_at**: TIMESTAMP WITH TIME ZONE
- **completed_at**: TIMESTAMP WITH TIME ZONE
- **error_message**: TEXT
- **retry_count**: INTEGER DEFAULT 0
- **progress**: INTEGER DEFAULT 0
- **created_at**: TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
- **modified_at**: TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
- **created_by**: VARCHAR(255) NOT NULL
- **modified_by**: VARCHAR(255) NOT NULL
- **deleted_at**: TIMESTAMP WITH TIME ZONE (Soft delete)

**Indexes**:
- idx_encoding_jobs_video_id ON encoding_jobs(video_id)
- idx_encoding_jobs_user_id ON encoding_jobs(user_id)
- idx_encoding_jobs_status ON encoding_jobs(status)
- idx_encoding_jobs_created_at ON encoding_jobs(created_at)
- idx_encoding_jobs_deleted_at ON encoding_jobs(deleted_at) WHERE deleted_at IS NOT NULL

#### view_sessions
**Source**: `streaming/src/main/resources/db/changelog/changes/v1.0-streaming-database-setup.sql`
- **id**: UUID PRIMARY KEY DEFAULT gen_random_uuid()
- **video_id**: UUID NOT NULL (References videos.id with CASCADE)
- **user_id**: VARCHAR(100) (Can be null for anonymous viewers)
- **session_id**: VARCHAR(100) NOT NULL UNIQUE (Unique session identifier)
- **ip_address**: VARCHAR(45)
- **user_agent**: TEXT
- **started_at**: TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
- **last_heartbeat**: TIMESTAMP DEFAULT CURRENT_TIMESTAMP
- **watch_duration**: BIGINT DEFAULT 0 (Total watch time in seconds)
- **max_position**: BIGINT DEFAULT 0 (Maximum position reached in seconds)
- **quality**: VARCHAR(20) DEFAULT 'AUTO' (Video quality: AUTO, Q_480P, Q_720P, Q_1080P)
- **is_complete**: BOOLEAN DEFAULT false
- **ended_at**: TIMESTAMP
- **created_at**: TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
- **modified_at**: TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL

**Indexes**:
- idx_view_sessions_video_id ON view_sessions(video_id)
- idx_view_sessions_user_id ON view_sessions(user_id)
- idx_view_sessions_session_id ON view_sessions(session_id)
- idx_view_sessions_started_at ON view_sessions(started_at)
- idx_view_sessions_last_heartbeat ON view_sessions(last_heartbeat)
- idx_view_sessions_ended_at ON view_sessions(ended_at)
- idx_view_sessions_ip_address ON view_sessions(ip_address)

**Constraints**:
- chk_view_sessions_quality CHECK (quality IN ('AUTO', 'Q_480P', 'Q_720P', 'Q_1080P'))
- chk_view_sessions_durations CHECK (watch_duration >= 0 AND max_position >= 0)

## Relationships

### Foreign Key Constraints
- **user_roles.user_id** → **users.id** (many-to-one)
- **user_roles.role_id** → **roles.id** (many-to-one)
- **role_permissions.role_id** → **roles.id** (many-to-one)
- **role_permissions.permission_id** → **permissions.id** (many-to-one)
- **encoding_jobs.video_id** → **videos.id** (many-to-one with CASCADE DELETE/UPDATE)
- **view_sessions.video_id** → **videos.id** (many-to-one with CASCADE DELETE/UPDATE)

### Entity Relationships
- **users** ↔ **user_roles** ↔ **roles** (many-to-many through user_roles)
- **roles** ↔ **role_permissions** ↔ **permissions** (many-to-many through role_permissions)
- **videos** → **encoding_jobs** (one-to-many)
- **videos** → **view_sessions** (one-to-many)

## AWS S3 Bucket Structure

**Bucket Name**: `video-hosting-thesis`

### Folder Layout

```
video-hosting-thesis/
├── originals/
│   └── {uuid}/
│       └── original_video.mp4          # Raw uploaded files
├── encoded/
│   └── {video_id}/
│       ├── master.m3u8                 # HLS master playlist
│       │   ├── playlist.m3u8           # Quality-specific playlist
│       │   ├── segment_000.ts          # HLS video segments
│       │   ├── segment_001.ts
│       │   └── ...
│       ├── 720p/
│       │   ├── playlist.m3u8
│       │   ├── segment_000.ts
│       │   └── ...
│       └── 480p/
│           ├── playlist.m3u8
│           ├── segment_000.ts
│           └── ...
└── thumbnails/
    └── {video_id}/
        ├── thumbnail_1080p.jpg         # Quality-specific thumbnails
        ├── thumbnail_720p.jpg
        └── thumbnail_480p.jpg
```

### Content Types and Access Control
- **HLS Playlists (.m3u8)**: `application/vnd.apple.mpegurl`
- **Video Segments (.ts)**: `video/mp2t`
- **MP4 Files**: `video/mp4`
- **JPEG Images**: `image/jpeg`
- **PNG Images**: `image/png`
- **Access**: Public read access through CloudFront CDN with signed URLs

### S3 Operations
**Services**: Upload Service, Encoding Service, Streaming Service
- **Upload**: Store original files in `originals/{uuid}/`
- **Encoding**: 
  - Download from `originals/` for processing
  - Upload encoded HLS content to `encoded/{video_id}/`
  - Generate and upload thumbnails to `thumbnails/{video_id}/`
- **Streaming**: Serve content via CloudFront CDN

## RabbitMQ Message Broker

### Configuration
- **Host**: localhost (default)
- **Port**: 5672
- **Management Interface**: http://localhost:15672 (guest/guest)
- **Virtual Host**: `/`

### Exchanges and Queues

#### Main Exchange
- **Name**: `video.exchange`
- **Type**: Topic Exchange

#### Queues and Routing Keys

| Queue | Routing Key | Purpose | Consumer |
|-------|-------------|---------|----------|
| `video.encoding.queue` | `video.encoding` | Video encoding jobs | Encoding Service |
| `video.metadata.queue` | `video.metadata` | Metadata updates | Metadata Service |
| `video.search.queue` | `video.search.refresh` | Search index updates | Metadata Service |
| `video.streaming.queue` | `video.streaming` | Streaming notifications | Streaming Service |

### Message Flow
1. **Upload Service** → publishes to `video.encoding` → **Encoding Service**
2. **Encoding Service** → publishes to `video.streaming` → **Streaming Service**
3. **Upload Service** → publishes to `video.metadata` → **Metadata Service**
4. **Encoding Service** → publishes to `video.search.refresh` → **Metadata Service**

### Message Payload Example
```json
{
  "videoId": "550e8400-e29b-41d4-a716-446655440000",
  "userId": "user123",
  "title": "Video Title",
  "originalFilename": "video.mp4",
  "fileSize": 104857600,
  "mimeType": "video/mp4",
  "s3Key": "originals/uuid/video.mp4",
  "status": "UPLOADED"
}
```

## Redis Cache

### Configuration
- **Host**: localhost (default)
- **Port**: 6379
- **Protocol**: Redis Serialization Protocol (RESP)
- **Persistence**: Enabled with data volume mounting

### Usage Patterns

#### Session Management
- **Upload Service**: Multipart upload session storage
- **Streaming Service**: View session caching
- **Authentication Service**: User session caching

#### Cache Keys Structure
```
multipart:uploads:{sessionId}     # Multipart upload sessions
view:sessions:{sessionId}         # Video viewing sessions  
user:sessions:{userId}            # User authentication sessions
video:metadata:{videoId}          # Video metadata caching
```

#### Redis Operations
- **SET/GET**: Session data storage and retrieval
- **EXPIRE**: Time-based session cleanup
- **DEL**: Manual session cleanup
- **KEYS**: Pattern-based key discovery (development only)

### Data Types Used
- **Strings**: Session identifiers and simple values
- **Hashes**: Complex session data structures
- **Sets**: User permission caches
- **Sorted Sets**: Time-ordered data (view statistics)

## Infrastructure Services

### Database Configuration
- **PostgreSQL Master**: Port 5432 (read/write)
- **PostgreSQL Slave**: Port 5433 (read-only)
- **Connection Pooling**: HikariCP
- **High Availability**: Master-slave replication with HAProxy load balancer

### Monitoring and Management
- **PostgreSQL**: pgAdmin interface
- **RabbitMQ**: Management web interface (port 15672)
- **Redis**: Redis Commander interface (port 8081)
- **Infrastructure**: Docker Compose with health checks

### Security Considerations
- **Database**: SSL connections, encrypted passwords
- **S3**: IAM roles, bucket policies, signed URLs
- **RabbitMQ**: AMQP over TLS in production
- **Redis**: Password protection, network isolation

## Notes
- **Auto-generated**: Documentation extracted from Liquibase changelogs and service configurations
- **Schema Evolution**: Managed through Liquibase version control
- **Scalability**: Designed for horizontal scaling with microservices architecture
- **Performance**: Optimized with appropriate indexes and caching strategies 