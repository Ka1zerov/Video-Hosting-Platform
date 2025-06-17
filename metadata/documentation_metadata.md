# Technical Documentation: Metadata Microservice

## 1. Overview

### Microservice Name
**Metadata Service** - Video Hosting Platform

### Purpose and Responsibilities
The Metadata Service is responsible for managing extended video metadata, search functionality, analytics, and user interactions within the distributed video hosting platform. It serves as the central hub for video discovery, content organization, and user engagement metrics.

Key responsibilities:
- Video metadata management and retrieval
- Search functionality with full-text capabilities
- Popular and recently watched video recommendations
- Video analytics and view tracking
- Content Discovery and recommendation engine
- CDN URL generation for optimized content delivery

### Key Technologies and Frameworks
- **Java 21** - Latest LTS version with modern language features
- **Spring Boot 3.4.5** - Core application framework
- **Spring Data JPA** - Data persistence layer
- **Spring Web** - REST API implementation
- **Spring Actuator** - Health monitoring and metrics
- **Spring Cache** - Caching abstraction
- **PostgreSQL** - Primary database (shared with Upload Service)
- **Redis** - Caching layer for performance optimization
- **RabbitMQ** - Asynchronous messaging for service integration
- **Liquibase** - Database migration management
- **Lombok** - Code generation for boilerplate reduction
- **Gradle** - Build automation tool

## 2. API Specification

### Base URL
```
http://localhost:8083/api/metadata
```

### Authentication/Authorization
- **Header-based Authentication**: X-User-Id header (forwarded by API Gateway)
- **No direct authentication implementation** - relies on upstream gateway

### REST Endpoints

#### Video Metadata Operations

##### GET `/videos`
**Description**: Retrieve paginated list of all ready videos
**Method**: GET
**Parameters**:
- `page` (optional, default: 0) - Page number
- `size` (optional, default: 20, max: 100) - Page size

**Response Format**:
```json
{
  "content": [
    {
      "id": "uuid",
      "title": "string",
      "description": "string",
      "duration": 300,
      "thumbnailUrl": "string",
      "uploadedAt": "2024-01-01T00:00:00",
      "viewsCount": 1000,
      "lastAccessed": "2024-01-01T00:00:00",
      "cdnUrls": {
        "thumbnailUrl": "string",
        "cdnEnabled": true
      }
    }
  ],
  "pageable": {...},
  "totalElements": 100,
  "totalPages": 5
}
```

##### GET `/videos/{videoId}`
**Description**: Retrieve specific video metadata by ID
**Method**: GET
**Path Parameters**:
- `videoId` (UUID) - Video identifier

**Response**: Single VideoMetadataDto object

##### GET `/videos/search`
**Description**: Full-text search across video titles
**Method**: GET
**Parameters**:
- `query` (required) - Search query string
- `page` (optional, default: 0) - Page number
- `size` (optional, default: 20, max: 100) - Page size

**Response**: Paginated VideoMetadataDto list

##### GET `/videos/popular`
**Description**: Retrieve videos ordered by view count (most popular first)
**Method**: GET
**Parameters**:
- `page` (optional, default: 0) - Page number
- `size` (optional, default: 20, max: 100) - Page size

**Response**: Paginated VideoMetadataDto list

##### GET `/videos/recent`
**Description**: Retrieve recently watched videos
**Method**: GET
**Parameters**:
- `page` (optional, default: 0) - Page number
- `size` (optional, default: 20, max: 100) - Page size

**Response**: Paginated VideoMetadataDto list

### Error Responses
```json
{
  "timestamp": "2024-01-01T00:00:00",
  "status": 404,
  "error": "Video not found",
  "message": "Video with ID 'uuid' not found"
}
```

## 3. Architectural Patterns

### Layered Architecture
The service follows a traditional layered architecture pattern:

1. **Controller Layer** (`VideoMetadataController`)
   - Handles HTTP requests and responses
   - Input validation and pagination
   - Exception handling

2. **Service Layer** (`VideoMetadataService`)
   - Business logic implementation
   - Data transformation (Entity ↔ DTO)
   - Caching strategy implementation

3. **Repository Layer** (`VideoRepository`)
   - Data access abstraction
   - Custom query implementations
   - Spring Data JPA integration

4. **Model Layer** (Entities and DTOs)
   - Data representation
   - JPA entity mappings
   - Validation constraints

### Shared Database Pattern
**Important**: Uses shared database architecture with Upload Service for ACID compliance and referential integrity.

### Domain-Driven Design Elements
- Clear separation of concerns
- Entity-based modeling
- Repository pattern implementation
- Service abstraction

## 4. Communication Protocols

### Internal Communication
- **HTTP/REST**: Primary API communication protocol
- **Database Transactions**: ACID compliance through shared PostgreSQL database

