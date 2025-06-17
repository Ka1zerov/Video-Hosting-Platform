# Technical Documentation: Streaming Service

## 1. Overview

**Microservice name:** streaming-service  
**Purpose:** Video streaming, quality management, session tracking, and viewing analytics within a distributed video hosting platform  
**Version:** 0.0.1-SNAPSHOT  
**Technologies:** Java 21, Spring Boot 3.4.5, PostgreSQL, Redis, AWS S3, CloudFront, Liquibase  

### Key Responsibilities
- Video playback with HLS and DASH streaming protocols
- Multi-quality video streaming (480p, 720p, 1080p)
- User viewing session management and analytics
- CDN integration via AWS CloudFront
- Real-time view tracking and statistics
- Dynamic playlist generation with secure signed URLs

## 2. API Specification

### Video Streaming Endpoints

#### POST /api/streaming/play
Initiates video playback with streaming information
- **Request Body:** `PlaybackRequest`
  ```json
  {
    "videoId": "uuid",
    "preferredQuality": "720p",
    "format": "hls",
    "sessionId": "session-uuid"
  }
  ```
- **Response:** `VideoStreamResponse` with streaming URLs, qualities, and CDN information
- **Authentication:** Optional (supports anonymous viewing)

#### GET /api/streaming/videos
Retrieves paginated list of available videos
- **Parameters:** `page` (default: 0), `size` (default: 20)
- **Response:** Paginated `VideoStreamResponse` list

#### GET /api/streaming/videos/search
Search videos by title
- **Parameters:** `title`, `page`, `size`
- **Response:** Paginated search results

#### GET /api/streaming/videos/popular
Get most viewed videos
- **Parameters:** `page`, `size`
- **Response:** Videos ordered by view count

#### GET /api/streaming/videos/user/{userId}
Get user's videos (authenticated)
- **Path Variable:** `userId`
- **Response:** User's video collection
- **Security:** User can only access their own videos

### Session Management Endpoints

#### POST /api/streaming/sessions/start
Start new viewing session
- **Parameters:** `videoId`, `userId` (optional)
- **Response:** `ViewSession` object with session details

#### POST /api/streaming/sessions/heartbeat
Update session progress (playback position)
- **Request Body:** `ViewSessionRequest`
  ```json
  {
    "sessionId": "session-uuid",
    "currentPosition": 120,
    "watchDuration": 120,
    "quality": "720p"
  }
  ```

#### POST /api/streaming/sessions/end
End viewing session
- **Request Body:** `EndSessionRequest`
  ```json
  {
    "sessionId": "session-uuid",
    "isComplete": true
  }
  ```

### Analytics Endpoints

#### GET /api/streaming/sessions/analytics/{videoId}
Get comprehensive video analytics
- **Response:** `VideoAnalytics` with views, completion rates, watch time

#### GET /api/streaming/sessions/watch-time/{videoId}
Get total watch time for video

#### GET /api/streaming/sessions/viewers/{videoId}
Get unique viewers count

#### GET /api/streaming/sessions/completion-rate/{videoId}
Get video completion rate percentage

### Video Quality Endpoints

#### GET /api/streaming/qualities/video/{videoId}
Get available quality options for video

#### GET /api/streaming/qualities/all
List all supported quality levels

#### GET /api/streaming/qualities/health
Health check for quality service

### Authentication/Authorization
- **Type:** Gateway-based authentication (no JWT within service)
- **User Context:** Retrieved via `CurrentUserService` from request headers
- **Access Control:** User-based video ownership validation
- **Anonymous Access:** Supported for public videos

## 3. Architectural Patterns

### Layered Architecture
- **Controller Layer:** REST endpoints and request handling
- **Service Layer:** Business logic and orchestration
- **Repository Layer:** Data persistence abstraction
- **Model Layer:** Domain entities and DTOs

