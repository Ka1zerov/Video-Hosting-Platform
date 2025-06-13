# 🎥 Video Streaming Service

The **Video Streaming Service** is a core microservice of the Video Hosting Platform that handles video playback, streaming, and viewer analytics. It provides secure video access with support for multiple quality streams (HLS/DASH) and CloudFront CDN integration.

## 🎯 Features

### Core Streaming
- **Multi-Quality Streaming**: Support for 480p, 720p, 1080p quality streams
- **Adaptive Streaming**: HLS and DASH manifest generation
- **Secure Streaming**: JWT-based stream tokens with IP validation
- **CDN Integration**: CloudFront support for global content delivery

### Video Management
- **Video Discovery**: Browse, search, and filter available videos
- **Popular Content**: Most viewed and trending videos
- **User Content**: Personal video libraries
- **Real-time Analytics**: View counts, watch time, completion rates

### Session Management
- **View Tracking**: Real-time session monitoring
- **Heartbeat System**: Active session management
- **Analytics Collection**: Detailed viewing statistics
- **Quality Switching**: Dynamic quality adaptation

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Streaming Service                        │
├─────────────────────────────────────────────────────────────┤
│  Controllers                                                │
│  ┌─────────────────────┐  ┌─────────────────────────────┐   │
│  │ VideoStreamController│  │  ViewSessionController     │   │
│  │ - Stream playback   │  │  - Session management      │   │
│  │ - Video discovery   │  │  - Analytics tracking      │   │
│  └─────────────────────┘  └─────────────────────────────┘   │
├─────────────────────────────────────────────────────────────┤
│  Services                                                   │
│  ┌──────────────────┐ ┌─────────────┐ ┌─────────────────┐  │
│  │VideoStreamingServ│ │CloudFrontSer│ │ViewSessionServ  │  │
│  │- Video access    │ │- CDN URLs   │ │- Session mgmt   │  │
│  │- Quality mgmt    │ │- Cache ctrl │ │- Analytics      │  │
│  └──────────────────┘ └─────────────┘ └─────────────────┘  │
│  ┌─────────────────────────────────────────────────────┐   │
│  │              StreamTokenService                     │   │
│  │              - JWT tokens                           │   │
│  │              - Security validation                  │   │
│  └─────────────────────────────────────────────────────┘   │
├─────────────────────────────────────────────────────────────┤
│  Data Layer                                                 │
│  ┌────────────┐ ┌──────────────┐ ┌──────────────────────┐  │
│  │   Video    │ │VideoQuality  │ │    ViewSession       │  │
│  │Repository  │ │Repository    │ │    Repository        │  │
│  └────────────┘ └──────────────┘ └──────────────────────┘  │
├─────────────────────────────────────────────────────────────┤
│  Storage & Cache                                            │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────────────┐   │
│  │ PostgreSQL  │ │    Redis    │ │      AWS S3         │   │
│  │ Video meta  │ │ Stream      │ │   Video files       │   │
│  │ Sessions    │ │ tokens      │ │   HLS/DASH          │   │
│  │ Analytics   │ │ Cache       │ │   Thumbnails        │   │
│  └─────────────┘ └─────────────┘ └─────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

## 🛠️ Technology Stack

- **Framework**: Spring Boot 3.4.5 + Java 21
- **Database**: PostgreSQL (shared with other services)
- **Cache**: Redis (session storage, token cache)
- **Storage**: AWS S3 (video files, manifests)
- **CDN**: Amazon CloudFront
- **Security**: JWT tokens, IP validation
- **Streaming**: HLS/DASH protocols

## 📊 Database Schema

