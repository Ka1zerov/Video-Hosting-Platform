# Upload Service

Upload Service is part of the Video Hosting Platform and handles video file upload processing.

## Features

- **Video file uploads** to Amazon S3
- **File validation** (type, size)
- **Video metadata storage** in PostgreSQL
- **Message publishing** to RabbitMQ for further processing
- **User authentication** via Gateway
- **CRUD operations** for video management
- **Database migrations** via Liquibase
- **Automatic audit** for created/modified fields
- **Soft delete** for data recovery (files still deleted from S3)

## Database Schema

Database migrations are managed by Liquibase. The main entities:

### Video Entity
- **id** (UUID) - Primary key
- **title** (VARCHAR) - Video title
- **description** (TEXT) - Video description
- **original_filename** (VARCHAR) - Original file name
- **file_size** (BIGINT) - File size in bytes
- **mime_type** (VARCHAR) - MIME type
- **s3_key** (VARCHAR) - S3 object key
- **status** (VARCHAR) - Video processing status
- **uploaded_at** (TIMESTAMP) - Upload timestamp
- **user_id** (VARCHAR) - User identifier
- **created_at** (TIMESTAMP) - Record creation timestamp
- **modified_at** (TIMESTAMP) - Record modification timestamp
- **created_by** (VARCHAR) - User who created the record
- **modified_by** (VARCHAR) - User who last modified the record
- **deleted_at** (TIMESTAMP) - Soft delete timestamp (null = not deleted)

## API Endpoints

### Upload video
```http
POST /api/upload/video
Content-Type: multipart/form-data

Parameters:
- file: video file (required)
- title: video title (required)
- description: video description (optional)
```

### Upload video with metadata
```http
POST /api/upload/video-with-metadata
Content-Type: multipart/form-data
```

### Get video by ID
```http
GET /api/upload/video/{videoId}
```

### Get user's video list
```http
GET /api/upload/videos
```

### Delete video (soft delete)
```http
DELETE /api/upload/video/{videoId}
```
Note: This performs soft delete - marks video as deleted in database but keeps metadata for recovery. S3 file is permanently deleted.

### Restore deleted video
```http
POST /api/upload/video/{videoId}/restore
```
Note: Restores a soft-deleted video. S3 file cannot be restored if it was already deleted.

### Permanently delete video
```http
DELETE /api/upload/video/{videoId}/permanent
```
Note: Permanently removes video metadata from database. Video must be soft-deleted first.

### Service health check
```http
GET /api/upload/health
```

## Soft Delete Behavior

- **Soft Delete**: `DELETE /api/upload/video/{videoId}`
  - Marks video as deleted in database (`deleted_at` timestamp)
  - Sets status to `DELETED`
  - **Permanently deletes file from S3** (for cost efficiency)
  - Video metadata remains in database for recovery

- **Restore**: `POST /api/upload/video/{videoId}/restore`
  - Removes `deleted_at` timestamp
  - Sets status back to `UPLOADED`
  - **Note**: S3 file cannot be restored and must be re-uploaded

- **Permanent Delete**: `DELETE /api/upload/video/{videoId}/permanent`
  - Physically removes record from database
  - Only works on already soft-deleted videos

## Configuration

### Environment Variables

#### Database
- `DB_USERNAME` - PostgreSQL username (default: postgres)
- `DB_PASSWORD` - PostgreSQL password (default: postgres)

#### AWS S3
- `AWS_ACCESS_KEY` - AWS access key
- `AWS_SECRET_KEY` - AWS secret key
- `AWS_REGION` - AWS region (default: us-east-1)
- `S3_BUCKET_NAME` - S3 bucket name
- `S3_BUCKET_PREFIX` - file prefix (default: videos/)

#### RabbitMQ
- `RABBITMQ_HOST` - RabbitMQ host (default: localhost)
- `RABBITMQ_PORT` - RabbitMQ port (default: 5672)
- `RABBITMQ_USERNAME` - RabbitMQ username (default: guest)
- `RABBITMQ_PASSWORD` - RabbitMQ password (default: guest)

## Supported Video Formats

- MP4 (video/mp4)
- AVI (video/avi)
- MOV (video/mov)
- WMV (video/wmv)
- FLV (video/flv)
- WebM (video/webm)
- MKV (video/mkv)
- M4V (video/m4v)

## Limitations

- **Maximum file size**: 2GB
- **Authentication required** for all operations (except health check)
- **User isolation** - each user sees only their own videos
- **S3 files cannot be restored** after deletion (only metadata)

## Architecture

Upload Service integrates with:

1. **Amazon S3** - for video file storage
2. **PostgreSQL** - for metadata storage
3. **RabbitMQ** - for sending messages to Encoding Service
4. **Gateway** - for user authentication via X-User-Id header

## JPA Configuration

Current JPA settings provide:

- **`ddl-auto: validate`** - Validates database schema matches entities without modifying it (production-safe)
- **`show-sql: true`** - Shows SQL queries in logs for debugging
- **`open-in-view: false`** - Prevents lazy loading outside transaction boundaries (avoids N+1 problems)

## Running

```bash
# Build project
./gradlew build

# Run application
./gradlew bootRun
```

Service will be available on port 8082.

## Docker

```bash
# Build Docker image
docker build -t upload-service .

# Run container
docker run -p 8082:8082 upload-service
```

## Database Migrations

Database schema is managed by Liquibase. Migration files are located in:
- `src/main/resources/db/changelog/db.changelog-master.yaml` - Master changelog
- `src/main/resources/db/changelog/changes/` - Individual migration files

To run migrations manually:
```bash
./gradlew update
```

## Monitoring

Available monitoring endpoints:
- `/actuator/health` - application health
- `/actuator/info` - application information
- `/actuator/metrics` - application metrics 