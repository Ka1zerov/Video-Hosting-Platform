# Video Hosting Platform Infrastructure

This directory contains the shared infrastructure for the entire Video Hosting Platform. All microservices use these centralized services to avoid duplication and simplify management.

## 🏗️ Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                    Video Hosting Platform                       │
├─────────────────────────────────────────────────────────────────┤
│  MICROSERVICES                                                  │
│ ┌─────────────┐   ┌─────────────┐  ┌─────────────┐  ┌─────────┐ │
│ │ Gateway     │─> │ Auth        │  │ Upload      │  │ Metadata│ │
│ │ Port: 8080  │─> │ Port: 8081  │  │ Port: 8082  │  │ Port:   │ │
│ │             │─> │             │  │             │  │ 8083    │ │
│ └─────────────┘   └─────────────┘  └─────────────┘  └─────────┘ │
│                           │                 │             │     │                    │
├───────────────────────────┼─────────────────┼─────────────┼─────┤
│  SHARED INFRASTRUCTURE    │                 │             │     │
│                    ┌──────┴──────┐          │             │     │
│                    │ PostgreSQL  │          │             │     │
│                    │ Auth DB     │          │             │     │
│                    │ Port: 5432  │          │             │     │
│                    └─────────────┘          │             │     │
│                    ┌────────────────────────┴─────────────┴───┐ │
│                    │ PostgreSQL Video DB (Shared)             │ │
│                    │ Port: 5433                               │ │
│                    │ • videos (upload service)                │ │
│                    │ • video_metadata (metadata service)      │ │
│                    │ • categories, playlists, analytics...    │ │
│                    └────────────────────────┬─────────────┬───┘ │
│                    ┌────────────────────────┴─────────────┴──┐  │
│                    │ Redis Cache (Shared)                    │  │
│                    │ Port: 6379                              │  │
│                    │ • Session cache, metadata cache, etc.   │  │
│                    └────────────────────────┬─────────────┬──┘  │
│                    ┌────────────────────────┴─────────────┴──┐  │
│                    │ RabbitMQ Message Broker                 │  │
│                    │ Port: 5672 | UI: 15672                  │  │
│                    │ • Video processing events               │  │
│                    │ • Search indexing events                │  │
│                    │ • Encoding completion events            │  │
│                    └─────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

## 🎯 Services Breakdown

### **Core Infrastructure**
- **postgres-auth**: Authentication service database (port 5432)
- **postgres-video**: Shared database for upload and metadata services (port 5433)  
- **redis**: Shared cache for all video-related services (port 6379)
- **rabbitmq**: Message broker for async communication (port 5672)

### **Admin Tools** (Optional)
- **pgadmin-auth**: Database administration for auth DB (port 5050)
- **pgadmin-video**: Database administration for video DB (port 5051)
- **redis-commander**: Redis management interface (port 8081)

## 🚀 Quick Start

### Start Infrastructure
```bash
# Start all infrastructure services
./scripts/platform.sh start full

# Or start specific service groups
./scripts/platform.sh start auth    # Only auth database
./scripts/platform.sh start video   # Video services (DB + Redis + RabbitMQ)

# Start with admin tools
./scripts/platform.sh start admin
```

### Check Status
```bash
./scripts/platform.sh status
```

### View Logs
```bash
./scripts/platform.sh logs                    # All services
./scripts/platform.sh logs postgres-video     # Specific service
```

### Stop Services
```bash
./scripts/platform.sh stop
```

## 📊 Port Allocation

| Service | Port | Purpose | Access |
|---------|------|---------|---------|
| postgres-auth | 5432 | Auth database | Internal |
| postgres-video | 5433 | Video platform database | Internal |
| redis | 6379 | Shared cache | Internal |
| rabbitmq | 5672 | Message broker | Internal |
| rabbitmq-ui | 15672 | RabbitMQ management | http://localhost:15672 |
| pgadmin-auth | 5050 | Auth DB admin | http://localhost:5050 |
| pgadmin-video | 5051 | Video DB admin | http://localhost:5051 |
| redis-commander | 8081 | Redis admin | http://localhost:8081 |

## 🔧 Service Profiles