### Video Qualities Table
```sql
CREATE TABLE video_qualities (
    id BIGINT PRIMARY KEY,
    video_id BIGINT NOT NULL,
    quality_name VARCHAR(20) NOT NULL,
    width INTEGER NOT NULL,
    height INTEGER NOT NULL,
    bitrate INTEGER NOT NULL,
    file_size BIGINT,
    s3_key VARCHAR(500),
    hls_playlist_url VARCHAR(500),
    encoding_status VARCHAR(20) DEFAULT 'PENDING',
    encoding_progress INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### View Sessions Table
```sql
CREATE TABLE view_sessions (
    id BIGINT PRIMARY KEY,
    video_id BIGINT NOT NULL,
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

## 🔌 API Endpoints

### Video Streaming
```http
# Get stream info for video playback
POST /api/streaming/play
Content-Type: application/json
{
    "videoId": 123,
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

### Session Management
```http
# Start viewing session
POST /api/streaming/sessions/start?videoId=123&userId=user1

# Send heartbeat (progress update)
POST /api/streaming/sessions/heartbeat
Content-Type: application/json
{
    "videoId": 123,
    "sessionId": "session-uuid",
    "currentPosition": 120,
    "watchDuration": 180,
    "quality": "720p"
}

# End viewing session
POST /api/streaming/sessions/end?sessionId=session-uuid&isComplete=true

# Get session analytics
GET /api/streaming/sessions/analytics/{videoId}
```

## 🔐 Security

### Stream Tokens
- **JWT-based**: Secure video access tokens
- **IP Validation**: Optional IP address verification
- **Expiration**: 1-hour token lifetime (configurable)
- **Blacklisting**: Token revocation support

### Access Control
```java
// Token generation
String token = streamTokenService.generateStreamToken(
    videoId, sessionId, ipAddress
);

// Token validation
boolean isValid = streamTokenService.validateStreamToken(
    token, videoId, ipAddress
);
```

## 🌐 CloudFront CDN Integration

### Configuration
```yaml
aws:
  cloudfront:
    domain: ${CLOUDFRONT_DOMAIN:}
    enabled: ${CLOUDFRONT_ENABLED:false}

streaming:
  cdn:
    enabled: ${CDN_ENABLED:false}
    cache-control: "public, max-age=31536000"
    manifest-cache-control: "public, max-age=60"
```

### URL Conversion
```java
// Automatic S3 to CloudFront URL conversion
String s3Url = "https://bucket.s3.amazonaws.com/videos/file.m3u8";
String cdnUrl = cloudFrontService.getCdnUrl(s3Url);
// Result: "https://d123456789.cloudfront.net/videos/file.m3u8"
```

### Cache Behaviors
- **Video Segments**: Long cache (1 year)
- **Manifests**: Short cache (1 minute)
- **Thumbnails**: Medium cache (1 day)

## 📈 Analytics & Monitoring

### Real-time Metrics
- **Active Sessions**: Current viewers count
- **View Counts**: Total and unique viewers
- **Watch Time**: Total and average watch duration
- **Completion Rate**: Percentage of completed views
- **Quality Distribution**: Popular quality preferences

### Session Tracking
```java
// Start tracking
ViewSession session = viewSessionService.startViewSession(
    videoId, userId, ipAddress, userAgent
);

// Update progress
viewSessionService.updateViewSession(heartbeatRequest);

// Get analytics
VideoAnalytics analytics = viewSessionService.getVideoAnalytics(videoId);
```

## 🚀 Quick Start

### 1. Prerequisites
- Java 21+
- Docker & Docker Compose
- Running infrastructure (PostgreSQL, Redis, RabbitMQ)

### 2. Configuration
```bash
# Set environment variables
export DB_USERNAME=streaming_user
export DB_PASSWORD=streaming_pass
export AWS_ACCESS_KEY=your-access-key
export AWS_SECRET_KEY=your-secret-key
export CLOUDFRONT_DOMAIN=d123456789.cloudfront.net
export CLOUDFRONT_ENABLED=true
```

### 3. Run Service
```bash
# Start infrastructure first
cd ../infrastructure
./scripts/platform.sh start video

# Run streaming service
cd ../streaming
./gradlew bootRun
```

### 4. Test Endpoints
```bash
# Health check
curl http://localhost:8084/api/streaming/health

# Get available videos
curl http://localhost:8084/api/streaming/videos

# Start video playback
curl -X POST http://localhost:8084/api/streaming/play \
  -H "Content-Type: application/json" \
  -d '{"videoId": 1, "sessionId": "test-session"}'
```

## 🔧 Configuration

### Application Properties
```yaml
# Streaming settings
streaming:
  formats:
    hls:
      enabled: true
      segment-duration: 10
    dash:
      enabled: true
    mp4:
      enabled: true
  
  qualities:
    - name: "480p"
      width: 854
      height: 480
      bitrate: 1000
    - name: "720p"
      width: 1280
      height: 720
      bitrate: 2500
    - name: "1080p"
      width: 1920
      height: 1080
      bitrate: 5000
  
  security:
    token-expiry: 3600
    rate-limit:
      requests-per-minute: 60
```

## 📊 Performance Optimizations

### Database Indexes
- **Video Lookups**: `(status, created_at)`, `(status, views_count)`
- **Session Queries**: `(video_id)`, `(user_id)`, `(session_id)`
- **Analytics**: `(video_id, started_at)`, `(ip_address)`

### Caching Strategy
- **Video Metadata**: 30 minutes TTL
- **Stream Tokens**: 1 hour TTL
- **Analytics**: 15 minutes TTL
- **Session Data**: 24 hours TTL

### CDN Optimization
- **Segment Files**: `Cache-Control: public, max-age=31536000`
- **Manifests**: `Cache-Control: public, max-age=60`
- **Geographic Distribution**: Global edge locations

## 🐛 Troubleshooting

### Common Issues
1. **Token Validation Fails**
   - Check JWT secret configuration
   - Verify token expiration
   - Confirm IP address matching

2. **CDN URLs Not Working**
   - Verify CloudFront domain configuration
   - Check AWS credentials
   - Ensure S3 bucket permissions

3. **Session Tracking Issues**
   - Check Redis connectivity
   - Verify session ID uniqueness
   - Monitor heartbeat intervals

### Debug Commands
```bash
# Check service health
curl http://localhost:8084/actuator/health

# View active sessions
curl http://localhost:8084/api/streaming/sessions/active

# Check Redis keys
redis-cli keys "stream_*"

# Database session query
psql -h localhost -p 5433 -U streaming_user -d video_platform \
  -c "SELECT * FROM view_sessions WHERE ended_at IS NULL;"
```

## 🤝 Integration

### With Other Services
- **Upload Service**: Video metadata synchronization
- **Encoding Service**: Quality stream availability
- **Metadata Service**: Search and discovery
- **Gateway**: Authentication and routing

### Event Communication (RabbitMQ)
```yaml
# Listen for encoding completion
rabbitmq:
  queue:
    streaming: video.streaming.queue
  routing:
    key:
      streaming: video.streaming
```

## 📝 Development

### Adding New Quality
1. Update configuration
2. Add database migration
3. Update encoding service
4. Test stream generation

### Custom Analytics
1. Extend `ViewSession` model
2. Add repository methods
3. Update analytics service
4. Create API endpoints

---

**Note**: This service is designed for educational purposes as part of a Master's thesis. For production deployment, additional security, monitoring, and scalability considerations would be required. 