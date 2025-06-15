# 🎬 Encoding and Streaming Services Integration

## 📋 Overview of Changes

Complete integration between encoding and streaming services has been implemented for proper HLS playlist and video quality handling.

### ✅ What was fixed:

1. **Encoding Service:**
   - ✅ Added notification sending for completed qualities via RabbitMQ
   - ✅ Generation of correct URLs for HLS playlists of each quality
   - ✅ Transmission of metadata about file sizes and bitrates

2. **Streaming Service:**
   - ✅ Added RabbitMQ listener for receiving quality notifications
   - ✅ Automatic creation/updating of VideoQuality records in DB
   - ✅ CloudFront integration for CDN URLs
   - ✅ API endpoints for quality checking

## 🏗️ Integration Architecture

```
┌─────────────────┐    RabbitMQ      ┌─────────────────┐
│  Encoding       │─────────────────▶│  Streaming      │
│  Service        │   quality_event  │  Service        │
│                 │                  │                 │
│ 1. Encode video │                  │ 3. Save qualities
│ 2. Upload to S3 │                  │ 4. Update DB    │
│ 3. Send event   │                  │ 5. Serve HLS    │
└─────────────────┘                  └─────────────────┘
```

## 📁 S3 File Structure

```
video-hosting-thesis/
├── originals/
│   └── {uuid}/
│       └── original_video.mp4
├── encoded/
│   └── {video_id}/
│       ├── master.m3u8          ← master playlist
│       ├── 1080p/
│       │   ├── playlist.m3u8    ← now used
│       │   ├── segment_000.ts
│       │   └── ...
│       ├── 720p/
│       │   ├── playlist.m3u8    ← now used
│       │   └── ...
│       └── 480p/
│           ├── playlist.m3u8    ← now used
│           └── ...
└── thumbnails/
    └── {video_id}/
        ├── thumbnail_1080p.jpg
        ├── thumbnail_720p.jpg
        └── thumbnail_480p.jpg
```

## 🔧 New Components

### Encoding Service:

1. **VideoQualityCompletedEvent.java** - DTO for notifications
2. **StreamingNotificationService.java** - notification sending
3. **Updated VideoEncodingService.java** - quality tracking

### Streaming Service:

1. **VideoQualityCompletedEvent.java** - receiving notifications
2. **VideoQualityService.java** - quality management
3. **VideoQualityMessageListener.java** - RabbitMQ listener
4. **VideoQualityController.java** - API for testing
5. **Updated RabbitConfig.java** - configuration

## 🚀 Integration Testing

### 1. Start infrastructure:
```bash
cd infrastructure
docker-compose --profile video up -d
```

### 2. Start services:

**Terminal 1 - Encoding:**
```bash
cd encoding
gradle bootRun
```

**Terminal 2 - Streaming:**
```bash
cd streaming  
gradle bootRun
```

### 3. Upload video (Upload service):
```bash
cd upload
gradle bootRun
```

### 4. Check video qualities:

**Get qualities for video:**
```bash
curl http://localhost:8084/api/streaming/qualities/video/{VIDEO_ID}
```

**Quality statistics:**
```bash
curl http://localhost:8084/api/streaming/qualities/video/{VIDEO_ID}/stats
```

**All qualities in system:**
```bash
curl http://localhost:8084/api/streaming/qualities/all
```

**Get stream with qualities:**
```bash
curl -X POST http://localhost:8084/api/streaming/play \
  -H "Content-Type: application/json" \
  -d '{"videoId": "VIDEO_ID_HERE", "sessionId": "test-session"}'
```

## 📊 API Response Example

