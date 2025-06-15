import React, { useState, useRef, useCallback } from 'react';
import uploadService from '../../services/uploadService';
import ProgressBar from './ProgressBar';
import FileDropZone from './FileDropZone';
import UploadStatus from './UploadStatus';

const VideoUpload = ({ onUploadComplete, onUploadError }) => {
  const [file, setFile] = useState(null);
  const [metadata, setMetadata] = useState({ title: '', description: '' });
  const [uploading, setUploading] = useState(false);
  const [progress, setProgress] = useState(0);
  const [chunkProgress, setChunkProgress] = useState({ current: 0, total: 0, chunkPercent: 0 });
  const [uploadResult, setUploadResult] = useState(null);
  const [error, setError] = useState(null);
  const [uploadId, setUploadId] = useState(null);
  const fileInputRef = useRef(null);

  // Supported video formats
  const SUPPORTED_FORMATS = [
    'video/mp4',
    'video/avi', 
    'video/mov',
    'video/wmv',
    'video/flv',
    'video/webm',
    'video/mkv',
    'video/m4v'
  ];

  const MAX_FILE_SIZE = 2 * 1024 * 1024 * 1024; // 2GB

  // Format file size for display
  const formatFileSize = (bytes) => {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  };

  // Handle file selection
  const handleFileSelect = useCallback((selectedFile) => {
    // Validate file
    const validateFile = (selectedFile) => {
      if (!selectedFile) {
        return 'Please select a file';
      }

      if (!SUPPORTED_FORMATS.includes(selectedFile.type)) {
        return `Unsupported file format. Supported formats: ${SUPPORTED_FORMATS.join(', ')}`;
      }

      if (selectedFile.size > MAX_FILE_SIZE) {
        return `File too large. Maximum size: ${formatFileSize(MAX_FILE_SIZE)}`;
      }

      return null;
    };

    const validationError = validateFile(selectedFile);
    if (validationError) {
      setError(validationError);
      return;
    }

    setFile(selectedFile);
    setError(null);
    setUploadResult(null);
    setProgress(0);
    setChunkProgress({ current: 0, total: 0, chunkPercent: 0 });

    // Auto-generate title from filename if not set
    if (!metadata.title && selectedFile.name) {
      const nameWithoutExtension = selectedFile.name.replace(/\.[^/.]+$/, '');
      setMetadata(prev => ({ ...prev, title: nameWithoutExtension }));
    }
  }, [metadata.title]);

  // Handle drag & drop
  const handleDrop = useCallback((e) => {
    e.preventDefault();
    const droppedFile = e.dataTransfer.files[0];
    if (droppedFile) {
      handleFileSelect(droppedFile);
    }
  }, [handleFileSelect]);

  // Handle file input change
  const handleFileInputChange = (e) => {
    const selectedFile = e.target.files[0];
    if (selectedFile) {
      handleFileSelect(selectedFile);
    }
  };

  // Handle metadata change
  const handleMetadataChange = (field, value) => {
    setMetadata(prev => ({ ...prev, [field]: value }));
  };

  // Handle upload progress
  const handleProgress = useCallback((progressPercent) => {
    setProgress(progressPercent);
  }, []);

  // Handle chunk progress
  const handleChunkProgress = useCallback((chunkNumber, chunkPercent, totalChunks) => {
    setChunkProgress({
      current: chunkNumber,
      total: totalChunks,
      chunkPercent: chunkPercent
    });
  }, []);

  // Handle upload ID received
  const handleUploadIdReceived = useCallback((receivedUploadId) => {
    console.log('Upload ID received:', receivedUploadId);
    setUploadId(receivedUploadId);
  }, []);

  // Start upload
  const handleUpload = async () => {
    if (!file) {
      setError('Please select a file');
      return;
    }

    if (!metadata.title.trim()) {
      setError('Please enter a title');
      return;
    }

    setUploading(true);
    setError(null);
    setUploadResult(null);
    setProgress(0);

    try {
      console.log('Starting upload:', file.name, formatFileSize(file.size));
      
      const result = await uploadService.uploadVideo(
        file,
        metadata,
        handleProgress,
        handleChunkProgress,
        handleUploadIdReceived
      );

      console.log('Upload completed:', result);
      setUploadResult(result);
      setProgress(100);
      
      if (onUploadComplete) {
        onUploadComplete(result);
      }

    } catch (err) {
      console.error('Upload failed:', err);
      setError(err.message || 'Upload failed');
      
      if (onUploadError) {
        onUploadError(err);
      }
    } finally {
      setUploading(false);
    }
  };

  // Cancel upload
  const handleCancel = async () => {
    if (uploadId && uploading) {
      try {
        await uploadService.cancelUpload(uploadId);
        console.log('Upload cancelled');
      } catch (err) {
        console.error('Failed to cancel upload:', err);
      }
    }
    
    setUploading(false);
    setProgress(0);
    setChunkProgress({ current: 0, total: 0, chunkPercent: 0 });
    setError(null);
  };

  // Reset form
  const handleReset = () => {
    setFile(null);
    setMetadata({ title: '', description: '' });
    setUploading(false);
    setProgress(0);
    setChunkProgress({ current: 0, total: 0, chunkPercent: 0 });
    setUploadResult(null);
    setError(null);
    setUploadId(null);
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  };

  return (
    <div style={{
      maxWidth: '800px',
      margin: '0 auto',
      padding: '20px',
      fontFamily: '"Segoe UI", Tahoma, Geneva, Verdana, sans-serif'
    }}>
      <h1 style={{ 
        color: '#d32f2f', 
        textAlign: 'center', 
        marginBottom: '30px',
        fontSize: '28px'
      }}>
        Upload Video
      </h1>

      {/* File Selection */}
      {!file ? (
        <FileDropZone
          onDrop={handleDrop}
          onFileSelect={handleFileSelect}
          fileInputRef={fileInputRef}
          onFileInputChange={handleFileInputChange}
          supportedFormats={SUPPORTED_FORMATS}
          maxFileSize={MAX_FILE_SIZE}
        />
      ) : (
        /* File Info & Metadata */
        <div style={{
          border: '2px solid #e0e0e0',
          borderRadius: '8px',
          padding: '20px',
          marginBottom: '20px',
          backgroundColor: 'white'
        }}>
          {/* File Info */}
          <div style={{
            display: 'flex',
            alignItems: 'center',
            marginBottom: '20px',
            padding: '15px',
            backgroundColor: '#f5f5f5',
            borderRadius: '6px'
          }}>
            <div style={{ flex: 1 }}>
              <div style={{ fontWeight: 'bold', color: '#333', marginBottom: '5px' }}>
                📹 {file.name}
              </div>
              <div style={{ color: '#666', fontSize: '14px' }}>
                Size: {formatFileSize(file.size)} • Type: {file.type}
              </div>
            </div>
            {!uploading && (
              <button
                onClick={handleReset}
                style={{
                  background: 'none',
                  border: 'none',
                  color: '#d32f2f',
                  cursor: 'pointer',
                  fontSize: '18px',
                  padding: '5px'
                }}
                title="Remove file"
              >
                ✕
              </button>
            )}
          </div>

          {/* Metadata Form */}
          <div style={{ marginBottom: '20px' }}>
            <div style={{ marginBottom: '15px' }}>
              <label style={{
                display: 'block',
                marginBottom: '5px',
                fontWeight: 'bold',
                color: '#333'
              }}>
                Title *
              </label>
              <input
                type="text"
                value={metadata.title}
                onChange={(e) => handleMetadataChange('title', e.target.value)}
                disabled={uploading}
                placeholder="Enter video title"
                style={{
                  width: '100%',
                  padding: '10px',
                  border: '1px solid #ddd',
                  borderRadius: '4px',
                  fontSize: '14px',
                  backgroundColor: uploading ? '#f5f5f5' : 'white'
                }}
              />
            </div>

            <div style={{ marginBottom: '20px' }}>
              <label style={{
                display: 'block',
                marginBottom: '5px',
                fontWeight: 'bold',
                color: '#333'
              }}>
                Description
              </label>
              <textarea
                value={metadata.description}
                onChange={(e) => handleMetadataChange('description', e.target.value)}
                disabled={uploading}
                placeholder="Enter video description (optional)"
                rows="3"
                style={{
                  width: '100%',
                  padding: '10px',
                  border: '1px solid #ddd',
                  borderRadius: '4px',
                  fontSize: '14px',
                  resize: 'vertical',
                  backgroundColor: uploading ? '#f5f5f5' : 'white'
                }}
              />
            </div>
          </div>

          {/* Upload Controls */}
          <div style={{
            display: 'flex',
            gap: '10px',
            justifyContent: 'center'
          }}>
            {!uploading ? (
              <>
                <button
                  onClick={handleUpload}
                  disabled={!metadata.title.trim()}
                  style={{
                    backgroundColor: metadata.title.trim() ? '#d32f2f' : '#ccc',
                    color: 'white',
                    border: 'none',
                    padding: '12px 24px',
                    borderRadius: '6px',
                    fontSize: '16px',
                    fontWeight: 'bold',
                    cursor: metadata.title.trim() ? 'pointer' : 'not-allowed',
                    transition: 'background-color 0.2s'
                  }}
                >
                  🚀 Start Upload
                </button>
                <button
                  onClick={handleReset}
                  style={{
                    backgroundColor: 'transparent',
                    color: '#666',
                    border: '2px solid #ddd',
                    padding: '12px 24px',
                    borderRadius: '6px',
                    fontSize: '16px',
                    cursor: 'pointer',
                    transition: 'all 0.2s'
                  }}
                >
                  Clear
                </button>
              </>
            ) : (
              <button
                onClick={handleCancel}
                style={{
                  backgroundColor: '#f44336',
                  color: 'white',
                  border: 'none',
                  padding: '12px 24px',
                  borderRadius: '6px',
                  fontSize: '16px',
                  cursor: 'pointer'
                }}
              >
                ⏹ Cancel Upload
              </button>
            )}
          </div>
        </div>
      )}

      {/* Progress */}
      {uploading && (
        <ProgressBar
          progress={progress}
          chunkProgress={chunkProgress}
          fileName={file?.name}
        />
      )}

      {/* Status Messages */}
      <UploadStatus
        error={error}
        uploadResult={uploadResult}
        uploading={uploading}
        onReset={handleReset}
      />
    </div>
  );
};

export default VideoUpload; 