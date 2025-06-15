import authService from './authService';

const API_BASE_URL = process.env.NODE_ENV === 'development' 
  ? 'http://localhost:8080' 
  : window.location.origin;

class UploadService {
  constructor() {
    this.activeUploads = new Map(); // Track active uploads
  }

  /**
   * Get all user videos
   */
  async getUserVideos() {
    const response = await authService.apiRequest(`${API_BASE_URL}/api/upload/videos`);
    return await response.json();
  }

  /**
   * Get specific video by ID
   */
  async getVideo(videoId) {
    const response = await authService.apiRequest(`${API_BASE_URL}/api/upload/video/${videoId}`);
    return await response.json();
  }

  /**
   * Delete video (soft delete)
   */
  async deleteVideo(videoId) {
    const response = await authService.apiRequest(
      `${API_BASE_URL}/api/upload/video/${videoId}`,
      { method: 'DELETE' }
    );
    return await response.json();
  }

  /**
   * Upload small video file (< 5MB) using regular upload
   */
  async uploadSmallVideo(file, metadata, onProgress) {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('title', metadata.title);
    formData.append('description', metadata.description || '');

    const token = authService.getAccessToken();
    if (!token) {
      throw new Error('No access token available');
    }

    return new Promise((resolve, reject) => {
      const xhr = new XMLHttpRequest();

      xhr.upload.addEventListener('progress', (event) => {
        if (event.lengthComputable && onProgress) {
          const progress = Math.round((event.loaded / event.total) * 100);
          onProgress(progress);
        }
      });

      xhr.addEventListener('load', () => {
        if (xhr.status === 200) {
          resolve(JSON.parse(xhr.responseText));
        } else {
          reject(new Error(`Upload failed: ${xhr.status} ${xhr.statusText}`));
        }
      });

      xhr.addEventListener('error', () => {
        reject(new Error('Upload failed: Network error'));
      });

      xhr.open('POST', `${API_BASE_URL}/api/upload/video`);
      xhr.setRequestHeader('Authorization', `Bearer ${token}`);
      xhr.send(formData);
    });
  }

  /**
   * Initiate multipart upload for large files (>= 5MB)
   */
  async initiateMultipartUpload(file, metadata) {
    const request = {
      title: metadata.title,
      description: metadata.description || '',
      originalFilename: file.name,
      fileSize: file.size,
      mimeType: file.type
    };

    const response = await authService.apiRequest(
      `${API_BASE_URL}/api/upload/multipart/initiate`,
      {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(request)
      }
    );

    // Parse JSON from response
    const jsonResponse = await response.json();
    console.log('Initiate response:', jsonResponse);
    return jsonResponse;
  }

  /**
   * Upload single chunk
   */
  async uploadChunk(uploadId, partNumber, chunk, onProgress) {
    console.log('uploadChunk called with:', { uploadId, partNumber, chunkSize: chunk.size });
    
    const formData = new FormData();
    formData.append('chunk', chunk);
    formData.append('uploadId', uploadId);
    formData.append('partNumber', partNumber.toString());

    // Debug: log FormData contents
    console.log('FormData contents:');
    for (let [key, value] of formData.entries()) {
      console.log(`  ${key}:`, value);
    }

    const token = authService.getAccessToken();
    if (!token) {
      throw new Error('No access token available');
    }

    return new Promise((resolve, reject) => {
      const xhr = new XMLHttpRequest();

      xhr.upload.addEventListener('progress', (event) => {
        if (event.lengthComputable && onProgress) {
          const progress = Math.round((event.loaded / event.total) * 100);
          onProgress(progress);
        }
      });

      xhr.addEventListener('load', () => {
        console.log('XHR Response:', {
          status: xhr.status,
          statusText: xhr.statusText,
          responseText: xhr.responseText,
          headers: xhr.getAllResponseHeaders()
        });
        
        if (xhr.status === 200) {
          try {
            const response = JSON.parse(xhr.responseText);
            resolve(response);
          } catch (parseError) {
            console.error('Failed to parse response JSON:', parseError);
            reject(new Error(`Invalid JSON response: ${xhr.responseText}`));
          }
        } else {
          let errorMessage = `Chunk upload failed: ${xhr.status} ${xhr.statusText}`;
          try {
            const errorResponse = JSON.parse(xhr.responseText);
            if (errorResponse.message) {
              errorMessage = errorResponse.message;
            }
          } catch (e) {
            // Use default error message if JSON parsing fails
          }
          reject(new Error(errorMessage));
        }
      });

      xhr.addEventListener('error', () => {
        reject(new Error('Chunk upload failed: Network error'));
      });

      xhr.open('POST', `${API_BASE_URL}/api/upload/multipart/upload-chunk`);
      xhr.setRequestHeader('Authorization', `Bearer ${token}`);
      xhr.send(formData);
    });
  }

