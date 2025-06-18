# Video Hosting Platform Infrastructure - Technical Documentation

## 1. Overview

### Infrastructure Component Name
Video Hosting Platform Shared Infrastructure

### Purpose and Responsibilities
The infrastructure component serves as the backbone of a distributed video hosting platform, providing:
- Centralized data storage with PostgreSQL master-slave replication
- High-performance caching layer with Redis
- Asynchronous message processing via RabbitMQ
- Load balancing and high availability through HAProxy
- Administrative tools for monitoring and database management

### Key Technologies and Frameworks Used
- **PostgreSQL 14/15**: Primary and authentication databases with streaming replication
- **HAProxy 2.8**: Load balancer for database connections
- **Redis 7**: In-memory data structure store for caching
- **RabbitMQ 3**: Message broker with management interface
- **Docker Compose**: Container orchestration
- **pgAdmin 4**: Database administration interface
- **Redis Commander**: Redis management interface

## 2. API Specification

### Infrastructure Management API (via shell scripts)
The infrastructure provides a command-line API through the `platform.sh` script:

| Command | HTTP Method Equivalent | Purpose | Response Format |
|---------|----------------------|---------|----------------|
| `./platform.sh start [profile]` | POST | Start infrastructure services | Status output |
| `./platform.sh stop` | DELETE | Stop all services | Status output |
| `./platform.sh status` | GET | Get services status | Docker compose status |
| `./platform.sh logs [service]` | GET | Retrieve service logs | Log streams |
| `./platform.sh replication-status` | GET | Check PostgreSQL replication health | SQL query results |
| `./platform.sh backup-video` | POST | Create database backup | Backup status |
| `./platform.sh restore-video` | PUT | Restore database from backup | Restore status |

### Database Connection Endpoints
| Service | Port | Protocol | Authentication |
|---------|------|----------|---------------|
| Auth Database | 5432 | PostgreSQL | app_user/app_pass |
| Video Database Master | 5433 | PostgreSQL | upload_user/upload_pass |
| Video Database Slave | 5434 | PostgreSQL | upload_user/upload_pass |
| HAProxy Write (Master) | 5435 | PostgreSQL | upload_user/upload_pass |
| HAProxy Read (Balanced) | 5436 | PostgreSQL | upload_user/upload_pass |
| Redis | 6379 | Redis Protocol | No authentication |
| RabbitMQ | 5672 | AMQP | guest/guest |

### Management Web Interfaces
| Service | URL | Authentication |
|---------|-----|---------------|
| RabbitMQ Management | http://localhost:15672 | guest/guest |
| HAProxy Statistics | http://localhost:8404/stats | No authentication |
| pgAdmin Auth | http://localhost:5050 | admin@auth.local/admin |
| pgAdmin Video | http://localhost:5051 | admin@video.local/admin |
| Redis Commander | http://localhost:8081 | No authentication |

## 3. Architectural Patterns

### Master-Slave Database Pattern
- **Pattern**: Database Replication with Read/Write Splitting
- **Implementation**: PostgreSQL streaming replication with HAProxy load balancing
- **Benefits**: Improved read performance, high availability, horizontal scaling for read operations

### Load Balancer Pattern
- **Pattern**: Reverse Proxy Load Balancing
- **Implementation**: HAProxy with TCP mode for database connections
- **Configuration**: 
  - Write operations routed exclusively to master (port 5435)
  - Read operations load balanced between master and slave (port 5436)

### Message Broker Pattern
- **Pattern**: Publish-Subscribe Messaging
- **Implementation**: RabbitMQ with topic exchanges
- **Message Flow**: Upload → Encoding → Metadata → Streaming services

### Shared Database Pattern
- **Pattern**: Database Per Service with Shared Infrastructure
- **Implementation**: Separate logical databases for auth and video services
- **Context Separation**: Liquibase contexts prevent schema conflicts

## 4. Communication Protocols

### Internal Communication
- **PostgreSQL Protocol**: Database connections using libpq wire protocol
- **Redis Protocol (RESP)**: Cache operations using Redis Serialization Protocol
- **AMQP 0-9-1**: Message queuing through RabbitMQ

### External Communication
- **HTTP/1.1**: Management interfaces and statistics
- **TCP**: Database load balancing through HAProxy

### Message Protocols
- **Video Upload Events**: JSON messages via AMQP
- **Video Processing Events**: JSON messages with quality metadata
- **Metadata Update Events**: JSON messages for search indexing

## 5. IETF RFC References

- **RFC 793 (TCP)**: Transmission Control Protocol for database connections
- **RFC 2616 (HTTP/1.1)**: Management interfaces communication
- **RFC 3986 (URI)**: Database connection strings format
- **RFC 4627 (JSON)**: Message payload format for RabbitMQ

## 6. Configuration & Environment

### Environment Variables
```yaml
# PostgreSQL Configuration
POSTGRES_DB: auth_db / video_platform
POSTGRES_USER: app_user / upload_user
POSTGRES_PASSWORD: app_pass / upload_pass
POSTGRES_REPLICATION_USER: replicator
POSTGRES_REPLICATION_PASSWORD: replicator_pass

# RabbitMQ Configuration  
RABBITMQ_DEFAULT_USER: guest
RABBITMQ_DEFAULT_PASS: guest

# pgAdmin Configuration
PGADMIN_DEFAULT_EMAIL: admin@auth.local / admin@video.local
PGADMIN_DEFAULT_PASSWORD: admin
```

### Configuration Files
- `docker-compose.yml`: Service orchestration and networking
- `config/haproxy/haproxy.cfg`: Load balancer configuration
- `config/redis/redis.conf`: Redis server configuration
- `config/postgres/auth-init/`: Auth database initialization scripts
- `config/postgres/video-init/`: Video database initialization scripts
- `config/postgres/master-init.sql`: Master database replication setup
- `config/postgres/slave-init.sh`: Slave database setup script