Docker Compose profiles allow starting only needed services:

- **`auth`**: Only authentication database
- **`video`**: Video-related services (postgres-video, redis, rabbitmq)  
- **`full`**: All core services (default)
- **`admin`**: All services + admin tools

## 🗄️ Database Strategy

### Separate Databases
- **auth_db** (postgres-auth): User authentication, sessions, permissions
- **video_platform** (postgres-video): Video content, metadata, analytics

### Shared Database for Video Services
Upload and Metadata services share the same PostgreSQL database but use **Liquibase contexts** to avoid conflicts:

```yaml
# Upload Service
spring:
  liquibase:
    contexts: upload-service

# Metadata Service  
spring:
  liquibase:
    contexts: metadata-service
```

This ensures each service only runs its own database migrations.

## 🐰 Message Broker (RabbitMQ)

RabbitMQ handles asynchronous communication between microservices:

### Automatic Startup
RabbitMQ starts automatically with the `video` or `full` profiles. No additional configuration needed!

### Access
- **Management UI**: http://localhost:15672 (guest/guest)
- **AMQP Port**: localhost:5672

### Message Flow
```
Upload Service → video.uploaded → Encoding Service
Encoding Service → video.processed → Metadata Service + Streaming Service
Metadata Service → video.metadata.updated → Search/Streaming Services
```

**📚 For detailed RabbitMQ documentation, see [RABBITMQ.md](RABBITMQ.md)**

## 🛠️ Development Workflow

### 1. Start Infrastructure
```bash
./scripts/platform.sh start video
```

### 2. Start Services (in order)
```bash
# Terminal 1 - Upload Service (creates video tables)
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

### 3. Access Admin Tools
- **RabbitMQ**: http://localhost:15672 (guest/guest)
- **Video DB Admin**: http://localhost:5051 (admin@video.local/admin)
- **Redis Commander**: http://localhost:8081

## 💾 Backup & Restore

### Backup Databases
```bash
./scripts/platform.sh backup-auth     # Creates auth_backup_YYYYMMDD_HHMMSS.sql
./scripts/platform.sh backup-video    # Creates video_backup_YYYYMMDD_HHMMSS.sql
```

### Restore Databases
```bash
./scripts/platform.sh restore-auth auth_backup_20231201_143022.sql
./scripts/platform.sh restore-video video_backup_20231201_143022.sql
```

### Database Shells
```bash
./scripts/platform.sh shell-auth      # Connect to auth database
./scripts/platform.sh shell-video     # Connect to video database  
./scripts/platform.sh shell-redis     # Connect to Redis CLI
```

## 🧹 Cleanup

### Stop and Remove Everything
```bash
./scripts/platform.sh clean
```

This safely removes old docker-compose files and configurations that are no longer needed.

## 🔒 Security Notes

- **Local Development Only**: Current configuration is for local development
- **Default Passwords**: Services use default credentials (change for production)
- **Network Isolation**: Services communicate through `platform-network`
- **No Authentication**: Admin tools have no authentication (local only)

## 📁 Directory Structure

```
infrastructure/
├── docker-compose.yml          # Main infrastructure definition
├── scripts/
│   └── platform.sh            # Management script
├── config/
│   ├── postgres/
│   │   ├── auth-init/         # Auth DB initialization scripts
│   │   └── video-init/        # Video DB initialization scripts
│   └── redis/
│       └── redis.conf         # Redis configuration
├── README.md                  # This file
└── RABBITMQ.md               # RabbitMQ documentation
```

## 📚 Documentation

- **[RABBITMQ.md](RABBITMQ.md)** - Complete RabbitMQ setup and message flow documentation
- **Project Root README.md** - Overall platform architecture and getting started
- **Service READMEs** - Individual service documentation

## 🔄 Migration from Service-Specific Infrastructure

If migrating from individual service docker setups:

1. **Stop old services**: `docker-compose down` in each service
2. **Start new infrastructure**: `./infrastructure/scripts/platform.sh start full`
3. **Update service configs**: Point to new port numbers if needed
4. **Clean old volumes**: Remove old docker volumes if necessary

The new infrastructure maintains the same port numbers and database names for compatibility. 
