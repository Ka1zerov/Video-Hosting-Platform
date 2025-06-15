# Video Hosting Platform Infrastructure

This directory contains the shared infrastructure for the entire Video Hosting Platform with **PostgreSQL Master-Slave replication** support. All microservices use these centralized services to avoid duplication and simplify management.

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
- **postgres-video-master**: Primary database for video services (port 5433)
- **postgres-video-slave**: Read replica database (port 5434)
- **haproxy-postgres**: PostgreSQL load balancer (ports 5435, 5436)
- **redis**: Shared cache for all video-related services (port 6379)
- **rabbitmq**: Message broker for async communication (port 5672)

### **Admin Tools** (Optional)
- **pgadmin-auth**: Database administration for auth DB (port 5050)
- **pgadmin-video**: Database administration for video DB (port 5051)
- **redis-commander**: Redis management interface (port 8081)

## 🔄 Master-Slave Replication

### How It Works
- **Master Database** (port 5433): Handles all write operations
- **Slave Database** (port 5434): Read-only replica synchronized with master
- **HAProxy Load Balancer**: Routes traffic intelligently
  - Port **5435**: Routes writes to master only
  - Port **5436**: Routes reads to master + slave (load balanced)

### Application Configuration
Each microservice needs two datasource configurations:

```yaml
spring:
  # Write operations - HAProxy routes to Master
  datasource:
    url: jdbc:postgresql://localhost:5435/video_platform
    
  # Read-only operations - HAProxy routes to Master + Slave  
  datasource-readonly:
    url: jdbc:postgresql://localhost:5436/video_platform
```

## 🚀 Quick Start

### Start Infrastructure
```bash
# Start all infrastructure services (including replication)
./scripts/platform.sh start full

# Or start specific service groups
./scripts/platform.sh start auth    # Only auth database
./scripts/platform.sh start video   # Video services (Master+Slave+Redis+RabbitMQ)

# Start with admin tools
./scripts/platform.sh start admin
```

### Check Replication Status
```bash
./scripts/platform.sh replication-status
```

### Check Status
```bash
./scripts/platform.sh status
```

### View Logs
```bash
./scripts/platform.sh logs                       # All services
./scripts/platform.sh logs postgres-video-master # Master database logs
./scripts/platform.sh logs postgres-video-slave  # Slave database logs
./scripts/platform.sh logs haproxy-postgres      # Load balancer logs
```

### Stop Services
```bash
./scripts/platform.sh stop
```

## 📊 Port Allocation

### Production Infrastructure
| Service | Port | Purpose | Access |
|---------|------|---------|---------|
| postgres-auth | 5432 | Auth database | Internal |
| postgres-video-master | 5433 | Video platform master DB | Internal |
| postgres-video-slave | 5434 | Video platform slave DB | Internal |
| **haproxy-postgres-write** | **5435** | **Master DB (writes only)** | **Applications** |
| **haproxy-postgres-read** | **5436** | **Read load balancer** | **Applications** |
| redis | 6379 | Shared cache | Internal |
| rabbitmq | 5672 | Message broker | Internal |
| rabbitmq-ui | 15672 | RabbitMQ management | http://localhost:15672 |
| haproxy-stats | 8404 | HAProxy statistics | http://localhost:8404/stats |
| pgadmin-auth | 5050 | Auth DB admin | http://localhost:5050 |
| pgladmin-video | 5051 | Video DB admin | http://localhost:5051 |
| redis-commander | 8081 | Redis admin | http://localhost:8081 |

## 🔧 Service Profiles

Docker Compose profiles allow starting only needed services:

- **`auth`**: Only authentication database
- **`video`**: Video-related services (postgres master+slave, redis, rabbitmq, haproxy)  
- **`full`**: All core services (default)
- **`admin`**: All services + admin tools

## 🗄️ Database Strategy

### Separate Databases
- **auth_db** (postgres-auth): User authentication, sessions, permissions
- **video_platform** (postgres-video-master/slave): Video content, metadata, analytics

### Master-Slave Replication
- **Streaming Replication**: Real-time synchronization between master and slave
- **Read Scaling**: Distribute read queries across master and slave
- **High Availability**: Automatic failover capabilities

### Shared Database for Video Services
Upload, Metadata, Streaming, and Encoding services share the same PostgreSQL database but use **Liquibase contexts** to avoid conflicts:

