# 🎬 Video Streaming Service

A microservice responsible for video streaming, quality management, and viewing analytics in the video hosting platform.

## 🚀 Features

### Core Streaming
- **Video Playback**: HLS and DASH streaming support
- **Quality Selection**: Static quality levels (480p, 720p, 1080p) for MVP
- **CDN Integration**: CloudFront support for global content delivery
- **Session Management**: User viewing sessions and analytics
- **Access Control**: User-based video access validation

### Analytics & Monitoring
- **View Tracking**: Real-time view count and session monitoring
- **Watch Analytics**: Duration, completion rates, and user engagement
- **Quality Metrics**: Streaming performance and quality statistics
- **Health Monitoring**: Service health checks and metrics

### MVP Simplifications
- **Static Quality Enum**: Predefined quality levels instead of dynamic database records
- **Simplified Processing**: Direct video status updates without complex quality tracking
- **Fast Response**: Immediate quality availability for all videos

## 🏗️ Architecture

### Service Components
```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Video Stream  │    │   Session Mgmt  │    │   Analytics     │
│   Controller    │    │   Service       │    │   Service       │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         └───────────────────────┼───────────────────────┘
                                 │
         ┌─────────────────────────────────────────────────┐
         │            Video Streaming Service              │
         └─────────────────────────────────────────────────┘
                                 │
         ┌─────────────────────────────────────────────────┐
         │              PostgreSQL Database                │
         │  ┌─────────────┐    ┌─────────────────────────┐ │
         │  │   videos    │    │    view_sessions        │ │
         │  └─────────────┘    └─────────────────────────┘ │
         └─────────────────────────────────────────────────┘
```

### Database Schema

### Videos Table
```sql
CREATE TABLE videos (
    id UUID PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    original_filename VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    mime_type VARCHAR(100),
    s3_key VARCHAR(500),
    status VARCHAR(20) DEFAULT 'UPLOADED',
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    user_id VARCHAR(100) NOT NULL,
    duration BIGINT,
    views_count BIGINT DEFAULT 0,
    last_accessed TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modified_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL,
    modified_by VARCHAR(100) NOT NULL,
    deleted_at TIMESTAMP
);
```

**Note**: Thumbnail and manifest URLs are generated dynamically using `VideoUrlService` based on video ID and AWS S3 configuration, eliminating the need to store URLs in the database.

### View Sessions Table
```sql
CREATE TABLE view_sessions (
    id UUID PRIMARY KEY,
    video_id UUID NOT NULL,
    user_id VARCHAR(100),
    session_id VARCHAR(100) UNIQUE NOT NULL,
    ip_address VARCHAR(45),
    user_agent TEXT,
    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_heartbeat TIMESTAMP,
    watch_duration BIGINT DEFAULT 0,
    max_position BIGINT DEFAULT 0,
    quality VARCHAR(20) DEFAULT 'AUTO',
    is_complete BOOLEAN DEFAULT false,
    ended_at TIMESTAMP
);
```

### Video Quality Enum (MVP)
```java
public enum VideoQualityEnum {
    Q_480P("480p", 854, 480, 1000),
    Q_720P("720p", 1280, 720, 2500),
    Q_1080P("1080p", 1920, 1080, 4000);
}
```

## 🔌 API Endpoints

### Video Streaming
```http
# Get stream info for video playback
POST /api/streaming/play
Content-Type: application/json
{
    "videoId": "uuid",
    "preferredQuality": "720p",
    "format": "hls",
    "sessionId": "session-uuid"
}

# Browse available videos
GET /api/streaming/videos?page=0&size=20

# Search videos by title
GET /api/streaming/videos/search?title=example&page=0&size=20

# Get popular videos
GET /api/streaming/videos/popular?page=0&size=20

# Get user's videos
GET /api/streaming/videos/user/{userId}?page=0&size=20

# Get video details
GET /api/streaming/videos/{videoId}
```

### Video Quality (MVP)
```http
# Get available qualities for video
GET /api/streaming/qualities/video/{videoId}

# Get quality statistics
GET /api/streaming/qualities/video/{videoId}/stats

# Get all quality types
GET /api/streaming/qualities/all

# Health check
GET /api/streaming/qualities/health
```

### Session Management
```http
# Start viewing session
POST /api/streaming/sessions/start?videoId={uuid}&userId={userId}

# Send heartbeat (progress update)
POST /api/streaming/sessions/heartbeat
Content-Type: application/json
{
    "sessionId": "session-uuid",
    "currentPosition": 120,
    "watchDuration": 120,
    "quality": "720p"
}

# End viewing session
POST /api/streaming/sessions/end
Content-Type: application/json
{
    "sessionId": "session-uuid",
    "isComplete": true
}

# Get session details
GET /api/streaming/sessions/{sessionId}

# Get video sessions (admin/owner only)
GET /api/streaming/sessions/video/{videoId}?page=0&size=20

# Get user sessions
GET /api/streaming/sessions/user/{userId}?page=0&size=20
```

