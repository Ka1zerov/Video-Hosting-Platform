# Upload Service - Technical Documentation

## 1. Overview

### Microservice Name
**Upload Service** - Video file upload and management microservice

### Purpose and Responsibilities
The Upload Service is a critical component of the distributed video hosting platform responsible for:
- Video file upload processing with AWS S3 integration
- File validation and metadata management
- Multipart upload support for large video files (>5MB)
- Database persistence with PostgreSQL
- Message publishing to RabbitMQ for downstream processing
- Automated cleanup of expired upload sessions
- User authentication and authorization via header-based approach

### Key Technologies and Frameworks
- **Java 21** - Runtime environment
- **Spring Boot 3.4.5** - Core framework
- **Spring Data JPA** - Data persistence layer
- **Spring AMQP** - RabbitMQ integration
- **PostgreSQL** - Primary database
- **Redis** - Session storage and caching
- **AWS S3** - Object storage
- **Liquibase** - Database migrations
- **Lombok** - Code generation
- **Jackson** - JSON processing

## 2. API Specification

### Base URL
```
http://localhost:8082/api/upload
```

### Authentication
- **Header-based authentication**: `X-User-Id` header required for all endpoints
- **Authorization**: User ID validation through gateway service

### REST Endpoints

#### Standard Upload Operations

**Upload Video**
```http
POST /api/upload/video
Content-Type: multipart/form-data
Headers: X-User-Id: {userId}

Parameters:
- file: MultipartFile (required) - Video file
- title: String (required) - Video title
- description: String (optional) - Video description

Response: UploadResponse
```

**Get Video**
```http
GET /api/upload/video/{videoId}
Headers: X-User-Id: {userId}

Response: Video entity
```

**Get User Videos**
```http
GET /api/upload/videos
Headers: X-User-Id: {userId}

Response: List<Video>
```

**Delete Video (Soft Delete)**
```http
DELETE /api/upload/video/{videoId}
Headers: X-User-Id: {userId}

Response: Success message
```

**Restore Video**
```http
POST /api/upload/video/{videoId}/restore
Headers: X-User-Id: {userId}

Response: Success message
```

**Permanently Delete Video**
```http
DELETE /api/upload/video/{videoId}/permanent
Headers: X-User-Id: {userId}

Response: Success message
```

#### Multipart Upload Operations

**Initiate Multipart Upload**
```http
POST /api/upload/multipart/initiate
Content-Type: application/json
Headers: X-User-Id: {userId}

Body: MultipartUploadRequest
{
  "title": "string",
  "description": "string",
  "originalFilename": "string",
  "fileSize": number,
  "mimeType": "string",
  "partSize": number
}

Response: MultipartUploadResponse
```

**Upload Chunk**
```http
POST /api/upload/multipart/upload-chunk
Content-Type: multipart/form-data
Headers: X-User-Id: {userId}

Parameters:
- chunk: MultipartFile (required)
- uploadId: String (required)
- partNumber: Integer (required, min: 1)

Response: ChunkUploadResponse
```

**Complete Upload**
```http
POST /api/upload/multipart/complete/{uploadId}
Headers: X-User-Id: {userId}

Response: UploadResponse
```

**Abort Upload**
```http
DELETE /api/upload/multipart/abort/{uploadId}
Headers: X-User-Id: {userId}

Response: Success message
```

**Get Upload Status**
```http
GET /api/upload/multipart/status/{uploadId}
Headers: X-User-Id: {userId}

Response: MultipartUploadSession
```

#### Admin Endpoints

**Get Cleanup Statistics**
```http
GET /api/upload/multipart/admin/cleanup-stats

Response: CleanupStats
```

**Trigger Manual Cleanup**
```http
POST /api/upload/multipart/admin/cleanup

Response: Success message
```

**Cleanup Specific Session**
```http
DELETE /api/upload/multipart/admin/cleanup/{uploadId}

Response: Success message
```

### Request/Response Format

**UploadResponse**
```json
{
  "id": "UUID",
  "title": "string",
  "description": "string",
  "originalFilename": "string",
  "fileSize": number,
  "mimeType": "string",
  "s3Key": "string",
  "error": "string"
}
```

**MultipartUploadSession**
```json
{
  "uploadId": "string",
  "s3Key": "string",
  "userId": "string",
  "title": "string",
  "totalParts": number,
  "uploadedParts": {},
  "progressPercentage": number,
  "createdAt": "ISO-8601",
  "expiresAt": "ISO-8601"
}
```

## 3. Architectural Patterns

### Design Patterns Implemented

