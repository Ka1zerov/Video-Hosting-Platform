# Technical Documentation: Video Encoding Service

## 1. Overview

### Microservice Name
**Video Encoding Service** (`encoding-service`)

### Purpose and Responsibilities
The Video Encoding Service is a core microservice within the distributed video hosting platform responsible for:

- **Multi-bitrate Video Transcoding**: Converting uploaded videos into multiple quality levels (1080p, 720p, 480p)
- **HLS Streaming Format**: Generating HTTP Live Streaming (HLS) playlists and segments for adaptive streaming
- **Thumbnail Generation**: Creating thumbnails for each quality level
- **Asynchronous Processing**: Processing video encoding jobs through message queues
- **Progress Tracking**: Real-time monitoring of encoding progress
- **Error Handling**: Comprehensive error handling with retry mechanisms

### Key Technologies and Frameworks

| Technology | Version | Purpose |
|------------|---------|---------|
| **Java** | 21 | Primary programming language |
| **Spring Boot** | 3.4.5 | Application framework |
| **Spring Data JPA** | 3.4.5 | Database abstraction layer |
| **Spring AMQP** | 3.4.5 | RabbitMQ integration |
| **PostgreSQL** | 42.7.3 | Primary database |
| **Redis** | - | Caching and session management |
| **RabbitMQ** | - | Message broker for async processing |
| **FFmpeg** | 0.8.0 (wrapper) | Video transcoding engine |
| **AWS S3** | 1.12.605 | Object storage |
| **Liquibase** | - | Database migrations |
| **Lombok** | - | Boilerplate code reduction |

## 2. API Specification

### Base URL
`http://localhost:8085/api/encoding`

### Endpoints

#### Job Management

| Endpoint | Method | Description | Request/Response |
|----------|--------|-------------|------------------|
| `/job/{jobId}` | GET | Get encoding job details | **Response**: `EncodingJob` object |
| `/job/video/{videoId}` | GET | Get job by video ID | **Response**: `EncodingJob` object |
| `/jobs` | GET | List jobs with optional filters | **Query Params**: `status`, `userId`<br>**Response**: `List<EncodingJob>` |
| `/job/{jobId}/retry` | POST | Retry failed encoding job | **Response**: Success/Error message |
| `/job/{jobId}` | DELETE | Cancel pending job | **Response**: Success/Error message |

#### Monitoring

| Endpoint | Method | Description | Response |
|----------|--------|-------------|----------|
| `/stats` | GET | Get encoding statistics | Encoding stats object |
| `/health` | GET | Health check endpoint | "Encoding Service is running" |

### Data Models

#### EncodingJob
```json
{
  "id": "uuid",
  "videoId": "uuid",
  "userId": "string",
  "title": "string",
  "originalFilename": "string",
  "fileSize": "long",
  "mimeType": "string",
  "s3Key": "string",
  "status": "PENDING|PROCESSING|COMPLETED|FAILED",
  "startedAt": "datetime",
  "completedAt": "datetime",
  "errorMessage": "string",
  "retryCount": "integer",
  "progress": "integer (0-100)"
}
```

#### EncodingStatus Enum
- `PENDING`: Job created, waiting to be processed
- `PROCESSING`: Currently being encoded
- `COMPLETED`: Successfully completed
- `FAILED`: Failed with error

### Authentication/Authorization
- **CORS**: Enabled for all origins (`@CrossOrigin(origins = "*")`)
- No explicit authentication mechanism implemented in current version

## 3. Architectural Patterns

### Event-Driven Architecture
- **Message-Driven Processing**: Uses RabbitMQ for asynchronous job processing
- **Event Sourcing**: Job lifecycle tracked through status changes
- **Producer-Consumer Pattern**: Listens to video upload events and processes them

### Domain-Driven Design (DDD)
- **Service Layer**: Clear separation of business logic (`VideoEncodingService`)
- **Repository Pattern**: Data access abstraction (`EncodingJobRepository`)
- **Entity Models**: Rich domain models (`EncodingJob`, `Video`)

### Microservice Patterns
- **Database per Service**: Dedicated PostgreSQL database
- **Asynchronous Processing**: Non-blocking video encoding
- **Circuit Breaker**: Error handling with retry mechanisms
- **Configuration Externalization**: Environment-based configuration

## 4. Communication Protocols

### Internal Communication
- **RabbitMQ AMQP**: Primary message broker
  - **Exchange**: `video.exchange` (Topic Exchange)
  - **Queue**: `video.encoding.queue`
  - **Routing Key**: `video.encoding`
  - **Message Format**: JSON

### External Communication
- **HTTP/REST**: API endpoints (Port 8085)
- **AWS S3 API**: File storage operations
- **Redis Protocol**: Caching operations

### Message Flow
```
Upload Service → RabbitMQ → Encoding Service → S3 Storage
                    ↓
            PostgreSQL (Job Tracking)
```

## 5. IETF RFC References

| RFC | Standard | Usage |
|-----|----------|-------|
| RFC 2616 | HTTP/1.1 | REST API communication |
| RFC 7540 | HTTP/2 | Modern HTTP protocol support |
| RFC 8216 | HTTP Live Streaming (HLS) | Video streaming format |
| RFC 0821 | SMTP | Potentially for error notifications |
| RFC 5321 | AMQP | RabbitMQ message protocol |