  /**
   * Complete multipart upload
   */
  async completeMultipartUpload(uploadId) {
    const response = await authService.apiRequest(
      `${API_BASE_URL}/api/upload/multipart/complete/${uploadId}`,
      { method: 'POST' }
    );
    return await response.json();
  }

  /**
   * Abort multipart upload
   */
  async abortMultipartUpload(uploadId) {
    const response = await authService.apiRequest(
      `${API_BASE_URL}/api/upload/multipart/abort/${uploadId}`,
      { method: 'DELETE' }
    );
    return await response.text(); // This returns a string message
  }

  /**
   * Get upload status
   */
  async getUploadStatus(uploadId) {
    const response = await authService.apiRequest(
      `${API_BASE_URL}/api/upload/multipart/status/${uploadId}`
    );
    return await response.json();
  }

  /**
   * Upload large file with chunked multipart upload
   */
  async uploadLargeVideo(file, metadata, onProgress, onChunkProgress, onUploadIdReceived) {
    const CHUNK_SIZE = 5 * 1024 * 1024; // 5MB chunks
    const totalChunks = Math.ceil(file.size / CHUNK_SIZE);

    // Initialize upload
    const initResponse = await this.initiateMultipartUpload(file, metadata);
    const uploadId = initResponse.uploadId;

    // Notify component about uploadId
    if (onUploadIdReceived) {
      onUploadIdReceived(uploadId);
    }

    // Track this upload
    const uploadInfo = {
      uploadId,
      file,
      metadata,
      totalChunks,
      uploadedChunks: 0,
      startTime: Date.now(),
      aborted: false
    };
    
    this.activeUploads.set(uploadId, uploadInfo);

    try {
      // Upload chunks sequentially for simplicity
      for (let chunkNumber = 1; chunkNumber <= totalChunks; chunkNumber++) {
        // Check if upload was aborted
        if (uploadInfo.aborted) {
          throw new Error('Upload aborted');
        }

        const start = (chunkNumber - 1) * CHUNK_SIZE;
        const end = Math.min(start + CHUNK_SIZE, file.size);
        const chunk = file.slice(start, end);

        await this.uploadChunk(
          uploadId,
          chunkNumber,
          chunk,
          (chunkProgress) => {
            if (onChunkProgress) {
              onChunkProgress(chunkNumber, chunkProgress, totalChunks);
            }
          }
        );

        uploadInfo.uploadedChunks = chunkNumber;

        // Update overall progress
        if (onProgress) {
          const overallProgress = Math.round((chunkNumber / totalChunks) * 100);
          onProgress(overallProgress);
        }
      }

      // Complete upload
      const completeResponse = await this.completeMultipartUpload(uploadId);
      
      // Clean up tracking
      this.activeUploads.delete(uploadId);
      
      return completeResponse;

    } catch (error) {
      // Cleanup on error
      try {
        await this.abortMultipartUpload(uploadId);
      } catch (abortError) {
        console.error('Failed to abort upload:', abortError);
      }
      
      this.activeUploads.delete(uploadId);
      throw error;
    }
  }

  /**
   * Cancel active upload
   */
  async cancelUpload(uploadId) {
    const uploadInfo = this.activeUploads.get(uploadId);
    if (uploadInfo) {
      uploadInfo.aborted = true;
      await this.abortMultipartUpload(uploadId);
      this.activeUploads.delete(uploadId);
    }
  }

  /**
   * Get active uploads info
   */
  getActiveUploads() {
    return Array.from(this.activeUploads.values());
  }

  /**
   * Smart upload that chooses between regular and multipart based on file size
   */
  async uploadVideo(file, metadata, onProgress, onChunkProgress, onUploadIdReceived) {
    const MIN_MULTIPART_SIZE = 5 * 1024 * 1024; // 5MB

    if (file.size < MIN_MULTIPART_SIZE) {
      console.log('Using regular upload for small file');
      return this.uploadSmallVideo(file, metadata, onProgress);
    } else {
      console.log('Using multipart upload for large file');
      return this.uploadLargeVideo(file, metadata, onProgress, onChunkProgress, onUploadIdReceived);
    }
  }
}

const uploadService = new UploadService();
export default uploadService; 