### Service Discovery
- **Docker Compose Networking**: Services communicate via service names
- **Network**: `platform-network` bridge network for service isolation
- **DNS Resolution**: Automatic service name resolution within Docker network

### Docker Compose Profiles
- `auth`: Authentication services only
- `video`: Video platform services (master-slave PostgreSQL, Redis, RabbitMQ)
- `full`: All core services (default)
- `admin`: All services plus administrative tools

## 7. Observability

### Logging Practices
- **Structured Logging**: Services use structured log formats
- **Centralized Access**: `./platform.sh logs [service]` for log aggregation
- **Log Levels**: Different verbosity levels for debugging and production

### Monitoring Integrations
- **HAProxy Statistics**: Real-time connection and performance metrics
- **PostgreSQL Stats**: Built-in pg_stat_replication for replication monitoring
- **RabbitMQ Management**: Queue depths, message rates, consumer metrics
- **Docker Health Checks**: Container health monitoring for all services

### Key Metrics Monitored
- Database connection counts and response times
- Replication lag and status
- Message queue depths and processing rates
- Redis cache hit rates and memory usage
- Service availability and health check status

## 8. Security

### Authentication Mechanisms
- **Database Authentication**: Username/password authentication for PostgreSQL
- **RabbitMQ Authentication**: Guest account for development (should be changed in production)
- **Network Isolation**: Docker bridge network limits external access

### Security Best Practices
- **Least Privilege**: Separate database users for different services
- **Network Segmentation**: Services isolated in Docker network
- **Health Checks**: Continuous monitoring of service availability
- **Data Persistence**: Named volumes for data durability

### Notable Security Risks
- **Default Credentials**: RabbitMQ uses default guest/guest credentials
- **No TLS**: Database connections not encrypted (suitable for local development)
- **Admin Interface Exposure**: Management interfaces exposed on all network interfaces
- **Replication Authentication**: Basic password authentication for replication user

## 9. AI/ML Usage

**Not Applicable**: The infrastructure component does not implement any AI/ML functionality. This is a pure infrastructure layer providing data storage, caching, and messaging services for the video hosting platform.

## 10. Deployment & Runtime

### Containerization
All services run in Docker containers with the following specifications:

#### PostgreSQL Containers
```yaml
# Master Database
postgres-video-master:
  image: postgres:15
  ports: ["5433:5432"]
  volumes: 
    - postgres_video_master_data:/var/lib/postgresql/data
    - ./config/postgres/video-init:/docker-entrypoint-initdb.d
  command: postgres -c wal_level=replica -c max_wal_senders=3
```

#### HAProxy Container  
```yaml
haproxy-postgres:
  image: haproxy:2.8-alpine
  ports: ["5435:5435", "5436:5436", "8404:8404"]
  volumes: ["./config/haproxy/haproxy.cfg:/usr/local/etc/haproxy/haproxy.cfg:ro"]
```

#### Redis Container
```yaml
redis:
  image: redis:7-alpine
  ports: ["6379:6379"]
  volumes: 
    - redis_data:/data
    - ./config/redis/redis.conf:/usr/local/etc/redis/redis.conf
```

#### RabbitMQ Container
```yaml
rabbitmq:
  image: rabbitmq:3-management-alpine
  ports: ["5672:5672", "15672:15672"]
  volumes: [rabbitmq_data:/var/lib/rabbitmq]
```

### Entry Points and Exposed Ports
| Service | Internal Port | External Port | Protocol |
|---------|---------------|---------------|----------|
| Auth PostgreSQL | 5432 | 5432 | TCP |
| Video PostgreSQL Master | 5432 | 5433 | TCP |
| Video PostgreSQL Slave | 5432 | 5434 | TCP |
| HAProxy Write | 5435 | 5435 | TCP |
| HAProxy Read | 5436 | 5436 | TCP |
| HAProxy Stats | 8404 | 8404 | HTTP |
| Redis | 6379 | 6379 | TCP |
| RabbitMQ AMQP | 5672 | 5672 | TCP |
| RabbitMQ Management | 15672 | 15672 | HTTP |

### Exposed Volumes
- `postgres_auth_data`: Auth database persistent storage
- `postgres_video_master_data`: Master database persistent storage  
- `postgres_video_slave_data`: Slave database persistent storage
- `redis_data`: Redis data persistence
- `rabbitmq_data`: RabbitMQ data persistence
- `pgadmin_auth_data`: pgAdmin auth configuration
- `pgadmin_video_data`: pgAdmin video configuration

### Runtime Dependencies
- **Docker Engine**: Version 20.10 or higher
- **Docker Compose**: Version 2.0 or higher
- **Available Ports**: 5432-5436, 6379, 5672, 8081, 8404, 15672
- **System Resources**: Minimum 4GB RAM, 20GB disk space for persistent volumes

### Health Checks
All services include health check configurations:
- **PostgreSQL**: `pg_isready` command execution
- **Redis**: `redis-cli ping` command
- **RabbitMQ**: `rabbitmqctl status` command  
- **HAProxy**: Configuration file validation

### Startup Sequence
1. **Network Creation**: Docker bridge network establishment
2. **Volume Creation**: Named volumes for data persistence
3. **Database Initialization**: Master database startup and initialization
4. **Replication Setup**: Slave database configuration and synchronization
5. **Load Balancer**: HAProxy startup with backend health checks
6. **Cache and Messaging**: Redis and RabbitMQ service startup
7. **Administrative Tools**: Optional admin interface startup

---

*Document Version: 1.0*
*Platform Version: Video Hosting Platform v1.0*  
*Authors: Platform Development Team* 