### VideoStreamResponse with qualities:

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "title": "Test Video",
  "duration": 120,
  "thumbnailUrl": "https://dj3aclnp3gcgd.cloudfront.net/thumbnails/550e8400.../thumbnail_720p.jpg",
  "hlsManifestUrl": "https://dj3aclnp3gcgd.cloudfront.net/encoded/550e8400.../master.m3u8",
  "qualities": [
    {
      "qualityName": "1080p",
      "width": 1920,
      "height": 1080,
      "bitrate": 4000,
      "hlsPlaylistUrl": "https://dj3aclnp3gcgd.cloudfront.net/encoded/550e8400.../1080p/playlist.m3u8",
      "available": true
    },
    {
      "qualityName": "720p", 
      "width": 1280,
      "height": 720,
      "bitrate": 2500,
      "hlsPlaylistUrl": "https://dj3aclnp3gcgd.cloudfront.net/encoded/550e8400.../720p/playlist.m3u8",
      "available": true
    },
    {
      "qualityName": "480p",
      "width": 854, 
      "height": 480,
      "bitrate": 1000,
      "hlsPlaylistUrl": "https://dj3aclnp3gcgd.cloudfront.net/encoded/550e8400.../480p/playlist.m3u8",
      "available": true
    }
  ],
  "cdnUrls": {
    "hlsUrl": "https://dj3aclnp3gcgd.cloudfront.net/encoded/550e8400.../master.m3u8",
    "thumbnailUrl": "https://dj3aclnp3gcgd.cloudfront.net/thumbnails/550e8400.../thumbnail_720p.jpg",
    "cdnEnabled": true
  }
}
```

## 🔍 Monitoring and Logs

### Encoding Service logs:
```bash
tail -f encoding/logs/encoding-service.log | grep -E "(Uploaded|Sent video qualities)"
```

### Streaming Service logs:
```bash  
tail -f streaming/logs/streaming-service.log | grep -E "(Received video quality|Saved video quality)"
```

### RabbitMQ Management:
- URL: http://localhost:15672
- Login: guest / guest
- Queue: `video.streaming.queue`

## 🛠️ Configuration

### Environment Variables (.env):
```bash
# AWS S3 Configuration  
AWS_ACCESS_KEY=your_access_key
AWS_SECRET_KEY=your_secret_key
AWS_REGION=eu-north-1
S3_BUCKET_NAME=video-hosting-thesis

# CloudFront Configuration
CLOUDFRONT_DOMAIN=dj3aclnp3gcgd.cloudfront.net
CLOUDFRONT_ENABLED=true
CDN_ENABLED=true

# RabbitMQ Configuration
RABBITMQ_EXCHANGE=video.exchange
RABBITMQ_QUEUE_ENCODING=video.encoding.queue
RABBITMQ_QUEUE_STREAMING=video.streaming.queue
RABBITMQ_ROUTING_KEY_ENCODING=video.encoding
RABBITMQ_ROUTING_KEY_STREAMING=video.streaming
```

## ✅ Checklist

- [ ] Encoding service successfully started
- [ ] Streaming service successfully started  
- [ ] RabbitMQ queues created
- [ ] CloudFront configured
- [ ] Video uploaded via upload service
- [ ] Encoding job completed successfully
- [ ] Qualities appeared in streaming DB
- [ ] API returns correct HLS URLs
- [ ] CDN URLs work correctly

## 🚨 Troubleshooting

### Qualities not appearing in streaming:
1. Check encoding service logs for RabbitMQ sending errors
2. Check RabbitMQ queue `video.streaming.queue`
3. Check streaming service logs for message reception

### HLS playlists unavailable:
1. Check S3 bucket and permissions
2. Check CloudFront distribution  
3. Check URLs in streaming service DB

### CDN URLs not working:
1. Check `CLOUDFRONT_DOMAIN` in configuration
2. Check `CDN_ENABLED=true`
3. Check CloudFront origin settings

## 🎯 Result

Now the streaming service is fully adapted to AWS S3 file structure and works correctly with:

- ✅ Individual HLS playlists for each quality
- ✅ Automatic quality updates via RabbitMQ  
- ✅ CloudFront CDN integration
- ✅ Correct S3 URL structures
- ✅ Full adaptive streaming support

MVP functionality is fully implemented! 🎉 
