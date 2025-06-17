# RabbitMQ in Video Hosting Platform

This document explains how RabbitMQ is used in the video hosting platform for asynchronous communication between microservices.

## 🚀 Automatic Startup

RabbitMQ **automatically starts** with the shared infrastructure. No additional configuration needed!

```bash
# RabbitMQ starts automatically with video profile
cd infrastructure
./scripts/platform.sh start video

# Or with full profile
./scripts/platform.sh start full
```

## 📊 Access RabbitMQ

### Management UI
- **URL**: http://localhost:15672
- **Username**: `guest`
- **Password**: `guest`

### Connection Details
- **Host**: localhost
- **Port**: 5672 (AMQP)
- **Virtual Host**: `/`

## 🎯 Message Flow Architecture

```
┌─────────────┐    video.uploaded     ┌─────────────┐
│   Upload    │ ───────────────────► │  Encoding   │
│   Service   │                      │   Service   │
└─────────────┘                      └─────────────┘
                                              │
                video.processed               │
                (480p, 720p, 1080p)           ▼
┌─────────────┐    ◄─────────────────  ┌─────────────┐
│  Metadata   │                        │  Streaming  │
│   Service   │                        │   Service   │
└─────────────┘                        └─────────────┘
      │                                       ▲
      │            video.metadata.updated     │
      └───────────────────────────────────────┘
```

## 📨 Message Types

### 1. Video Upload Events
**Publisher**: Upload Service  
**Consumers**: Encoding Service, Metadata Service

```json
{
  "eventType": "video.uploaded",
  "videoId": "550e8400-e29b-41d4-a716-446655440000",
  "userId": "user123",
  "title": "My Amazing Video",
  "description": "Video description",
  "originalFilename": "video.mp4",
  "fileSize": 52428800,
  "s3Key": "videos/550e8400-e29b-41d4-a716-446655440000/original.mp4",
  "timestamp": "2023-12-01T15:30:00Z"
}
```

## 🔧 Exchange and Queue Configuration

### Exchanges
```yaml
# Configured automatically by services
video.exchange:
  type: topic
  durable: true
```

### Queues and Routing Keys

| Service | Queue | Routing Key | Purpose |
|---------|-------|-------------|---------|
| Encoding | `video.encoding.queue` | `video.uploaded` | Process new uploads |

## 🛠️ Service Configuration

Each service automatically configures its RabbitMQ connection:

```yaml
# Example: upload/src/main/resources/application.yml
spring:
  rabbitmq:
    host: ${RABBITMQ_HOST:localhost}
    port: ${RABBITMQ_PORT:5672}
    username: ${RABBITMQ_USERNAME:guest}
    password: ${RABBITMQ_PASSWORD:guest}
    virtual-host: ${RABBITMQ_VHOST:/}

rabbitmq:
  exchange:
    video: ${RABBITMQ_EXCHANGE:video.exchange}
  queue:
    encoding: ${RABBITMQ_QUEUE_ENCODING:video.encoding.queue}
  routing:
    key:
      encoding: ${RABBITMQ_ROUTING_KEY_ENCODING:video.encoding}
```

## 📈 Monitoring

### Via Management UI
1. Open http://localhost:15672
2. Login with `guest`/`guest`
3. Check:
   - **Queues**: Message counts, consumers
   - **Exchanges**: Message rates
   - **Connections**: Active service connections

### Via Logs
```bash
# View RabbitMQ logs
cd infrastructure
./scripts/platform.sh logs rabbitmq

# View specific service message handling
./scripts/platform.sh logs postgres-video
```

## 🔄 Message Processing Workflow

### 1. Video Upload Flow
```
User uploads video
        ↓
Upload Service stores in S3
        ↓
Upload Service publishes video.uploaded event
        ↓
┌─────────────────────┬─────────────────────┐
│ Encoding Service    │ Metadata Service    │
│ (transcoding)       │ (indexing)          │
└─────────────────────┴─────────────────────┘
```

## ⚡ Benefits

### Asynchronous Processing
- **Non-blocking uploads**: Users don't wait for encoding
- **Parallel processing**: Multiple quality encoding simultaneously
- **Resilient**: Failed messages can be retried

### Decoupled Services
- **Independent scaling**: Scale encoding separately from upload
- **Technology flexibility**: Services can use different tech stacks
- **Fault tolerance**: One service failure doesn't affect others

### Event-Driven Architecture
- **Real-time updates**: Immediate search index updates
- **Audit trail**: Complete event history
- **Easy integration**: New services just subscribe to events

## 🚨 Troubleshooting

### Connection Issues
```bash
# Check RabbitMQ status
cd infrastructure
./scripts/platform.sh logs rabbitmq

# Verify service connections in management UI
# http://localhost:15672 → Connections tab
```

### Message Processing Issues
```bash
# Check queue sizes in management UI
# Dead letter queues indicate processing errors

# Check individual service logs
./scripts/platform.sh logs postgres-video
```

### Performance Issues
```bash
# Monitor queue depths and processing rates
# in management UI → Queues tab

# Consider adding more consumer instances
# for high-throughput queues
```

## 🔒 Security Notes

- **Local Development**: Default credentials used
- **Production**: Change default guest/guest credentials
- **Network**: Services communicate within Docker network
- **SSL/TLS**: Not configured for local development

RabbitMQ is ready to use immediately after starting the infrastructure! 🚀 