**1. Hexagonal Architecture (Ports and Adapters)**
- Clear separation between domain logic and external dependencies
- Repository pattern for data access
- Service layer for business logic
- Controller layer for API endpoints

**2. Template Method Pattern**
- `BaseVideoService` abstract class with common validation logic
- Concrete implementations: `VideoUploadService`, `MultipartUploadService`

**3. Strategy Pattern**
- Database routing strategy for read/write operations
- Message publishing strategies (safe vs. strict)

**4. Factory Pattern**
- S3 key generation for unique file identification
- Configuration factories for different environments

**5. Observer Pattern**
- Event-driven architecture with RabbitMQ message publishing
- Scheduled cleanup services

### Data Access Patterns

**Repository Pattern**
- JPA repositories with custom query methods
- Soft delete support with `@Query` annotations

**Transaction Management**
- `@Transactional` for data consistency
- Transaction-safe message publishing pattern

**Audit Pattern**
- Automatic audit fields (createdAt, modifiedAt, createdBy, modifiedBy)
- JPA Entity Listeners for automatic population

## 4. Communication Protocols

### Internal Communication

**Database Communication**
- **Protocol**: JDBC over TCP
- **Driver**: PostgreSQL JDBC Driver
- **Connection Pooling**: HikariCP (default with Spring Boot)
- **Features**: Read/write splitting, connection pooling

**Cache Communication**
- **Protocol**: Redis Protocol (RESP)
- **Library**: Spring Data Redis with Lettuce
- **Usage**: Session storage, caching

**Message Queue Communication**
- **Protocol**: AMQP 0.9.1
- **Library**: Spring AMQP
- **Exchange**: `video.exchange`
- **Routing Key**: `video.encoding`
- **Queue**: `video.encoding.queue`

### External Communication

**AWS S3 Integration**
- **Protocol**: HTTPS/REST API
- **SDK**: AWS Java SDK v1.12.605
- **Operations**: PUT, GET, DELETE, Multipart Upload
- **Authentication**: AWS Access Key/Secret Key

**HTTP API**
- **Protocol**: HTTP/1.1
- **Format**: JSON for request/response bodies
- **Multipart**: RFC 7578 for file uploads

## 5. IETF RFC References

The service adheres to the following Internet standards:

- **RFC 7230-7237**: HTTP/1.1 Message Syntax and Routing
- **RFC 7578**: Returning Values from Forms: multipart/form-data
- **RFC 3986**: Uniform Resource Identifier (URI): Generic Syntax
- **RFC 7519**: JSON Web Token (JWT) - for future token-based authentication
- **RFC 5321**: Simple Mail Transfer Protocol (SMTP) - for notification services
- **RFC 0793**: Transmission Control Protocol (TCP)
- **RFC 7159**: JavaScript Object Notation (JSON) Data Interchange Format

## 6. Configuration & Environment

### Environment Variables

**Database Configuration**
```yaml
DB_USERNAME: upload_user
DB_PASSWORD: upload_pass
```

**Redis Configuration**
```yaml
REDIS_HOST: localhost
REDIS_PORT: 6379
REDIS_PASSWORD: ""
```

**RabbitMQ Configuration**
```yaml
RABBITMQ_HOST: localhost
RABBITMQ_PORT: 5672
RABBITMQ_USERNAME: guest
RABBITMQ_PASSWORD: guest
RABBITMQ_VHOST: /
RABBITMQ_EXCHANGE: video.exchange
RABBITMQ_QUEUE_ENCODING: video.encoding.queue
RABBITMQ_ROUTING_KEY_ENCODING: video.encoding
```

**AWS S3 Configuration**
```yaml
AWS_ACCESS_KEY: your-access-key
AWS_SECRET_KEY: your-secret-key
AWS_REGION: eu-north-1
S3_BUCKET_NAME: video-hosting-thesis
S3_BUCKET_PREFIX: videos/
```

**Multipart Upload Configuration**
```yaml
MULTIPART_CLEANUP_ENABLED: true
MULTIPART_CLEANUP_MAX_AGE_HOURS: 24
```

### Configuration Files

**Primary Configuration**: `application.yml`
- Server port: 8082
- Database connection settings
- Redis configuration
- File upload limits (2GB max)
- Logging configuration

**Replica Configuration**: `application-replica.yml`
- Master-slave database setup
- Read/write splitting configuration

**Database Migrations**: `db/changelog/db.changelog-master.yaml`
- Liquibase change sets
- Schema versioning

### Service Discovery
- **Method**: Configuration-based service discovery
- **Integration**: Through API Gateway (header-based routing)