### Domain-Driven Design Elements
- **Entities:** Video, ViewSession with proper lifecycle management
- **Value Objects:** VideoQualityEnum, VideoStatus
- **Services:** VideoStreamingService, ViewSessionService
- **Repositories:** VideoRepository, ViewSessionRepository

### Event-Driven Architecture (Implicit)
- Session lifecycle events (start, heartbeat, end)
- View count updates
- Analytics aggregation

### Caching Strategy
- **Redis Integration:** Session storage and caching
- **CDN Caching:** CloudFront for video content delivery
- **Spring Caching:** Method-level caching with `@EnableCaching`

## 4. Communication Protocols

### HTTP/REST
- **Primary Protocol:** RESTful HTTP APIs
- **Content Type:** application/json
- **Status Codes:** Standard HTTP status codes
- **CORS Support:** Configurable cross-origin requests

### HTTP Live Streaming (HLS)
- **Protocol:** RFC 8216 compliant HLS
- **Segment Duration:** 10 seconds
- **Playlist Window:** 5 segments
- **Master Playlist:** Dynamic generation with multiple bitrates

### DASH (Dynamic Adaptive Streaming)
- **Protocol:** MPEG-DASH standard
- **Segment Duration:** 10 seconds
- **Manifest:** Dynamic XML generation

### AWS Services Integration
- **S3:** Video storage and retrieval
- **CloudFront:** CDN content delivery
- **SDK:** AWS SDK v2 for CloudFront, legacy v1 for S3

## 5. IETF RFC References

- **RFC 7231:** HTTP/1.1 Semantics and Content (REST API design)
- **RFC 8216:** HTTP Live Streaming (HLS protocol implementation)
- **RFC 7946:** GeoJSON format (for potential geographic analytics)
- **RFC 3986:** URI Generic Syntax (URL construction for video assets)
- **RFC 6265:** HTTP State Management (session cookies)
- **RFC 7617:** Basic HTTP Authentication Scheme
- **RFC 8259:** JSON Data Interchange Format

## 6. Configuration & Environment

### Database Configuration
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5433/video_platform
    username: ${DB_USERNAME:upload_user}
    password: ${DB_PASSWORD:upload_pass}
```

### AWS Configuration
```yaml
aws:
  access:
    key: ${AWS_ACCESS_KEY:your-access-key}
  secret:
    key: ${AWS_SECRET_KEY:your-secret-key}
  region: ${AWS_REGION:eu-north-1}
  s3:
    bucket:
      name: ${S3_BUCKET_NAME:video-hosting-thesis}
  cloudfront:
    domain: ${CLOUDFRONT_DOMAIN:}
    enabled: ${CLOUDFRONT_ENABLED:false}
    signing:
      enabled: ${CLOUDFRONT_SIGNING_ENABLED:true}
      key-pair-id: ${CLOUDFRONT_KEY_PAIR_ID:}
      private-key-path: ${CLOUDFRONT_PRIVATE_KEY_PATH:classpath:keys/cloudfront-private-key.pem}
