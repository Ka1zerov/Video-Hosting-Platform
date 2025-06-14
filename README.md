# Video Hosting Platform

This project was developed as part of my Master's thesis, implementing a microservices-based video hosting platform with modern technologies.

## 🏗️ Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────────────────────────────┐
│                                  Video Hosting Platform                                         │
├─────────────────────────────────────────────────────────────────────────────────────────────────┤
│  PRESENTATION LAYER                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────────────────────────┐   │
│  │                                Frontend                                                 │   │
│  │                      Web Interface / Mobile App                                        │   │
│  └─────────────────────────────────────────────────────────────────────────────────────────┘   │
│                                         │                                                       │
├─────────────────────────────────────────┼───────────────────────────────────────────────────────┤
│  API LAYER                              │                                                       │
│  ┌─────────────────────────────────────────────────────────────────────────────────────────┐   │
│  │                            API Gateway (Port: 8080)                                    │   │
│  │         Single Entry Point | JWT Auth | CORS | Request Routing                        │   │
│  └─────────────────────────────────────────────────────────────────────────────────────────┘   │
│         │              │                │                │                │                     │
├─────────┼──────────────┼────────────────┼────────────────┼────────────────┼─────────────────────┤
│  MICROSERVICES LAYER   │                │                │                │                     │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐           │
│  │    Auth     │  │   Upload    │  │  Metadata   │  │  Streaming  │  │  Encoding   │           │
│  │ Port: 8081  │  │ Port: 8082  │  │ Port: 8083  │  │ Port: 8084  │  │ Port: 8085  │           │
│  │             │  │             │  │             │  │             │  │             │           │
│  │ JWT Token   │  │ File Upload │  │ Video Info  │  │ HLS/DASH    │  │ Transcoding │           │
│  │ Management  │  │ S3 Storage  │  │ Search &    │  │ Manifests   │  │ Multiple    │           │
│  │             │  │             │  │ Analytics   │  │             │  │ Qualities   │           │
│  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────┘           │
│         │                │                │                │                │                     │
├─────────┼────────────────┼────────────────┼────────────────┼────────────────┼─────────────────────┤
│  SHARED INFRASTRUCTURE                                                                          │
│         │                │                │                │                │                     │
│  ┌─────────────┐  ┌───────────────────────────────────┐                                         │
│  │ PostgreSQL  │  │     PostgreSQL Video DB           │                                         │
│  │  Auth DB    │  │        (Shared)                   │                                         │
│  │ Port: 5432  │  │      Port: 5433                   │                                         │
│  │             │  │ • videos (upload service)         │                                         │
│  │ Users       │  │ • video_metadata (metadata)       │                                         │
│  │ Sessions    │  │ • categories, playlists           │                                         │
│  │ Permissions │  │ • analytics, user interactions    │                                         │
│  └─────────────┘  └───────────────────────────────────┘                                         │
│         │                                │                                                       │
│  ┌─────────────────────────────────────────────────────────────────────────────────────────┐   │
│  │                           Redis Cache (Shared)                                         │   │
│  │                             Port: 6379                                                 │   │
│  │         • Session cache • Metadata cache • JWT blacklist • Rate limiting              │   │
│  └─────────────────────────────────────────────────────────────────────────────────────────┘   │
│         │                                                                                       │
│  ┌─────────────────────────────────────────────────────────────────────────────────────────┐   │
│  │                        RabbitMQ Message Broker                                         │   │
│  │                      Port: 5672 | UI: 15672                                            │   │
│  │    • Video upload events • Encoding job queue • Processing completion events          │   │
│  │    • Search indexing events • Analytics events • Notification events                  │   │
│  └─────────────────────────────────────────────────────────────────────────────────────────┘   │
│         │                                                                                       │
│  ┌─────────────────────────────────────────────────────────────────────────────────────────┐   │
│  │                              AWS S3 Storage                                            │   │
│  │  • Original videos (upload service) • Encoded segments (multiple qualities)           │   │
│  │  • Thumbnails • Subtitles • Temporary files • Backup archives                         │   │
│  └─────────────────────────────────────────────────────────────────────────────────────────┘   │
│         │                                                                                       │
│  ┌─────────────────────────────────────────────────────────────────────────────────────────┐   │
│  │                         Amazon CloudFront CDN                                          │   │
│  │                    Global Content Delivery Network                                     │   │
│  │       • Cached video segments • Reduced latency • Global distribution                  │   │
│  └─────────────────────────────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────────────────────────────┘
```

## 🎯 Microservices

### Core Services
- **🌐 Gateway** (`gateway/`) - API Gateway with authentication and CORS handling
- **🔐 Authentication** (`authentication/`) - User authentication and authorization  
- **📤 Upload Service** (`upload/`) - Video file upload and storage (S3) with enterprise reliability features
- **📊 Metadata Service** (`metadata/`) - Video metadata, search, analytics, and user interactions

### Video Processing Services
- **🎬 Encoding Service** (`encoding/`) - Video transcoding to multiple qualities (480p, 720p, 1080p)
- **🎥 Streaming Service** (`streaming/`) - Video streaming and delivery

## 🚀 Quick Start

### 1. Start Infrastructure
```bash
# Start all shared infrastructure (PostgreSQL, Redis, RabbitMQ)
cd infrastructure
./scripts/platform.sh start full
```

### 2. Start Microservices
```bash
# Terminal 1 - Upload Service (must start first - creates video tables)
cd upload
./gradlew bootRun

# Terminal 2 - Metadata Service  
cd metadata
./gradlew bootRun

# Terminal 3 - Encoding Service
cd encoding
./gradlew bootRun

