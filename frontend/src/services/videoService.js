import authService from './authService';

// Base configuration - все запросы идут через Gateway
const GATEWAY_URL = process.env.NODE_ENV === 'development' 
  ? 'http://localhost:8080' 
  : window.location.origin;

/**
 * Video service for interacting with metadata service through Gateway
 */
class VideoService {
  
  /**
   * Get all videos with pagination
   */
  async getAllVideos(page = 0, size = 12) {
    try {
      const response = await authService.apiRequest(
        `${GATEWAY_URL}/api/metadata/videos?page=${page}&size=${size}`
      );
      return await response.json();
    } catch (error) {
      console.error('Failed to fetch videos:', error);
      throw this.handleError(error);
    }
  }

  /**
   * Search videos by query
   */
  async searchVideos(query, page = 0, size = 12) {
    try {
      const encodedQuery = encodeURIComponent(query);
      const response = await authService.apiRequest(
        `${GATEWAY_URL}/api/metadata/videos/search?query=${encodedQuery}&page=${page}&size=${size}`
      );
      return await response.json();
    } catch (error) {
      console.error('Failed to search videos:', error);
      throw this.handleError(error);
    }
  }

  /**
   * Get popular videos
   */
  async getPopularVideos(page = 0, size = 12) {
    try {
      const response = await authService.apiRequest(
        `${GATEWAY_URL}/api/metadata/videos/popular?page=${page}&size=${size}`
      );
      return await response.json();
    } catch (error) {
      console.error('Failed to fetch popular videos:', error);
      throw this.handleError(error);
    }
  }

  /**
   * Get recently watched videos
   */
  async getRecentVideos(page = 0, size = 12) {
    try {
      const response = await authService.apiRequest(
        `${GATEWAY_URL}/api/metadata/videos/recent?page=${page}&size=${size}`
      );
      return await response.json();
    } catch (error) {
      console.error('Failed to fetch recent videos:', error);
      throw this.handleError(error);
    }
  }

  /**
   * Get video by ID
   */
  async getVideoById(videoId) {
    try {
      const response = await authService.apiRequest(
        `${GATEWAY_URL}/api/metadata/videos/${videoId}`
      );
      return await response.json();
    } catch (error) {
      console.error('Failed to fetch video:', error);
      throw this.handleError(error);
    }
  }

  /**
   * Format duration in seconds to human readable format
   */
  formatDuration(durationInSeconds) {
    if (!durationInSeconds || durationInSeconds <= 0) {
      return '0:00';
    }

    const hours = Math.floor(durationInSeconds / 3600);
    const minutes = Math.floor((durationInSeconds % 3600) / 60);
    const seconds = durationInSeconds % 60;

    if (hours > 0) {
      return `${hours}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;
    } else {
      return `${minutes}:${seconds.toString().padStart(2, '0')}`;
    }
  }

  /**
   * Format upload date to human readable format
   */
  formatUploadDate(uploadedAt) {
    try {
      const date = new Date(uploadedAt);
      const now = new Date();
      const diffMs = now - date;
      const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));

      if (diffDays === 0) {
        return 'Today';
      } else if (diffDays === 1) {
        return 'Yesterday';
      } else if (diffDays < 7) {
        return `${diffDays} days ago`;
      } else if (diffDays < 30) {
        const weeks = Math.floor(diffDays / 7);
        return weeks === 1 ? '1 week ago' : `${weeks} weeks ago`;
      } else if (diffDays < 365) {
        const months = Math.floor(diffDays / 30);
        return months === 1 ? '1 month ago' : `${months} months ago`;
      } else {
        const years = Math.floor(diffDays / 365);
        return years === 1 ? '1 year ago' : `${years} years ago`;
      }
    } catch (error) {
      return 'Unknown';
    }
  }

  /**
   * Get preferred thumbnail URL (CDN if available, fallback to S3)
   */
  getThumbnailUrl(video) {
    if (video.cdnUrls && video.cdnUrls.cdnEnabled && video.cdnUrls.thumbnailUrl) {
      return video.cdnUrls.thumbnailUrl;
    }
    return video.thumbnailUrl;
  }

  /**
   * Get video stream information for playback
   */
  async getVideoStreamInfo(videoId) {
    try {
      const playbackRequest = {
        videoId: videoId,
        format: 'hls'
      };

      const response = await authService.apiRequest(
        `${GATEWAY_URL}/api/streaming/play`,
        {
          method: 'POST',
          body: JSON.stringify(playbackRequest)
        }
      );
      return await response.json();
    } catch (error) {
      console.error('Failed to get video stream info:', error);
      throw this.handleError(error);
    }
  }

  /**
   * Get master playlist content with JWT authentication and create blob URL
   */
  async getMasterPlaylistBlob(videoId, expirationHours = 2) {
    try {
      const response = await authService.apiRequest(
        `${GATEWAY_URL}/api/streaming/playlist/${videoId}/master.m3u8?expirationHours=${expirationHours}`
      );
      
      if (!response.ok) {
        throw new Error(`Failed to get playlist: ${response.status} ${response.statusText}`);
      }
      
      const playlistContent = await response.text();
      const blob = new Blob([playlistContent], { type: 'application/vnd.apple.mpegurl' });
      const blobUrl = URL.createObjectURL(blob);
      
      console.log('Created master playlist blob URL:', blobUrl);
      return blobUrl;
    } catch (error) {
      console.error('Failed to get master playlist:', error);
      throw this.handleError(error);
    }
  }

  /**
   * Cleanup blob URL
   */
  cleanupBlobUrl(blobUrl) {
    if (blobUrl && blobUrl.startsWith('blob:')) {
      URL.revokeObjectURL(blobUrl);
      console.log('Cleaned up blob URL:', blobUrl);
    }
  }

  /**
   * Get master playlist URL for HLS streaming (deprecated - use getMasterPlaylistBlob)
   */
  getMasterPlaylistUrl(videoId, expirationHours = 2) {
    return `${GATEWAY_URL}/api/streaming/playlist/${videoId}/master.m3u8?expirationHours=${expirationHours}`;
  }

  /**
   * Handle API errors
   */
  handleError(error) {
    if (error.response) {
      // Server responded with error status
      const status = error.response.status;
      const message = error.response.data?.message || error.response.statusText || 'Unknown server error';
      
      switch (status) {
        case 404:
          return new Error('Videos not found');
        case 500:
          return new Error('Server error occurred');
        default:
          return new Error(`Error ${status}: ${message}`);
      }
    } else if (error.message) {
      // Error from authService or fetch
      return new Error(error.message);
    } else {
      // Something else happened
      return new Error('An unexpected error occurred');
    }
  }
}

// Export singleton instance
const videoService = new VideoService();
export default videoService; 