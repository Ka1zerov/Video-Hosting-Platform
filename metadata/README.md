# Metadata Service

Metadata Service handles extended video metadata, search, analytics, and user interactions as part of the Video Hosting Platform.

## 🏗️ Shared Database Architecture

### PostgreSQL Shared with Upload Service

```
┌─────────────────┐    ┌─────────────────┐
│   Upload        │    │   Metadata      │
│   Service       │    │   Service       │
│   Port: 8082    │    │   Port: 8083    │
└─────────┬───────┘    └─────────┬───────┘
          │                      │
          │   ┌─────────────────┐ │
          └───┤  PostgreSQL     ├─┘
              │ video_platform  │
              │                 │
              │ UPLOAD TABLES:  │
              │ • videos        │ ← Main video table
              │                 │
              │ METADATA TABLES:│
              │ • video_metadata│ ← Extended metadata
              │ • video_tags    │ ← Search tags
              │ • categories    │ ← Categories
              │ • playlists     │ ← Playlists
              │ • video_analytics│ ← Analytics
              │ • video_views   │ ← View tracking
              │ • video_ratings │ ← Ratings
              │ • video_comments│ ← Comments
              │ • video_search_view│ ← Materialized view
              └─────────────────┘
```

### ⚡ Shared Database Benefits:

1. **ACID transactions** between services
2. **JOIN queries** without network calls
3. **Referential integrity** through Foreign Keys
4. **Shared migrations** via Liquibase
5. **Simple deployment** for local development

### 🔗 Table Relationships:

```sql
-- All metadata tables reference videos.id
video_metadata.video_id → videos.id
video_tags.video_id → videos.id
video_analytics.video_id → videos.id
video_views.video_id → videos.id
video_ratings.video_id → videos.id
video_comments.video_id → videos.id

-- Cascade delete: when video is deleted, 
-- all related metadata is automatically deleted
```

## 🛠️ **Liquibase Contexts Solution**

### Problem:
When using a shared database with two services, conflicts arise in the `DATABASECHANGELOG` table:
- Identical changeset IDs in different services
- Conflicts during simultaneous service startup
- Uncertainty about change sources

### Solution: Liquibase Contexts
```yaml
# Upload Service (application.yml)
spring:
  liquibase:
    contexts: upload-service

# Metadata Service (application.yml)  
spring:
  liquibase:
    contexts: metadata-service
```

### Changesets with contexts:
```sql
-- Upload Service
--changeset TymofiiSkrypko:upload-create-videos-table context:upload-service

-- Metadata Service  
--changeset TymofiiSkrypko:metadata-create-video-tags-table context:metadata-service
```

### Result in DATABASECHANGELOG:
```sql
SELECT id, filename, contexts FROM databasechangelog;

upload-create-videos-table          | v1.0-video-changelog.sql      | upload-service
metadata-create-video-tags-table    | v1.0-metadata-tables.sql      | metadata-service
```

### ✅ Benefits:
1. **Migration isolation** - each service executes only its changesets
2. **Unique IDs** - conflict avoidance through `upload-` and `metadata-` prefixes
3. **Safe parallel startup** - services don't interfere with each other
4. **Traceability** - clear visibility of which service made changes

## 🎯 Functionality

### 📊 **Metadata**
- Extended video information (duration, thumbnails, language)
- Privacy levels (PUBLIC, UNLISTED, PRIVATE)
- Age restrictions
- Geolocation and monetization

### 🏷️ **Tag System**
- Multiple tags per video
- Automatic indexing for search
- Tag clouds

### 📁 **Categories**
- Hierarchical categories
- Multiple categories per video
- Pre-configured categories

### 🔍 **Search**
- PostgreSQL full-text search
- Materialized view for performance
- Weighted ranking (relevance + views + rating)
- Filtering by categories, users

### 📈 **Analytics**
- View, like, dislike, comment counters
- Detailed view analytics (IP, User-Agent, duration)
- Average ratings and reviews
- Comments with reply support

### 📋 **Playlists**
- User playlists
- Public and private playlists
- Ordered videos in playlists

## 📡 API Endpoints

### Video Metadata
```http
GET    /api/metadata/videos/{videoId}           # Get metadata
PUT    /api/metadata/videos/{videoId}           # Update metadata
POST   /api/metadata/videos/{videoId}/tags      # Add tags
DELETE /api/metadata/videos/{videoId}/tags/{tag} # Remove tag
```

### Search
```http
GET /api/metadata/search?q={query}&category={cat}&page={page}
GET /api/metadata/search/suggestions?q={query}
```

### Categories
```http
GET    /api/metadata/categories                 # All categories
POST   /api/metadata/categories                 # Create category
GET    /api/metadata/categories/{id}/videos     # Category videos
```

### Playlists
```http
GET    /api/metadata/playlists                  # User playlists
POST   /api/metadata/playlists                  # Create playlist
PUT    /api/metadata/playlists/{id}/videos      # Add video
```

### Analytics
```http
POST   /api/metadata/videos/{videoId}/view      # Record view
POST   /api/metadata/videos/{videoId}/rate      # Rate video
GET    /api/metadata/videos/{videoId}/analytics # Get analytics
```

## 🚀 Getting Started

### Development
```bash
# Start infrastructure (PostgreSQL, Redis)
cd ../upload
./scripts/docker-dev.sh start

# Start upload service (creates videos table)
cd ../upload
./gradlew bootRun

# Start metadata service (creates metadata tables)
cd ../metadata
./gradlew bootRun
```

Service will be available on port 8083.

**Important**: Upload service must start first to create the `videos` table that metadata foreign keys reference.

## 🔄 Service Integration

### Upload → Metadata (RabbitMQ)
```java
// Upload Service sends event after upload
{
  "eventType": "video.uploaded",
  "videoId": "uuid",
  "userId": "user123",
  "title": "Video Title",
  "description": "Description"
}

// Metadata Service creates search index
```

### Encoding → Metadata (RabbitMQ)
```java
// Encoding Service sends after processing
{
  "eventType": "video.processed",
  "videoId": "uuid",
  "thumbnailUrl": "s3://bucket/thumb.jpg",
  "durationSeconds": 300
}
```

## 🗄️ Database Migrations

Liquibase changelog structure:
- `v1.0-metadata-tables.sql` - Core metadata tables
- `v1.1-categories-table.sql` - Category system
- `v1.2-playlists-table.sql` - Playlists
- `v1.3-analytics-table.sql` - Analytics and comments
- `v1.4-search-indexes.sql` - Search indexes and functions

## ⚡ Performance

### Caching (Redis)
- Video metadata: 1 hour
- Search results: 10 minutes
- Categories: 2 hours

### PostgreSQL Indexes
- GIN indexes for full-text search
- Composite indexes for frequent queries
- Materialized view for complex JOINs

### Automatic Search Updates
- Database triggers update search_vector
- Periodic materialized view refresh
- Asynchronous indexing via RabbitMQ

## 🔒 Security

- Authentication via Gateway (X-User-Id header)
- User data isolation
- Rate limiting for search and API
- Input validation

## 📊 Monitoring

- Spring Actuator endpoints
- Prometheus metrics
- Operation logging
- Database and Redis health checks 