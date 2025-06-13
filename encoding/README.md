# Video Encoding Service

The Video Encoding Service is responsible for transcoding uploaded videos into multiple bitrates using HLS (HTTP Live Streaming) format. It processes videos asynchronously using RabbitMQ job queues and stores the encoded content in AWS S3.

## Features

- **Multi-bitrate Encoding**: Transcodes videos into 1080p, 720p, and 480p quality levels
- **HLS Output**: Generates HLS playlists and segments for adaptive streaming
- **Thumbnail Generation**: Creates thumbnails for each quality level
- **Asynchronous Processing**: Uses RabbitMQ for job queue management
- **Progress Tracking**: Monitors encoding progress in real-time
- **Error Handling**: Comprehensive error handling with retry mechanisms
- **S3 Storage**: Stores encoded files in organized S3 bucket structure

## Architecture

### Input/Output Structure

```
bucket-name/
├── originals/
│   └── {uuid}/
│       └── original_video.mp4
├── encoded/
│   └── {video_id}/
│       ├── 1080p/
│       │   ├── playlist.m3u8
│       │   ├── segment_000.ts
│       │   ├── segment_001.ts
│       │   └── ...
│       ├── 720p/
│       │   ├── playlist.m3u8
│       │   └── segments...
│       └── 480p/
│           ├── playlist.m3u8
│           └── segments...
└── thumbnails/
    └── {video_id}/
        ├── thumbnail_1080p.jpg
        ├── thumbnail_720p.jpg
        └── thumbnail_480p.jpg
```

### Technologies Used

- **Spring Boot 3.4.5**: Main application framework
- **FFmpeg**: Video transcoding engine
- **RabbitMQ**: Message queue for asynchronous processing
- **PostgreSQL**: Database for job tracking
- **AWS S3**: Storage for encoded files
- **Redis**: Caching and session management
- **Liquibase**: Database migrations

## Configuration

### Environment Variables

```yaml
# Database
DB_USERNAME=encoding_user
DB_PASSWORD=encoding_pass

# AWS S3
AWS_ACCESS_KEY=your-access-key
AWS_SECRET_KEY=your-secret-key
AWS_REGION=us-east-1
S3_BUCKET_NAME=video-hosting-platform-bucket

# RabbitMQ
RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=guest
RABBITMQ_PASSWORD=guest

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# FFmpeg (macOS with Homebrew)
FFMPEG_PATH=/opt/homebrew/bin/ffmpeg
FFPROBE_PATH=/opt/homebrew/bin/ffprobe

# FFmpeg (Linux)
# FFMPEG_PATH=/usr/bin/ffmpeg
# FFPROBE_PATH=/usr/bin/ffprobe

# Encoding
ENCODING_TEMP_DIR=/tmp/encoding
HLS_SEGMENT_DURATION=10
```

### Application Properties

Key configuration properties:

- `server.port`: Service port (default: 8083)
- `encoding.temp.directory`: Temporary directory for processing
- `encoding.hls.segment.duration`: HLS segment duration in seconds
- `ffmpeg.path`: Path to FFmpeg executable
- `ffprobe.path`: Path to FFprobe executable

### Database Configuration

The encoding service uses a dedicated PostgreSQL database:
- **Host**: localhost:5434
- **Database**: encoding_db
- **User**: encoding_user
- **Password**: encoding_pass

## API Endpoints

### Job Management

- `GET /api/encoding/job/{jobId}` - Get job details
- `GET /api/encoding/job/video/{videoId}` - Get job by video ID
- `GET /api/encoding/jobs` - List jobs (with optional filters)
- `POST /api/encoding/job/{jobId}/retry` - Retry failed job
- `DELETE /api/encoding/job/{jobId}` - Cancel pending job

### Monitoring

- `GET /api/encoding/stats` - Get encoding statistics
- `GET /api/encoding/health` - Health check

## Job Lifecycle

1. **Message Reception**: Receives video upload message from RabbitMQ
2. **Job Creation**: Creates encoding job in database
3. **Video Download**: Downloads original video from S3
4. **Multi-quality Encoding**: Encodes video into multiple bitrates using FFmpeg
5. **Thumbnail Generation**: Creates thumbnails for each quality level
6. **S3 Upload**: Uploads encoded files and thumbnails to S3
7. **Cleanup**: Removes temporary files
8. **Status Update**: Updates job status to completed

## Error Handling

- **Retry Mechanism**: Failed jobs can be retried up to configured limit
- **Progress Tracking**: Real-time progress updates during encoding
- **Comprehensive Logging**: Detailed logs for debugging
- **Temporary File Cleanup**: Automatic cleanup on success or failure

## Development

### Prerequisites

- Java 21
- FFmpeg installed on system
- PostgreSQL database
- RabbitMQ server
- Redis server
- AWS S3 access

### Installing FFmpeg

**macOS (Homebrew):**
```bash
brew install ffmpeg
```

**Ubuntu/Linux:**
```bash
sudo apt update
sudo apt install ffmpeg
```

**Windows:**
Download from https://ffmpeg.org/download.html and add to PATH

### Running Locally

1. **Start infrastructure services:**
```bash
cd infrastructure
docker-compose --profile encoding up -d
```

2. **Create temp directory:**
```bash
mkdir -p /tmp/encoding
```

3. **Configure environment variables or use defaults**

4. **Run the application:**
```bash
cd encoding
./gradlew bootRun
```

### Database Migration

Liquibase migrations are automatically applied on startup. The service uses a dedicated `encoding_db` database for better isolation.

## Monitoring and Health Checks

- **Health Endpoint**: `/actuator/health`
- **Metrics**: `/actuator/metrics`
- **Job Statistics**: `/api/encoding/stats`
- **Database Admin**: http://localhost:5052 (pgAdmin)

## Scalability

The service is designed for horizontal scaling:
- **Stateless Processing**: No shared state between instances
- **Message Queue**: Distributes work across multiple instances
- **Temporary Storage**: Uses instance-specific temp directories
- **Database Coordination**: Uses database for job coordination

### Test Configuration

Integration tests automatically configure themselves for LocalStack:
- **S3 Endpoint**: http://localhost:4566
- **S3 Bucket**: test-bucket (created automatically)
- **AWS Credentials**: test/test
- **Cleanup**: Disabled (files preserved for inspection)

TestContainers automatically configures all services - no manual setup required.

### Integration Tests

Integration tests use **TestContainers** to automatically manage all required services:

```bash
# Run integration tests
./gradlew test --tests "*IntegrationTest"
```

TestContainers automatically starts:
- **PostgreSQL**: Isolated database for each test run
- **RabbitMQ**: Message broker for async communication
- **LocalStack**: AWS S3 emulation

No manual setup required - TestContainers handles everything! 