```yaml
# Upload Service
spring:
  liquibase:
    contexts: upload-service

# Metadata Service  
spring:
  liquibase:
    contexts: metadata-service

# Streaming Service
spring:
  liquibase:
    contexts: streaming-service
```

## ⚖️ HAProxy Load Balancer

HAProxy intelligently routes database connections:

### Write Operations (Port 5435)
- Routes **only** to master database
- Ensures data consistency
- Used by application's primary datasource

### Read Operations (Port 5436)  
- Load balances between master and slave
- 2:1 ratio favoring slave for reads
- Used by application's readonly datasource

### Monitoring
- **Statistics Page**: http://localhost:8404/stats
- **Health Checks**: Automatic failover if database goes down

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

### 2. Wait for Replication Setup
```bash
# Check that replication is working
./scripts/platform.sh replication-status
```

### 3. Start Services (in order)
```bash
# For development (using regular application.yml)
cd upload && ./gradlew bootRun
cd metadata && ./gradlew bootRun  
cd encoding && ./gradlew bootRun
cd streaming && ./gradlew bootRun

# For production with replication (using application-replica.yml)
cd upload && ./gradlew bootRun --spring.profiles.active=replica
cd metadata && ./gradlew bootRun --spring.profiles.active=replica
cd encoding && ./gradlew bootRun --spring.profiles.active=replica
cd streaming && ./gradlew bootRun --spring.profiles.active=replica
```

### 4. Access Admin Tools
- **HAProxy Stats**: http://localhost:8404/stats
- **RabbitMQ**: http://localhost:15672 (guest/guest)
- **Video DB Admin**: http://localhost:5051 (admin@video.local/admin)
- **Redis Commander**: http://localhost:8081

## 🧪 Testing

Integration tests use **TestContainers** to automatically manage all required services:
- **PostgreSQL**: Isolated database for each test run
- **RabbitMQ**: Message broker for async communication

TestContainers handles all container lifecycle management automatically - no manual setup required.

## 🛠️ Backup & Restore

### Backup Databases
```bash
./scripts/platform.sh backup-auth     # Creates auth_backup_YYYYMMDD_HHMMSS.sql
./scripts/platform.sh backup-video    # Creates video_backup_YYYYMMDD_HHMMSS.sql (from master)
```

### Restore Databases
```bash
./scripts/platform.sh restore-auth auth_backup_20231201_143022.sql
./scripts/platform.sh restore-video video_backup_20231201_143022.sql
```

### Database Shells
```bash
./scripts/platform.sh shell-auth      # Connect to auth database
./scripts/platform.sh shell-video     # Connect to video master database  
./scripts/platform.sh shell-redis     # Connect to Redis CLI
```

### Replication Management
```bash
./scripts/platform.sh replication-status    # Check replication lag and status
./scripts/platform.sh promote-slave         # Promote slave to master (emergency)
```

## 🧹 Cleanup

### Stop and Remove Everything
```bash
./scripts/platform.sh clean
```

This safely removes containers and volumes.

## 🔒 Security Notes

- **Local Development Only**: Current configuration is for local development
- **Default Passwords**: Services use default credentials (change for production)
- **Network Isolation**: Services communicate through `platform-network`
- **Replication Security**: Uses dedicated replication user with minimal privileges

## 📁 Directory Structure

```
infrastructure/
├── docker-compose.yml          # Main infrastructure definition with replication
├── scripts/
│   └── platform.sh            # Management script with replication commands
├── config/
│   ├── postgres/
│   │   ├── master-init.sql     # Master database replication setup
│   │   └── slave-init.sh       # Slave database replication setup
│   ├── haproxy/
│   │   └── haproxy.cfg         # Load balancer configuration
│   └── redis/
│       └── redis.conf          # Redis configuration
└── README.md                   # This file
```

## 🚨 Important Notes

### Application Configuration
- Use **application-replica.yml** profiles for production
- Regular **application.yml** still works for development (connects directly to port 5433)
- HAProxy automatically handles failover and load balancing

### Database Users
- All services should use the same database user (`upload_user`) for simplicity
- Consider separate users for production environments

### Replication Lag
- Monitor replication lag via HAProxy stats page
- Typical lag should be < 1ms for local setup
- High lag indicates performance issues

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