## 7. Observability

### Logging Practices

**Logging Framework**: SLF4J with Logback
**Log Levels**: INFO, WARN, ERROR, DEBUG
**Log Format**: 
```
%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n
```

**Key Log Events**:
- Video upload start/completion
- Multipart upload session lifecycle
- S3 operations (upload, delete)
- Message publishing events
- Cleanup operations
- Error conditions and exceptions

**Log Files**:
- Console output for development
- File output: `logs/upload-service.log`

### Monitoring Integrations

**Spring Boot Actuator**
- Health checks: `/actuator/health`
- Metrics: `/actuator/metrics`
- Info endpoint: `/actuator/info`

**Prometheus Ready**
- Micrometer metrics integration
- Custom metrics for upload operations
- JVM and system metrics

**Potential Grafana Dashboards**:
- Upload success/failure rates
- File size distribution
- S3 operation metrics
- Database connection pool status
- Redis cache hit/miss ratios

### Health Checks

**Application Health**
```http
GET /api/upload/health
GET /api/upload/multipart/health
GET /actuator/health
```

**Component Health Checks**:
- Database connectivity
- Redis connectivity
- S3 bucket accessibility
- RabbitMQ connection status

## 8. Security

### Security Mechanisms

**Authentication**
- Header-based authentication via `X-User-Id`
- Integration with upstream API Gateway
- User context propagation through request headers

**Authorization**
- User-based resource isolation
- Videos accessible only by their owners
- Admin endpoints for system operations

**Data Protection**
- SQL injection prevention via JPA/Hibernate
- Input validation using Bean Validation
- File type validation and size limits

**Secure Storage**
- AWS S3 server-side encryption
- Database encryption at rest (PostgreSQL)
- Redis password protection

**Network Security**
- HTTPS for external S3 communication
- Encrypted database connections
- Secure RabbitMQ connections

### Security Best Practices

**Input Validation**
- File type whitelist (video formats only)
- File size limits (2GB maximum)
- Filename sanitization
- Request parameter validation

**Error Handling**
- Generic error messages to prevent information disclosure
- Structured exception handling
- Audit logging of security events

**Resource Protection**
- Soft delete for data recovery
- Automatic cleanup of expired sessions
- Rate limiting through gateway

### Notable Security Risks

**Identified Risks**:
1. **Header Spoofing**: `X-User-Id` header can be manipulated if not properly validated by gateway
2. **Storage Costs**: Potential abuse of large file uploads
3. **Resource Exhaustion**: Multipart uploads without proper cleanup

**Mitigation Strategies**:
1. Implement proper gateway validation and JWT tokens
2. Implement storage quotas and monitoring
3. Automated cleanup service with configurable timeouts

## 9. AI/ML Usage

**Current Status**: No AI/ML functionality implemented in this microservice.

**Potential Future Integration**:
- Video content analysis for metadata extraction
- Thumbnail generation using computer vision
- Content moderation using ML models
- Video quality assessment
- Automatic transcription services

## 10. Deployment & Runtime

### Containerization

**Docker Support**: Ready for containerization with standard Spring Boot practices

**Dockerfile Structure** (recommended):
```dockerfile
FROM openjdk:21-jre-slim
COPY target/upload-service.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

### Entry Point and Ports

**Main Class**: `com.tskrypko.upload.UploadApplication`
**Port**: 8082
**Health Check**: `/actuator/health`

### Runtime Dependencies

**Required Services**:
- PostgreSQL database (port 5433)
- Redis server (port 6379)
- RabbitMQ (port 5672)
- AWS S3 service

**JVM Requirements**:
- Java 21 or higher
- Minimum 512MB heap memory
- File system write access for logs

### Environment Profiles

**Default Profile**: Single database configuration
**Replica Profile**: Master-slave database setup with read/write splitting

### Startup Sequence

1. Load environment configuration
2. Initialize database connections
3. Run Liquibase migrations
4. Connect to Redis and RabbitMQ
5. Initialize AWS S3 client
6. Start scheduled cleanup services
7. Expose HTTP endpoints

### Resource Requirements

**Memory**:
- Minimum: 512MB
- Recommended: 1GB
- Maximum file processing: 2GB

**Storage**:
- Logs: 100MB rolling files
- Temporary file processing space

**Network**:
- Outbound: AWS S3 API (HTTPS)
- Inbound: HTTP API on port 8082
- Database connections (PostgreSQL, Redis, RabbitMQ)

---

*Document Version: 1.0*   
*Platform Version: Video Hosting Platform v1.0*  
*Authors: Platform Development Team* 