# Terminal 4 - Streaming Service
cd streaming
./gradlew bootRun

# Terminal 5 - Gateway
cd gateway
./gradlew bootRun

# Terminal 6 - Auth Service
cd authentication
./gradlew bootRun
```

### 3. Access the Platform
- **API Gateway**: http://localhost:8080
- **Upload Service**: http://localhost:8082
- **Metadata Service**: http://localhost:8083  
- **Streaming Service**: http://localhost:8084
- **Encoding Service**: http://localhost:8085
- **Auth Service**: http://localhost:8081

### 4. Admin Tools (Optional)
```bash
# Start with admin tools
cd infrastructure
./scripts/platform.sh start admin
```
- **RabbitMQ UI**: http://localhost:15672 (guest/guest)
- **Video DB Admin**: http://localhost:5051 (admin@video.local/admin)
- **Redis Commander**: http://localhost:8081
- **Upload Service Admin**: http://localhost:8082/api/upload/multipart/admin/cleanup-stats

## 🏭 Infrastructure

### Shared Components
- **PostgreSQL Auth** (5432) - Authentication database
- **PostgreSQL Video** (5433) - Shared database for upload + metadata services
- **Redis** (6379) - Shared caching layer
- **RabbitMQ** (5672) - Message broker for async communication

### Key Features
- **Database Isolation**: Separate auth and video databases
- **Shared Video Database**: Upload and Metadata services share video_platform DB with Liquibase contexts
- **Service Profiles**: Start only needed infrastructure components
- **Admin Tools**: Optional database and cache management interfaces
- **Automatic Cleanup**: Upload service includes automated cleanup of expired sessions and S3 resources

For detailed infrastructure documentation, see [`infrastructure/README.md`](infrastructure/README.md).

## 🛠️ Technologies

### Backend
- **Java 21** + **Spring Boot 3**
- **Spring Security** - Authentication & authorization
- **Spring Data JPA** - Database access
- **Liquibase** - Database migrations
- **PostgreSQL** - Primary database
- **Redis** - Caching layer
- **RabbitMQ** - Message broker
- **AWS S3** - File storage

### Infrastructure
- **Docker Compose** - Service orchestration
- **HAProxy** - Load balancing (in upload service)
- **Gradle** - Build automation

## 📊 Service Communication

### Synchronous
- **Gateway → Services**: HTTP REST API calls
- **Frontend → Gateway**: REST API

### Asynchronous (RabbitMQ)
- **Upload → Encoding**: Video upload completion events
- **Encoding → Metadata**: Video processing completion events (480p, 720p, 1080p)
- **Upload → Metadata**: Video metadata events
- **Services → Services**: Event-driven communication

## 🗄️ Database Strategy

### Database Per Domain
- **auth_db**: User authentication, sessions, permissions
- **video_platform**: Video content, metadata, analytics (shared)

### Shared Video Database
Upload and Metadata services share the video database but use **Liquibase contexts** to prevent migration conflicts:

```yaml
# Upload Service migrations
contexts: upload-service

# Metadata Service migrations  
contexts: metadata-service
```

## 📁 Project Structure

```
Video-Hosting-Platform/
├── infrastructure/          # Shared infrastructure (Docker, configs)
│   ├── docker-compose.yml   # All infrastructure services
│   ├── scripts/platform.sh  # Infrastructure management
│   └── config/              # Service configurations
├── gateway/                 # API Gateway service
├── authentication/          # Authentication service  
├── upload/                  # Video upload service
├── metadata/               # Video metadata service
├── encoding/               # Video encoding service (multi-quality)
├── streaming/              # Video streaming service
├── README.md               # This file
└── settings.gradle         # Multi-project build
```

## 🔒 Security

- **JWT Authentication**: Stateless authentication via Gateway
- **Service-to-Service**: Trusted internal network communication
- **CORS Handling**: Configured in Gateway for frontend access
- **Input Validation**: Comprehensive validation in all services
- **Database Security**: Separate credentials per service

## 📈 Scalability Features

- **Microservices Architecture**: Independent scaling of components
- **Shared Database**: Efficient for upload/metadata operations
- **Message Queues**: Asynchronous processing
- **Caching Layer**: Redis for performance optimization
- **Load Balancing**: HAProxy configuration ready
- **Multi-Quality Encoding**: Adaptive streaming support

## 🧪 Development

### Prerequisites
- Java 21+
- Docker & Docker Compose
- Gradle 8+

### Development Workflow
1. Start infrastructure: `./infrastructure/scripts/platform.sh start video`
2. Start services in order: upload → metadata → encoding → streaming → gateway → auth
3. Use admin tools for debugging: `./infrastructure/scripts/platform.sh start admin`

### Testing
```bash
# Run tests for all services
./gradlew test

# Run tests for specific service
cd upload && ./gradlew test
```

## 📚 Service Documentation

Each service has its own detailed README:

- [Infrastructure Setup](infrastructure/README.md) - Shared infrastructure management
- [RabbitMQ Message Broker](infrastructure/RABBITMQ.md) - Asynchronous communication and event flow
- [Upload Service](upload/README.md) - Video file upload, S3 storage, multipart uploads, and reliability features
- [Metadata Service](metadata/README.md) - Video metadata, search, and analytics
- Gateway Service - API gateway and routing
- Authentication Service - User management and JWT
- Encoding Service - Multi-quality video transcoding
- Streaming Service - Video delivery and playback

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

**Note**: This platform is designed for educational purposes as part of a Master's thesis. For production use, additional security, monitoring, and deployment considerations would be required.