### Analytics
```http
# Get video analytics
GET /api/streaming/sessions/analytics/{videoId}

# Get active sessions (admin only)
GET /api/streaming/sessions/active

# Get total watch time
GET /api/streaming/sessions/watch-time/{videoId}

# Get unique viewers count
GET /api/streaming/sessions/viewers/{videoId}

# Get completion rate
GET /api/streaming/sessions/completion-rate/{videoId}
```

## 🔧 Configuration

### Application Properties
```yaml
server:
  port: 8084

spring:
  application:
    name: streaming-service
  
  datasource:
    url: jdbc:postgresql://localhost:5433/video_platform
    username: ${DB_USERNAME:upload_user}
    password: ${DB_PASSWORD:upload_pass}
  
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}

aws:
  s3:
    bucket:
      name: ${S3_BUCKET_NAME:video-hosting-bucket}
  cloudfront:
    domain: ${CLOUDFRONT_DOMAIN:}
    enabled: ${CLOUDFRONT_ENABLED:false}

streaming:
  session:
    timeout: ${SESSION_TIMEOUT:1800} # 30 minutes
    cleanup:
      interval: ${CLEANUP_INTERVAL:300} # 5 minutes
  
  quality:
    default: ${DEFAULT_QUALITY:720p}
    adaptive: ${ADAPTIVE_STREAMING:true}
```

### Environment Variables
```bash
# Database
DB_USERNAME=streaming_user
DB_PASSWORD=streaming_pass

# AWS
AWS_ACCESS_KEY=your-access-key
AWS_SECRET_KEY=your-secret-key
AWS_REGION=us-east-1
S3_BUCKET_NAME=video-hosting-bucket

# CloudFront (optional)
CLOUDFRONT_DOMAIN=d123456789.cloudfront.net
CLOUDFRONT_ENABLED=true

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=
```

## 🚀 Getting Started

### Prerequisites
- Java 21+
- PostgreSQL 15+
- Redis 7+
- AWS S3 access (for video storage)

### Local Development
```bash
# Clone the repository
git clone <repository-url>
cd streaming

# Set up environment variables
cp .env.example .env
# Edit .env with your configuration

# Run the service
./gradlew bootRun

# Or build and run JAR
./gradlew build
java -jar build/libs/streaming-0.0.1-SNAPSHOT.jar
```

### Docker Deployment
```bash
# Build Docker image
docker build -t streaming-service .

# Run with Docker Compose
docker-compose up -d
```

## 📊 Monitoring & Health

### Health Checks
```http
GET /actuator/health
GET /actuator/health/db
GET /actuator/health/redis
```

### Metrics
```http
GET /actuator/metrics
GET /actuator/metrics/streaming.sessions.active
GET /actuator/metrics/streaming.videos.views
GET /actuator/metrics/streaming.quality.requests
```

## 🧪 Testing

### Unit Tests
```bash
./gradlew test
```

### Integration Tests
```bash
./gradlew integrationTest
```

### API Testing
```bash
# Test video streaming
curl -X POST http://localhost:8084/api/streaming/play \
  -H "Content-Type: application/json" \
  -d '{"videoId":"uuid","preferredQuality":"720p"}'

# Test quality endpoints
curl http://localhost:8084/api/streaming/qualities/all
```

## 📈 Performance

### Optimization Features
- **Connection Pooling**: HikariCP for database connections
- **Redis Caching**: Session and metadata caching
- **CDN Integration**: CloudFront for global content delivery
- **Lazy Loading**: Efficient data fetching strategies
- **Static Quality Enum**: Fast quality resolution without database queries

### Scaling Considerations
- Horizontal scaling with load balancers
- Database read replicas for analytics queries
- Redis clustering for session storage
- CDN edge locations for global reach

## 🔒 Security

### Access Control
- User-based video access validation
- Session-based authentication
- IP-based rate limiting
- CORS configuration for web clients

### Data Protection
- Encrypted database connections
- Secure S3 bucket policies
- CloudFront signed URLs (when enabled)
- Audit logging for all operations

## 📝 API Documentation

Full API documentation is available at:
- Swagger UI: `http://localhost:8084/swagger-ui.html`
- OpenAPI Spec: `http://localhost:8084/v3/api-docs`

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details. 