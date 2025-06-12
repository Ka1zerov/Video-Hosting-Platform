# Multipart Upload for Video Hosting Platform

This document describes how to use multipart upload for uploading large video files to S3.

## 🎯 What is Multipart Upload?

**Multipart Upload** is a file upload technology that splits files into chunks, providing:

- ✅ **Resumable uploads** - continue from where interruption occurred
- ✅ **Parallel upload** - multiple parts simultaneously  
- ✅ **Reliability** - only failed parts need to be retried
- ✅ **Performance** - faster for large files than regular upload
- ✅ **Memory efficient** - no need to keep entire file in RAM

## 📋 Requirements

### Minimum requirements for multipart upload:
- **File size**: minimum 5 MB
- **Part size**: minimum 5 MB (except for the last part)
- **Maximum number of parts**: 10,000
- **Maximum file size**: 2 GB (service limitation)

### Supported formats:
```
video/mp4, video/avi, video/mov, video/wmv, 
video/flv, video/webm, video/mkv, video/m4v
```

## 🔄 Upload Process

```
1. Initialization ─┐
                   │
2. Upload parts ┼─► Part 1 ─┐
                │            │
                ├─► Part 2 ─┤
                │            ├─► Redis (progress)
                ├─► Part N ─┘
                │
3. Completion ───┘
```

## 📚 API Endpoints

### 1. Initialize multipart upload

```http
POST /api/upload/multipart/initiate
Content-Type: application/json
Authorization: Bearer <token>

{
  "title": "My Video",
  "description": "Video description",
  "originalFilename": "video.mp4",
  "fileSize": 104857600,
  "mimeType": "video/mp4"
}
```

**Response:**
```json
{
  "uploadId": "abc123-def456-ghi789",
  "s3Key": "videos/user123/1703123456789_uuid.mp4",
  "message": "Multipart upload initiated successfully",
  "totalParts": 20,
  "partSize": 5242880
}
```

### 2. Upload part (chunk)

```http
POST /api/upload/multipart/upload-chunk
Content-Type: multipart/form-data
Authorization: Bearer <token>

uploadId: abc123-def456-ghi789
partNumber: 1
chunk: <binary data>
```

**Response:**
```json
{
  "etag": "d41d8cd98f00b204e9800998ecf8427e",
  "partNumber": 1,
  "message": "Chunk uploaded successfully",
  "uploadedParts": 1,
  "totalParts": 20
}
```

### 3. Complete upload

```http
POST /api/upload/multipart/complete/abc123-def456-ghi789
Authorization: Bearer <token>
```

**Response:**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "title": "My Video",
  "description": "Video description",
  "originalFilename": "video.mp4",
  "fileSize": 104857600,
  "status": "UPLOADED",
  "uploadedAt": "2023-12-21T10:15:30",
  "message": "Video successfully uploaded via multipart upload"
}
```

### 4. Check status

```http
GET /api/upload/multipart/status/abc123-def456-ghi789
Authorization: Bearer <token>
```

**Response:**
```json
{
  "uploadId": "abc123-def456-ghi789",
  "s3Key": "videos/user123/1703123456789_uuid.mp4",
  "userId": "user123",
  "title": "My Video",
  "originalFilename": "video.mp4",
  "fileSize": 104857600,
  "totalParts": 20,
  "partSize": 5242880,
  "uploadedParts": {
    "1": "etag1",
    "2": "etag2",
    "5": "etag5"
  },
  "progressPercentage": 15.0,
  "createdAt": "2023-12-21T10:00:00",
  "expiresAt": "2023-12-22T10:00:00"
}
```

### 5. Abort upload

```http
DELETE /api/upload/multipart/abort/abc123-def456-ghi789
```

## 🏗️ Architecture

### System Components:

1. **MultipartUploadController** - REST API endpoints
2. **MultipartUploadService** - business logic
3. **Redis** - session and progress storage
4. **AWS S3** - cloud multipart upload
5. **PostgreSQL** - metadata after completion
6. **RabbitMQ** - completion notifications

### Data Flow:

```
Frontend ──► Controller ──► Service ──► S3
    │             │           │          │
    │             │           └─► Redis ─┘
    │             │           │
    │             │           └─► PostgreSQL ──► RabbitMQ
    │             │
    └─── Status ←─┘
```

## ⚡ Performance Optimization

### Client Recommendations:

1. **Parallel uploads**: No more than 3-5 simultaneously
2. **Part size**: 5-10 MB optimal
3. **Retry logic**: Retry failed parts
4. **Progress bar**: Display upload progress

### Example with Progress Bar:

```javascript
class ProgressTracker {
    constructor(uploader, onProgress) {
        this.uploader = uploader;
        this.onProgress = onProgress;
        this.uploadedParts = 0;
    }

    async uploadWithProgress(file, title, description) {
        await this.uploader.initiate(file, title, description);
        
        const promises = [];
        for (let partNumber = 1; partNumber <= this.uploader.totalParts; partNumber++) {
            const start = (partNumber - 1) * this.uploader.partSize;
            const end = Math.min(start + this.uploader.partSize, file.size);
            const chunk = file.slice(start, end);
            
            promises.push(
                this.uploader.uploadPart(chunk, partNumber)
                    .then(() => {
                        this.uploadedParts++;
                        const progress = (this.uploadedParts / this.uploader.totalParts) * 100;
                        this.onProgress(progress);
                    })
            );
        }
        
        await Promise.all(promises);
        return await this.uploader.complete();
    }
}

// Usage
const tracker = new ProgressTracker(uploader, (progress) => {
    console.log(`Progress: ${progress.toFixed(1)}%`);
    // Update progress bar in UI
});
```

## 🔧 Configuration

### Redis settings:

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      timeout: 5000ms
```

### S3 settings:

```yaml
aws:
  access:
    key: ${AWS_ACCESS_KEY}
  secret:
    key: ${AWS_SECRET_KEY}
  region: us-east-1
  s3:
    bucket:
      name: ${S3_BUCKET_NAME}
      prefix: videos/
```

## 🐛 Error Handling

### Common Errors:

1. **Session expired**: Re-initialize upload
2. **Invalid part size**: Check chunk size
3. **Missing part**: Upload missing parts
4. **S3 errors**: Retry with exponential backoff


## 📊 Monitoring

### Redis keys for monitoring:

```
multipart:upload:{uploadId} - upload session
TTL: 24 hours
```

### Metrics to track:

- Number of active sessions
- Average upload progress
- Number of completed/aborted uploads
- Multipart upload completion time

## 🔒 Security

1. **Authentication**: JWT tokens for all operations
2. **Validation**: File size and type checks
3. **Session TTL**: Automatic expiration after 24 hours
4. **Limits**: Maximum file size 2GB 