### External Communication
- **RabbitMQ (AMQP)**: Asynchronous messaging for inter-service communication
  - Exchange: `video.exchange`
  - Queues: `video.metadata.queue`, `video.search.queue`
  - Routing keys: `video.metadata`, `video.search.refresh`

### Data Access Protocols
- **JDBC**: PostgreSQL database connectivity
- **Redis Protocol**: Caching layer communication

## 5. IETF RFC References

- **RFC 2616**: HTTP/1.1 protocol for REST API implementation
- **RFC 3986**: URI specification for endpoint design
- **RFC 7231**: HTTP/1.1 semantics for proper status codes
- **RFC 7234**: HTTP/1.1 caching (implemented via Redis)
- **RFC 5322**: Internet Message Format (for structured logging)

## 6. Configuration & Environment

### Critical Environment Variables
```bash
# Database Configuration
DB_USERNAME=upload_user
DB_PASSWORD=upload_pass

# Redis Configuration  
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=

# RabbitMQ Configuration
RABBITMQ_HOST=localhost
RABBITMQ_USERNAME=guest
RABBITMQ_PASSWORD=guest

# AWS Configuration
S3_BUCKET_NAME=video-hosting-bucket
AWS_REGION=us-east-1
CLOUDFRONT_DOMAIN=
CLOUDFRONT_ENABLED=false
```

### Configuration Files
- `application.yml` - Main configuration
- `application-replica.yml` - Read replica configuration for horizontal scaling

### Service Discovery
- **Port**: 8083 (default)
- **Health Check**: `/actuator/health`
- **Metrics**: `/actuator/metrics`, `/actuator/prometheus`

### Dependencies
- Shared PostgreSQL database with Upload Service
- Redis cache cluster
- RabbitMQ message broker
- AWS S3 (for URL generation)
- CloudFront CDN (optional)

## 7. Observability

### Logging
- **Framework**: SLF4J with Logback
- **Log Level**: INFO (configurable per package)
- **Log Format**: Structured logging with timestamps
- **Log Files**: `logs/metadata-service.log`

### Monitoring Integration
- **Spring Actuator**: Built-in health checks and metrics
- **Prometheus**: `/actuator/prometheus` endpoint for metrics scraping
- **Micrometer**: Metrics collection framework

### Health Checks
```yaml
management:
  endpoint:
    health:
      show-details: when_authorized
```

### Available Metrics
- HTTP request metrics
- Database connection pool metrics
- Cache hit/miss ratios
- Custom business metrics

## 8. Security

### Security Mechanisms
1. **Input Validation**: Bean Validation (JSR-303) annotations
2. **SQL Injection Prevention**: Parameterized queries via JPA
3. **Data Sanitization**: Framework-level input sanitization
4. **Access Control**: Header-based user identification

### Authentication Flow
```
API Gateway → X-User-Id Header → Metadata Service
```

### Security Best Practices
- No sensitive data in logs
- Encrypted database connections
- Secure environment variable handling
- Rate limiting (implemented at gateway level)

### Notable Security Considerations
- **Shared Database Access**: Requires careful privilege management
- **No Direct Authentication**: Relies on upstream security (API Gateway)
- **Data Isolation**: User data separated by user_id fields

## 9. AI/ML Usage

**Current Status**: No AI/ML functionality implemented in this microservice.

**Future Considerations**:
- Recommendation algorithms based on viewing history
- Content-based filtering for video suggestions
- Search relevance scoring improvements
- User behavior analysis for content optimization

## 10. Deployment & Runtime

### Containerization
**Dockerfile**: Not present in current structure, but can be containerized using standard Spring Boot practices:

```dockerfile
FROM openjdk:21-jre-slim
COPY build/libs/metadata-service.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

### Entry Point
- **Main Class**: `com.tskrypko.metadata.MetadataApplication`
- **Port**: 8083
- **Startup Dependencies**: PostgreSQL, Redis (optional), RabbitMQ

### Runtime Dependencies
1. **PostgreSQL**: Shared database (must be started first)
2. **Redis**: Caching layer (optional but recommended)
3. **RabbitMQ**: Message broker for async communication

### Deployment Sequence
```bash
# 1. Start infrastructure
docker-compose up postgresql redis rabbitmq

# 2. Start Upload Service (creates shared tables)
cd ../upload && ./gradlew bootRun

# 3. Start Metadata Service
cd ../metadata && ./gradlew bootRun
```

### Resource Requirements
- **Memory**: 512MB minimum, 1GB recommended
- **CPU**: 1 vCPU minimum
- **Storage**: Minimal (logs only, data in shared PostgreSQL)
- **Network**: Internal cluster communication

### Scaling Considerations
- **Stateless Design**: Horizontal scaling ready
- **Read Replicas**: Database read scaling support
- **Cache Warming**: Redis cache for performance
- **Load Balancing**: Compatible with standard load balancers

---

*Generated on: [Current Date]*  
*Service Version: 0.0.1-SNAPSHOT*  
*Documentation Version: 1.0* 