## 6. Configuration & Environment

### Environment Variables

#### Database Configuration
```bash
DB_USERNAME=encoding_user
DB_PASSWORD=encoding_pass
```

#### AWS S3 Configuration
```bash
AWS_ACCESS_KEY=your-access-key
AWS_SECRET_KEY=your-secret-key
AWS_REGION=eu-north-1
S3_BUCKET_NAME=video-hosting-thesis
```

#### RabbitMQ Configuration
```bash
RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=guest
RABBITMQ_PASSWORD=guest
RABBITMQ_VHOST=/
```

#### FFmpeg Configuration
```bash
# macOS (Homebrew)
FFMPEG_PATH=/opt/homebrew/bin/ffmpeg
FFPROBE_PATH=/opt/homebrew/bin/ffprobe

# Linux
FFMPEG_PATH=/usr/bin/ffmpeg
FFPROBE_PATH=/usr/bin/ffprobe
```

#### Encoding Configuration
```bash
ENCODING_TEMP_DIR=/tmp/encoding
HLS_SEGMENT_DURATION=10
```

### Key Configuration Files
- `application.yml`: Main application configuration
- `application-replica.yml`: Replica-specific configuration
- `db/changelog/`: Liquibase database migration scripts

### Service Discovery
- **Static Configuration**: Environment variable-based service endpoints
- **Health Checks**: Spring Actuator endpoints (`/actuator/health`)

## 7. Observability

### Logging Practices
- **Structured Logging**: SLF4J with Logback
- **Log Levels**: INFO, WARN, ERROR appropriately used
- **Contextual Logging**: Job IDs, Video IDs included in log messages
- **Log File**: `logs/encoding-service.log`

#### Log Format
```
Console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"
File: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
```

### Monitoring Integrations
- **Spring Actuator**: Health checks and metrics
  - `/actuator/health`: Service health status
  - `/actuator/info`: Application information
  - `/actuator/metrics`: Application metrics
- **Custom Metrics**: Encoding statistics endpoint (`/api/encoding/stats`)

### Tracing
- **Transaction Management**: Database transaction boundaries clearly defined
- **Progress Tracking**: Real-time encoding progress updates
- **Error Tracking**: Comprehensive exception logging

## 8. Security

### Security Mechanisms
- **Database Connection Security**: PostgreSQL with username/password authentication
- **AWS IAM**: S3 access through AWS access keys
- **Environment Variable Protection**: Sensitive data externalized
- **CORS Configuration**: Currently allows all origins (development setup)

### Security Best Practices Followed
- **Separation of Concerns**: Database credentials separate from application code
- **Principle of Least Privilege**: Service-specific database user
- **Input Validation**: Bean validation annotations used
- **SQL Injection Prevention**: JPA/Hibernate query parameterization

### Notable Security Considerations
- **CORS Policy**: Currently permissive (`origins = "*"`) - should be restricted in production
- **Authentication**: No authentication mechanism currently implemented
- **Authorization**: No role-based access control
- **Data Encryption**: Relies on transport layer security

## 9. AI/ML Usage

**No AI/ML functionality is currently implemented** in this microservice. The video encoding is handled through traditional FFmpeg-based transcoding without machine learning enhancement.

### Potential AI/ML Integration Points
- **Content Analysis**: Video content classification
- **Quality Optimization**: AI-driven encoding parameter optimization
- **Thumbnail Selection**: Smart thumbnail generation using computer vision
- **Audio Enhancement**: ML-based audio processing

## 10. Deployment & Runtime

### Containerization
While no explicit Dockerfile is present in the analyzed code, the service is designed for containerized deployment:

#### Recommended Dockerfile Structure
```dockerfile
FROM openjdk:21-jdk-slim
RUN apt-get update && apt-get install -y ffmpeg
COPY build/libs/*.jar app.jar
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

### Entry Point
- **Main Class**: `com.tskrypko.encoding.EncodingApplication`
- **Server Port**: 8085
- **Spring Boot Starter**: Web, JPA, AMQP, Actuator, Data Redis

### Exposed Ports
- **8085**: HTTP REST API
- **Database**: PostgreSQL on port 5433 (external)
- **RabbitMQ**: Port 5672 (external)
- **Redis**: Port 6379 (external)

### Runtime Dependencies
- **FFmpeg**: System-level installation required
- **PostgreSQL Database**: `video_platform` database
- **RabbitMQ Server**: Message broker
- **Redis Server**: Caching layer
- **AWS S3**: Object storage access

### Storage Requirements
- **Temporary Storage**: `/tmp/encoding` directory for processing
- **Log Storage**: `logs/` directory for application logs
- **Database Storage**: PostgreSQL data persistence

### Scalability Considerations
- **Horizontal Scaling**: Stateless design allows multiple instances
- **Load Distribution**: RabbitMQ distributes jobs across instances
- **Resource Requirements**: CPU-intensive due to video encoding
- **Storage Cleanup**: Configurable temporary file cleanup

### Environment Profiles
- **Default Profile**: Development configuration
- **Replica Profile**: Additional configuration for read replicas

---

**Generated on**: $(date)  
**Service Version**: 0.0.1-SNAPSHOT  
**Documentation Version**: 1.0 