```

### Redis Configuration
```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
```

### Critical Environment Variables
- `DB_USERNAME`, `DB_PASSWORD`: Database credentials
- `AWS_ACCESS_KEY`, `AWS_SECRET_KEY`: AWS authentication
- `S3_BUCKET_NAME`: Video storage bucket
- `CLOUDFRONT_DOMAIN`: CDN domain
- `REDIS_HOST`, `REDIS_PORT`: Cache configuration

### Service Discovery
- **Port:** 8084
- **Health Check:** `/api/streaming/health`
- **Actuator Endpoints:** `/actuator/health`, `/actuator/metrics`

## 7. Observability

### Logging Framework
- **Framework:** SLF4J with Logback
- **Levels:** INFO (default), DEBUG for CloudFrontService
- **Pattern:** Timestamp, thread, level, logger, message
- **File Output:** `logs/streaming-service.log`

### Key Log Events
- Video stream requests and responses
- Session lifecycle events (start, heartbeat, end)
- Authentication and authorization attempts
- CloudFront URL generation and signing
- Database operations and performance

### Metrics and Monitoring
- **Spring Actuator:** Health checks and application metrics
- **Prometheus Support:** Metrics endpoint for monitoring
- **Custom Metrics:** View counts, session durations, quality selections
- **Error Tracking:** Exception logging with context

### Health Checks
- **Database Connectivity:** PostgreSQL connection validation
- **Redis Connectivity:** Cache availability
- **AWS Services:** S3 bucket access validation
- **Application Health:** Service-specific health indicators

## 8. Security

### Authentication Mechanisms
- **Gateway Authentication:** Authentication handled by API gateway
- **User Context:** User ID extracted from request headers
- **Anonymous Access:** Supported for public video viewing

### Authorization Model
- **Owner-based Access:** Users can only access their own videos
- **Public/Private Videos:** Framework for future public video support
- **Admin Access:** Prepared for administrative operations

### Content Security
- **CloudFront Signed URLs:** Time-limited access to video content
- **URL Expiration:** 2-hour default expiration for signed URLs
- **IP Address Tracking:** Client IP logging for analytics and security

### Rate Limiting
```yaml
streaming:
  security:
    rate-limit:
      requests-per-minute: 60
      requests-per-hour: 1000
```

### Input Validation
- **Bean Validation:** Jakarta Validation annotations
- **Request Validation:** DTO validation with `@Valid`
- **Path Parameter Validation:** UUID format validation

### Notable Security Considerations
- CloudFront private key management
- Session token security
- Database query parameter binding
- CORS configuration management

## 9. AI/ML Usage

**Current Implementation:** No AI/ML functionality present in this microservice.

**Potential Integration Points:**
- Video content analysis for automatic quality optimization
- Viewing pattern analysis for recommendation systems
- Anomaly detection in viewing sessions
- Content-based video categorization

## 10. Deployment & Runtime

### Containerization
```gradle
// Spring Boot plugin enables executable JAR creation
id 'org.springframework.boot' version '3.4.5'
```

### Application Entry Point
```java
@SpringBootApplication
@EnableCaching
@EnableAsync
@EnableTransactionManagement
public class StreamingApplication {
    public static void main(String[] args) {
        SpringApplication.run(StreamingApplication.class, args);
    }
}
```

### Runtime Configuration
- **Java Version:** 21 (Temurin/OpenJDK)
- **Spring Boot:** 3.4.5
- **Server Port:** 8084
- **JPA:** Hibernate with PostgreSQL dialect
- **Connection Pooling:** HikariCP (Spring Boot default)

### Key Dependencies
- **Core:** Spring Boot Web, Data JPA, Validation, Actuator
- **Database:** PostgreSQL 42.7.3, Liquibase
- **Caching:** Spring Data Redis
- **AWS:** SDK v2 (CloudFront, S3), legacy v1 for compatibility
- **Utilities:** Lombok, Jackson, dotenv-java
- **Testing:** TestContainers, Mockito, Awaitility

### Deployment Requirements
- **Memory:** Minimum 512MB, recommended 1GB+
- **CPU:** 1+ cores for concurrent streaming
- **Storage:** Minimal (logs and temporary files)
- **Network:** High bandwidth for video streaming
- **External Dependencies:** PostgreSQL, Redis, AWS services

### Exposed Ports
- **8084:** Main application HTTP port
- **Actuator Endpoints:** Same port, `/actuator/*` paths

### Volume Mounts
- **Logs:** `/app/logs` for persistent logging
- **Keys:** `/app/keys` for CloudFront private keys

### Environment-Specific Profiles
- **Default:** Local development configuration
- **Replica:** Production-like configuration (`application-replica.yml`)

---

**Generated on:** $(date)  
**Service Version:** 0.0.1-SNAPSHOT  
**Documentation Version:** 1.0 