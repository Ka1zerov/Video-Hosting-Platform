# Video Hosting Platform - Complete Technical Documentation

## Table of Contents
1. [System Overview](#1-system-overview)
2. [Architecture Overview](#2-architecture-overview)
3. [Microservice Breakdown](#3-microservice-breakdown)
4. [Database Schema](#4-database-schema)
5. [S3 Bucket and File Structure](#5-s3-bucket-and-file-structure)
6. [Message Broker Architecture](#6-message-broker-architecture)
7. [Protocols and Standards](#7-protocols-and-standards)
8. [Infrastructure and Deployment](#8-infrastructure-and-deployment)
9. [Security Architecture](#9-security-architecture)
10. [Observability and Monitoring](#10-observability-and-monitoring)
11. [Development Workflow](#11-development-workflow)
12. [References and Documentation](#12-references-and-documentation)

---

## 1. System Overview

### 1.1 Platform Purpose
The Video Hosting Platform is a distributed microservices-based system designed for enterprise-grade video hosting, processing, and streaming. The platform was developed as part of a Master's thesis, demonstrating modern cloud-native architecture patterns and technologies.

### 1.2 Key Features
- **Secure Video Upload**: Multi-format video file upload with AWS S3 storage
- **Multi-Quality Transcoding**: Automatic video encoding to multiple bitrates (480p, 720p, 1080p)
- **Adaptive Streaming**: HLS and DASH streaming protocols for optimal video delivery
- **User Authentication**: OAuth2 PKCE flow with JWT tokens and social login (Google)
- **CDN Integration**: AWS CloudFront for global content delivery
- **Advanced Upload Features**: Multipart uploads for large files with automatic cleanup
- **Real-time Analytics**: View tracking, session management, and viewing statistics
- **High Availability**: Master-slave PostgreSQL replication with HAProxy load balancing
- **Microservices Architecture**: Independent, scalable services with async communication

### 1.3 Technology Stack
- **Backend**: Java 21, Spring Boot 3.4.5, Spring Security, Spring Data JPA
- **Frontend**: React 18.2.0, Material-UI, HLS.js
- **Databases**: PostgreSQL 15 (with replication), Redis 7
- **Message Broker**: RabbitMQ 3
- **Storage**: AWS S3, AWS CloudFront CDN
- **Infrastructure**: Docker Compose, HAProxy
- **Build Tools**: Gradle, Maven
- **Video Processing**: FFmpeg

---

## 2. Architecture Overview

### 2.1 High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────────────────────────────────────┐
│                                  Video Hosting Platform                                         │
├─────────────────────────────────────────────────────────────────────────────────────────────────┤
│  PRESENTATION LAYER                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────────────────────────┐    │
│  │                           React Frontend (Port: 3000)                                   │    │
│  │                      OAuth2 PKCE | Material-UI | HLS Video Player                       │    │
│  └─────────────────────────────────────────────────────────────────────────────────────────┘    │
│                                         │                                                       │
├─────────────────────────────────────────┼───────────────────────────────────────────────────────┤
│  API LAYER                              │                                                       │
│  ┌─────────────────────────────────────────────────────────────────────────────────────────┐    │
│  │                            API Gateway (Port: 8080)                                     │    │
│  │         JWT Authentication | CORS | Request Routing | Load Balancing                    │    │
│  └─────────────────────────────────────────────────────────────────────────────────────────┘    │
│         │              │                │                │                │                     │
├─────────┼──────────────┼────────────────┼────────────────┼────────────────┼─────────────────────┤
│  MICROSERVICES LAYER   │                │                │                │                     │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌──────────────┐           │
│  │    Auth     │  │   Upload    │  │  Metadata   │  │  Streaming  │  │  Encoding    │           │
│  │ Port: 8081  │  │ Port: 8082  │  │ Port: 8083  │  │ Port: 8084  │  │ Port: 8085   │           │
│  │             │  │             │  │             │  │             │  │              │           │
│  │ OAuth2/JWT  │  │ S3 Upload   │  │ Search &    │  │ HLS/DASH    │  │ Multi-Quality│           │
│  │ Google SSO  │  │ Multipart   │  │ Analytics   │  │ Streaming   │  │ Transcoding  │           │
│  │ User Mgmt   │  │ Validation  │  │ CDN URLs    │  │ Sessions    │  │ FFmpeg       │           │
│  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────┘  └──────────────┘           │
│         │                │                │                │                │                   │
├─────────┼────────────────┼────────────────┼────────────────┼────────────────┼───────────────────┤
│  SHARED INFRASTRUCTURE & DATABASES        │                │                │                   │
│         │                │                │                │                │                   │
│  ┌─────────────┐  ┌────────────────────────────────────────────────────────────────────────┐    │
│  │ PostgreSQL  │  │                 PostgreSQL Video DB (Master-Slave)                     │    │
│  │  Auth DB    │  │                      HAProxy Load Balancer                             │    │
│  │ Port: 5432  │  │  Master: 5433 (writes) | Slave: 5434 (reads)                           │    │
│  │             │  │  HAProxy Write: 5435 | HAProxy Read: 5436                              │    │
│  │ Users       │  │                                                                        │    │
│  │ Sessions    │  │ • videos • encoding_jobs • view_sessions                               │    │
│  │ Permissions │  │ • user_roles • roles • permissions                                     │    │
│  └─────────────┘  └────────────────────────────────────────────────────────────────────────┘    │
│                          │                │                │                │                   │
│  ┌─────────────────────────────────────────────────────────────────────────────────────────┐    │
│  │                           Redis Cache (Port: 6379)                                      │    │
│  │    • User sessions • Upload sessions • Metadata cache • JWT blacklist                   │    │
│  └─────────────────────────────────────────────────────────────────────────────────────────┘    │
│                          │                │                │                │                   │
│  ┌─────────────────────────────────────────────────────────────────────────────────────────┐    │
│  │                      RabbitMQ Message Broker (Port: 5672)                               │    │
│  │    • Video upload events • Encoding jobs • Processing completion • Analytics events     │    │
│  └─────────────────────────────────────────────────────────────────────────────────────────┘    │
│                          │                │                │                │                   │
│  ┌─────────────────────────────────────────────────────────────────────────────────────────┐    │
│  │                      AWS S3 Storage + CloudFront CDN                                    │    │
│  │  • Original videos • Encoded HLS segments • Thumbnails • Global content delivery        │    │
│  └─────────────────────────────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────────────────────────────┘
```

### 2.2 Service Communication Flow

**Video Upload Flow:**
```
Frontend → Gateway → Upload Service → S3 → RabbitMQ → Encoding Service → S3 → Metadata Service
```

**Video Streaming Flow:**
```
Frontend → Gateway → Streaming Service → S3/CloudFront → Video Player
```

**Authentication Flow:**
```
Frontend → Gateway → Auth Service → PostgreSQL → JWT Token → All Services
```

---

## 3. Microservice Breakdown

### 3.1 API Gateway Service (Port: 8080)

**Purpose**: Single entry point and API gateway for the distributed platform

**Key Responsibilities:**
- Request routing to appropriate backend services
- JWT-based authentication and authorization
- Cross-Origin Resource Sharing (CORS) configuration
- Security enforcement and user context propagation

**Technologies:**
- Java 21, Spring Boot 3.4.5
- Spring Cloud Gateway 4.2.2
- Spring Security, OAuth2 Resource Server
- Netty (async event-driven networking)

**API Routes:**
- `/oauth2/**`, `/login`, `/logout`, `/api/auth/**` → Auth Service (8081)
- `/api/upload/**` → Upload Service (8082)
- `/api/metadata/**` → Metadata Service (8083)
- `/api/streaming/**` → Streaming Service (8084)
- `/api/encoding/**` → Encoding Service (8085)

**Security Features:**
- JWT Bearer token validation
- X-User-Id header injection for downstream services
- CORS protection with configurable origins
- Path-based access control

### 3.2 Authentication Service (Port: 8081)

**Purpose**: Central identity provider with OAuth2 authorization server

**Key Responsibilities:**
- User registration and authentication
- OAuth2 PKCE flow implementation
- JWT token generation and validation
- Google OAuth2/OIDC integration
- Role-based access control (RBAC)

**Technologies:**
- Java 21, Spring Boot 3.4.4
- Spring Security, Spring OAuth2 Authorization Server
- PostgreSQL (dedicated auth database)
- Thymeleaf (login pages)

**Key Features:**
- OAuth2 Authorization Code Flow with PKCE
- Access Token TTL: 5 minutes
- Refresh Token TTL: 7 days
- Google social login integration
- BCrypt password encoding

**Database Tables:**
- `users`, `roles`, `permissions`
- `user_roles`, `role_permissions`

### 3.3 Upload Service (Port: 8082)

**Purpose**: Video file upload processing with advanced reliability features

**Key Responsibilities:**
- Video file uploads to AWS S3
- File validation (type, size, format)
- Multipart upload support for large files (>5MB)
- Automatic cleanup of expired upload sessions
- Message publishing to RabbitMQ for encoding

**Technologies:**
- Java 21, Spring Boot 3.4.5
- AWS S3 SDK, Spring Data JPA
- Redis (session storage), RabbitMQ (messaging)
- Liquibase (database migrations)

**Key Features:**
- **Standard Upload**: For files <5MB via multipart/form-data
- **Multipart Upload**: Chunked uploading for large files with:
  - Resumable uploads
  - Parallel chunk processing
  - Automatic session cleanup (24h TTL)
  - Progress tracking
  - S3 garbage collection

**API Endpoints:**
```
POST   /api/upload/video                        # Standard upload
POST   /api/upload/multipart/initiate           # Start multipart upload
POST   /api/upload/multipart/upload-chunk       # Upload chunk
POST   /api/upload/multipart/complete/{id}      # Complete upload
DELETE /api/upload/multipart/abort/{id}         # Cancel upload
GET    /api/upload/multipart/status/{id}        # Check progress
```

**Advanced Features:**
- **Transaction-safe message publishing** prevents data inconsistency
- **Automatic cleanup service** runs hourly to remove expired sessions
- **Soft delete** with S3 file cleanup for cost efficiency
- **Admin endpoints** for monitoring and management

### 3.4 Encoding Service (Port: 8085)

**Purpose**: Multi-bitrate video transcoding using FFmpeg

**Key Responsibilities:**
- Transcoding videos into multiple quality levels (1080p, 720p, 480p)
- HLS playlist and segment generation
- Thumbnail creation for each quality level
- Asynchronous job processing via RabbitMQ
- Progress tracking and error handling

**Technologies:**
- Java 21, Spring Boot 3.4.5
- FFmpeg (video transcoding engine)
- AWS S3 (storage), PostgreSQL (job tracking)
- RabbitMQ (job queue), Redis (caching)

**Processing Pipeline:**
1. Receive video upload message from RabbitMQ
2. Download original video from S3
3. Transcode to multiple bitrates using FFmpeg
4. Generate HLS playlists and segments
5. Create thumbnails for each quality
6. Upload encoded content to S3
7. Update job status and notify downstream services

**Output Structure:**
```
encoded/{video_id}/
├── 1080p/playlist.m3u8 + segments
├── 720p/playlist.m3u8 + segments
├── 480p/playlist.m3u8 + segments
└── master.m3u8 (quality selector)
```

**Job Management:**
- Real-time progress tracking
- Retry mechanism for failed jobs
- Comprehensive error handling
- Automatic cleanup of temporary files

### 3.5 Metadata Service (Port: 8083)

**Purpose**: Video metadata management, search, and analytics

**Key Responsibilities:**
- Extended video metadata management
- Full-text search functionality
- Popular and recently watched recommendations
- Video analytics and view tracking
- CDN URL generation

**Technologies:**
- Java 21, Spring Boot 3.4.5
- Spring Data JPA, PostgreSQL (shared database)
- Redis (caching), AWS S3/CloudFront

**API Endpoints:**
```
GET /api/metadata/videos                 # Paginated video list
GET /api/metadata/videos/{videoId}       # Video details
GET /api/metadata/videos/search          # Full-text search
GET /api/metadata/videos/popular         # Most viewed videos
GET /api/metadata/videos/recent          # Recently watched
```

**Key Features:**
- Shared database architecture with Upload Service
- Liquibase context separation for schema management
- CDN-optimized URL generation
- Advanced caching strategies

### 3.6 Streaming Service (Port: 8084)

**Purpose**: Video streaming, session management, and viewing analytics

**Key Responsibilities:**
- HLS and DASH streaming protocol support
- Quality selection and adaptive streaming
- User viewing session management
- Real-time analytics and view tracking
- CDN integration with signed URLs

**Technologies:**
- Java 21, Spring Boot 3.4.5
- PostgreSQL (shared database), Redis (session storage)
- AWS S3, CloudFront CDN integration

**Core Features:**
- **Video Playback**: HLS master playlists with quality selection
- **Session Management**: Real-time session tracking with heartbeat
- **Analytics**: View counts, completion rates, watch time tracking
- **Quality Enum**: Static quality levels (480p, 720p, 1080p) for MVP simplicity
- **CDN Integration**: CloudFront signed URLs with 2-hour expiration

**Session Lifecycle:**
1. Start session with video ID and user ID
2. Send periodic heartbeat updates (position, quality)
3. Track watch duration and maximum position
4. End session with completion status
5. Update video analytics and view counts

### 3.7 Frontend Application (Port: 3000)

**Purpose**: React-based user interface for the video hosting platform

**Key Responsibilities:**
- User authentication with OAuth2 PKCE flow
- Video browsing, search, and discovery
- Video upload with progress tracking
- HLS video playback with quality selection
- Responsive Material-UI design

**Technologies:**
- React 18.2.0, Material-UI 5.15.0
- React Router DOM, Axios (HTTP client)
- HLS.js, React Player (video streaming)
- Custom OAuth2 PKCE implementation

**Key Features:**
- **Authentication**: Memory-stored access tokens, HTTP-only refresh cookies
- **Video Player**: HLS streaming with quality selection
- **Upload Interface**: Drag-and-drop with progress tracking
- **Search & Discovery**: Full-text search, popular videos, categories
- **Responsive Design**: Mobile-friendly Material-UI components

---

## 4. Database Schema

### 4.1 Database Architecture

**Database Strategy:**
- **Separate Databases**: `auth_db` (authentication) and `video_platform` (shared by video services)
- **Master-Slave Replication**: PostgreSQL streaming replication for high availability
- **HAProxy Load Balancing**: Intelligent routing of read/write operations

### 4.2 Authentication Database (`auth_db`)

**Core Tables:**
- **users**: User accounts with OAuth2 provider support
- **roles**: Role definitions (admin, user, moderator)
- **permissions**: Granular permissions (video.read, video.write, etc.)
- **user_roles**: Many-to-many user-role relationships
- **role_permissions**: Role-permission assignments with levels

### 4.3 Video Platform Database (`video_platform`)

**Primary Tables:**

#### videos
```sql
id UUID PRIMARY KEY
title VARCHAR(255) NOT NULL
description TEXT
original_filename VARCHAR(255) NOT NULL
file_size BIGINT NOT NULL
mime_type VARCHAR(100)
s3_key VARCHAR(512)
status VARCHAR(50) DEFAULT 'UPLOADED'
uploaded_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
user_id VARCHAR(255) NOT NULL
duration BIGINT -- Video duration in seconds
views_count BIGINT DEFAULT 0
last_accessed TIMESTAMP WITH TIME ZONE
created_at, modified_at, created_by, modified_by
deleted_at TIMESTAMP WITH TIME ZONE -- Soft delete
```

**Key Indexes:**
- `idx_videos_user_id`, `idx_videos_status`
- `idx_videos_status_views_count` (for popular videos)
- `idx_videos_title_status` (for search)

#### encoding_jobs
```sql
id UUID PRIMARY KEY
video_id UUID NOT NULL REFERENCES videos(id) CASCADE
user_id VARCHAR(255) NOT NULL
title VARCHAR(255) NOT NULL
s3_key VARCHAR(512) NOT NULL
status VARCHAR(50) DEFAULT 'PENDING'
started_at, completed_at TIMESTAMP WITH TIME ZONE
error_message TEXT
retry_count INTEGER DEFAULT 0
progress INTEGER DEFAULT 0 -- 0-100%
```

#### view_sessions
```sql
id UUID PRIMARY KEY
video_id UUID NOT NULL REFERENCES videos(id) CASCADE
user_id VARCHAR(100) -- Nullable for anonymous viewers
session_id VARCHAR(100) NOT NULL UNIQUE
ip_address VARCHAR(45)
user_agent TEXT
started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
last_heartbeat TIMESTAMP
watch_duration BIGINT DEFAULT 0 -- Total watch time in seconds
max_position BIGINT DEFAULT 0 -- Maximum position reached
quality VARCHAR(20) DEFAULT 'AUTO' -- Q_480P, Q_720P, Q_1080P
is_complete BOOLEAN DEFAULT false
ended_at TIMESTAMP
```

**Quality Constraint:**
```sql
CHECK (quality IN ('AUTO', 'Q_480P', 'Q_720P', 'Q_1080P'))
```

### 4.4 Database Migrations

**Liquibase Management:**
- **Upload Service**: Context `upload-service` for video-related tables
- **Metadata Service**: Context `metadata-service` for search and analytics
- **Streaming Service**: Context `streaming-service` for session management
- **Encoding Service**: Context `encoding-service` for job tracking

**Migration Sources:**
- Upload: `upload/src/main/resources/db/changelog/changes/v1.0-video-changelog.sql`
- Streaming: `streaming/src/main/resources/db/changelog/changes/v1.0-streaming-database-setup.sql`
- Encoding: `encoding/src/main/resources/db/changelog/changes/v1.0-encoding-job-table.sql`

---

## 5. S3 Bucket and File Structure

### 5.1 AWS S3 Bucket Organization

**Bucket Name**: `video-hosting-thesis`

**Folder Structure:**
```
video-hosting-thesis/
├── originals/
│   └── {uuid}/
│       └── original_video.mp4          # Raw uploaded files
├── encoded/
│   └── {video_id}/
│       ├── master.m3u8                 # HLS master playlist
│       ├── 1080p/
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

### 5.2 Content Types and Access Control

**MIME Types:**
- **HLS Playlists (.m3u8)**: `application/vnd.apple.mpegurl`
- **Video Segments (.ts)**: `video/mp2t`
- **MP4 Files**: `video/mp4`
- **Thumbnails**: `image/jpeg`, `image/png`

**Access Control:**
- **Public Read Access**: Through CloudFront CDN
- **Signed URLs**: Time-limited access (2-hour expiration)
- **Private Storage**: Direct S3 access restricted
- **IAM Roles**: Service-specific permissions

### 5.3 CDN Integration

**AWS CloudFront Configuration:**
- **Global Distribution**: Edge locations for low latency
- **Signed URLs**: Secure content delivery
- **Cache Behavior**: Optimized for video streaming
- **Origins**: S3 bucket as primary origin

---

## 6. Message Broker Architecture

### 6.1 RabbitMQ Configuration

**Connection Details:**
- **Host**: localhost (default)
- **Port**: 5672 (AMQP), 15672 (Management UI)
- **Virtual Host**: `/`
- **Credentials**: guest/guest (development)

### 6.2 Exchange and Queue Structure

**Main Exchange:**
- **Name**: `video.exchange`
- **Type**: Topic Exchange
- **Durability**: Persistent

**Queue Configuration:**

| Queue | Routing Key | Consumer | Purpose |
|-------|-------------|----------|---------|
| `video.encoding.queue` | `video.encoding` | Encoding Service | Video transcoding jobs |
| `video.metadata.queue` | `video.metadata` | Metadata Service | Metadata updates |
| `video.search.queue` | `video.search.refresh` | Metadata Service | Search index updates |
| `video.streaming.queue` | `video.streaming` | Streaming Service | Streaming notifications |

### 6.3 Message Flow Patterns

**Video Processing Pipeline:**
```
Upload Service → video.encoding → Encoding Service
Encoding Service → video.streaming → Streaming Service
Upload Service → video.metadata → Metadata Service
Encoding Service → video.search.refresh → Metadata Service
```

**Message Payload Example:**
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

---

## 7. Protocols and Standards

### 7.1 HTTP and REST Standards

**IETF RFC References:**
- **RFC 2616**: HTTP/1.1 protocol specification
- **RFC 7231**: HTTP/1.1 semantics and content
- **RFC 7234**: HTTP/1.1 caching mechanisms
- **RFC 3986**: URI generic syntax

### 7.2 Authentication and Security

**OAuth2 and JWT Standards:**
- **RFC 6749**: OAuth 2.0 Authorization Framework
- **RFC 7636**: Proof Key for Code Exchange (PKCE)
- **RFC 7519**: JSON Web Token (JWT)
- **RFC 7517**: JSON Web Key (JWK)
- **RFC 6750**: OAuth 2.0 Bearer Token Usage

### 7.3 Video Streaming Protocols

**HLS (HTTP Live Streaming):**
- **RFC 8216**: HTTP Live Streaming specification
- **Segment Duration**: 10 seconds
- **Playlist Window**: 5 segments
- **Quality Levels**: 480p, 720p, 1080p

**DASH (Dynamic Adaptive Streaming):**
- **MPEG-DASH**: ISO/IEC 23009-1 standard
- **Segment Duration**: 10 seconds
- **Manifest Format**: XML-based

### 7.4 Message Queue Protocol

**AMQP (Advanced Message Queuing Protocol):**
- **Version**: AMQP 0-9-1
- **Exchange Types**: Topic, Direct, Fanout
- **Message Persistence**: Durable queues and messages
- **Acknowledgments**: Manual acknowledgment for reliability

---

## 8. Infrastructure and Deployment

### 8.1 Docker Compose Architecture

**Service Profiles:**
- **`auth`**: Authentication database only
- **`video`**: Video services (PostgreSQL master+slave, Redis, RabbitMQ, HAProxy)
- **`full`**: All core services (default)
- **`admin`**: All services + admin tools

### 8.2 Database Replication Setup

**PostgreSQL Master-Slave Configuration:**

**Master Database (Port 5433):**
- Handles all write operations
- Streaming replication to slave
- WAL (Write-Ahead Logging) enabled

**Slave Database (Port 5434):**
- Read-only replica
- Real-time synchronization
- Automatic recovery capabilities

**HAProxy Load Balancer:**
- **Write Port (5435)**: Routes writes to master only
- **Read Port (5436)**: Load balances reads between master and slave
- **Health Checks**: Automatic failover capabilities
- **Statistics**: http://localhost:8404/stats

### 8.3 Service Dependencies

**Startup Sequence:**
1. Infrastructure services (PostgreSQL, Redis, RabbitMQ)
2. Database replication setup
3. Upload Service (creates shared database tables)
4. Metadata, Encoding, Streaming Services
5. Gateway Service
6. Authentication Service
7. Frontend Application

### 8.4 Port Allocation

**Microservices:**
- Frontend: 3000
- Gateway: 8080
- Authentication: 8081
- Upload: 8082
- Metadata: 8083
- Streaming: 8084
- Encoding: 8085

**Infrastructure:**
- PostgreSQL Auth: 5432
- PostgreSQL Master: 5433
- PostgreSQL Slave: 5434
- HAProxy Write: 5435
- HAProxy Read: 5436
- Redis: 6379
- RabbitMQ: 5672
- RabbitMQ Management: 15672
- HAProxy Stats: 8404

### 8.5 Docker Compose Commands

**Start Infrastructure:**
```bash
./infrastructure/scripts/platform.sh start full    # All services
./infrastructure/scripts/platform.sh start video   # Video services only
./infrastructure/scripts/platform.sh start admin   # With admin tools
```

**Management Commands:**
```bash
./infrastructure/scripts/platform.sh status           # Check status
./infrastructure/scripts/platform.sh logs            # View logs
./infrastructure/scripts/platform.sh replication-status  # Check replication
./infrastructure/scripts/platform.sh stop            # Stop services
./infrastructure/scripts/platform.sh clean           # Remove everything
```

### 8.6 Environment Configuration

**Development Mode:**
- Direct database connections
- Default credentials
- Local file storage
- Comprehensive logging

**Production Mode:**
- HAProxy load balancing
- Read replica utilization
- Environment-based configuration
- Optimized caching

---

## 9. Security Architecture

### 9.1 Authentication Flow

**OAuth2 PKCE Flow:**
1. Frontend generates code verifier and challenge
2. User redirects to authorization server
3. After consent, authorization code returned
4. Frontend exchanges code + verifier for tokens
5. Access token (5 min TTL) + Refresh token (7 days TTL)
6. Automatic token refresh every 4.5 minutes

### 9.2 Service-to-Service Security

**Gateway Security:**
- JWT token validation via JWK Set
- X-User-Id header injection
- Path-based access control
- CORS configuration

**Inter-Service Communication:**
- Header-based user context propagation
- Trusted internal network assumption
- Database-level access control

### 9.3 Data Security

**Database Security:**
- Encrypted connections (TLS in production)
- Service-specific database users
- Row-level security via user_id filtering
- Audit trails with created_by/modified_by

**Storage Security:**
- AWS S3 server-side encryption
- CloudFront signed URLs
- Time-limited access (2-hour expiration)
- IAM role-based permissions

### 9.4 Input Validation and Sanitization

**File Upload Security:**
- MIME type validation
- File size limits (2GB maximum)
- Virus scanning capabilities (configurable)
- Secure filename handling

**API Security:**
- Bean Validation annotations
- SQL injection prevention via JPA
- XSS prevention in frontend
- CSRF protection for form-based endpoints

---

## 10. Observability and Monitoring

### 10.1 Logging Strategy

**Logging Framework:**
- SLF4J with Logback across all services
- Structured logging with correlation IDs
- Different log levels per environment
- Centralized log aggregation ready

**Log Patterns:**
```
Console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"
File: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
```

### 10.2 Health Checks

**Service Health Endpoints:**
- **Spring Actuator**: `/actuator/health` for all services
- **Custom Health**: Service-specific health checks
- **Database**: Connection pool monitoring
- **External Services**: S3, Redis, RabbitMQ connectivity

**Infrastructure Health:**
- **HAProxy**: Statistics dashboard at :8404/stats
- **PostgreSQL**: Replication lag monitoring
- **RabbitMQ**: Queue depth and consumer monitoring

### 10.3 Metrics and Monitoring

**Application Metrics (via Micrometer):**
- HTTP request metrics (rate, duration, errors)
- Database connection pool metrics
- Cache hit/miss ratios
- Custom business metrics

**Infrastructure Metrics:**
- PostgreSQL replication lag
- Redis memory usage
- RabbitMQ message throughput
- S3 operation metrics

**Alerting (Configurable):**
- Service health failures
- Database replication lag
- High error rates
- Resource utilization thresholds

### 10.4 Performance Monitoring

**Video-Specific Metrics:**
- Upload success/failure rates
- Encoding job completion times
- Streaming session quality
- CDN cache hit rates

**User Experience Metrics:**
- Page load times
- Video start time
- Buffering events
- Quality switching frequency

---

## 11. Development Workflow

### 11.1 Local Development Setup

**Prerequisites:**
- Java 21+
- Node.js 16+ (for frontend)
- Docker and Docker Compose
- AWS CLI (for S3 access)
- FFmpeg (for encoding service)

**Quick Start:**
```bash
# 1. Start infrastructure
cd infrastructure
./scripts/platform.sh start full

# 2. Start services (in separate terminals)
cd upload && ./gradlew bootRun
cd metadata && ./gradlew bootRun
cd encoding && ./gradlew bootRun
cd streaming && ./gradlew bootRun
cd gateway && ./gradlew bootRun
cd authentication && ./gradlew bootRun

# 3. Start frontend
cd frontend && npm start
```

### 11.2 Testing Strategy

**Unit Testing:**
- JUnit 5 for Java services
- Mockito for mocking dependencies
- Jest for React components

**Integration Testing:**
- TestContainers for database testing
- Embedded Redis for cache testing
- MockMvc for API testing

**End-to-End Testing:**
- Service integration tests
- File upload and processing tests
- Authentication flow tests

### 11.3 Build and Deployment

**Java Services:**
```bash
./gradlew build        # Build service
./gradlew test         # Run tests
./gradlew bootJar      # Create executable JAR
```

**Frontend:**
```bash
npm install           # Install dependencies
npm test             # Run tests
npm run build        # Create production build
```

### 11.4 Database Migration Management

**Liquibase Commands:**
```bash
./gradlew update              # Apply migrations
./gradlew rollback           # Rollback changes
./gradlew generateChangelog  # Generate changelog
```

**Context Separation:**
- Upload Service: `upload-service` context
- Metadata Service: `metadata-service` context
- Streaming Service: `streaming-service` context
- Encoding Service: `encoding-service` context

---

## 12. References and Documentation

### 12.1 Service-Specific Documentation

**Core Services:**
- [Infrastructure Setup](infrastructure/README.md) - Shared infrastructure management
- [RabbitMQ Message Broker](infrastructure/RABBITMQ.md) - Asynchronous communication
- [Upload Service](upload/README.md) - Video upload and multipart upload features
- [Metadata Service](metadata/README.md) - Video metadata and search functionality
- [Streaming Service](streaming/README.md) - Video delivery and session management
- [Encoding Service](encoding/README.md) - Multi-quality transcoding
- [Authentication Service](authentication/README.md) - OAuth2 and user management
- [Gateway Service](gateway/README.md) - API gateway and routing

### 12.2 Technical Documentation

**Detailed Technical Specs:**
- [Gateway Technical Docs](gateway/documentation_gateway.md)
- [Authentication Technical Docs](authentication/documentation_authentication.md)
- [Upload Service Technical Docs](upload/documentation_upload-service.md)
- [Encoding Service Technical Docs](encoding/documentation_encoding-service.md)
- [Streaming Service Technical Docs](streaming/documentation_streaming-service.md)
- [Metadata Service Technical Docs](metadata/documentation_metadata.md)
- [Infrastructure Technical Docs](infrastructure/documentation_infrastructure.md)
- [Frontend Technical Docs](documentation_video-hosting-frontend.md)

### 12.3 Database Documentation

- [Complete Database Schema](database_schema.md) - All tables, relationships, and indexes
- [Liquibase Changelogs](*/src/main/resources/db/changelog/) - Migration history

### 12.4 Architecture Documentation

- [Main README](README.md) - Project overview and quick start guide
- [Port Configuration](README.md#port-configuration) - Complete port allocation
- [Recent Fixes](README.md#recent-fixes) - Latest system updates

### 12.5 Development Resources

**Setup Guides:**
- Local development environment setup
- Docker Compose profile usage
- Database replication configuration
- AWS S3 and CloudFront setup

**API Documentation:**
- Swagger/OpenAPI specs (available at `/swagger-ui.html` for each service)
- Postman collections for API testing
- Service endpoint documentation

### 12.6 Operational Guides

**Infrastructure Management:**
- Service startup/shutdown procedures
- Database backup and restore
- Monitoring and alerting setup
- Troubleshooting common issues

**Security Procedures:**
- Token management and rotation
- Access control configuration
- Security incident response
- Data protection compliance

---

## Appendix

### A. Glossary

**Terms and Definitions:**
- **HLS**: HTTP Live Streaming - Apple's streaming protocol
- **DASH**: Dynamic Adaptive Streaming over HTTP
- **PKCE**: Proof Key for Code Exchange - OAuth2 security extension
- **JWT**: JSON Web Token - Token-based authentication
- **CDN**: Content Delivery Network - Global content distribution
- **Multipart Upload**: Chunked file upload for large files
- **Master-Slave**: Database replication pattern
- **Soft Delete**: Logical deletion preserving data for recovery

### B. Common Issues and Solutions

**Database Connection Issues:**
- Check HAProxy status and replication health
- Verify database credentials and connectivity
- Monitor connection pool metrics

**Video Processing Failures:**
- Check FFmpeg installation and PATH
- Verify S3 bucket permissions
- Monitor encoding job queue status

**Authentication Problems:**
- Verify JWT token expiration and refresh
- Check OAuth2 configuration and redirect URIs
- Validate CORS settings

### C. Performance Tuning Guidelines

**Database Optimization:**
- Connection pool sizing
- Query optimization and indexing
- Read replica utilization

**Caching Strategy:**
- Redis cache configuration
- CDN caching policies
- Application-level caching

**Video Streaming Optimization:**
- Quality selection algorithms
- CDN edge location configuration
- Bandwidth adaptive streaming

---

*Document Version: 1.0*  
*Last Updated: Generated from comprehensive platform documentation*  
*Platform Version: Video Hosting Platform v1.0*  
*Authors: Platform Development Team* 
