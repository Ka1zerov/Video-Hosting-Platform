import React, { useState } from 'react';

const FileDropZone = ({ 
  onDrop, 
  onFileSelect, 
  fileInputRef, 
  onFileInputChange, 
  supportedFormats, 
  maxFileSize 
}) => {
  const [isDragOver, setIsDragOver] = useState(false);

  // Format file size for display
  const formatFileSize = (bytes) => {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  };

  // Handle drag events
  const handleDragOver = (e) => {
    e.preventDefault();
    setIsDragOver(true);
  };

  const handleDragLeave = (e) => {
    e.preventDefault();
    setIsDragOver(false);
  };

  const handleDrop = (e) => {
    e.preventDefault();
    setIsDragOver(false);
    onDrop(e);
  };

  // Handle click to open file selector
  const handleClick = () => {
    if (fileInputRef.current) {
      fileInputRef.current.click();
    }
  };

  return (
    <div
      onClick={handleClick}
      onDragOver={handleDragOver}
      onDragLeave={handleDragLeave}
      onDrop={handleDrop}
      style={{
        border: `3px dashed ${isDragOver ? '#d32f2f' : '#ddd'}`,
        borderRadius: '12px',
        padding: '60px 40px',
        textAlign: 'center',
        cursor: 'pointer',
        backgroundColor: isDragOver ? '#fff5f5' : '#fafafa',
        transition: 'all 0.3s ease',
        marginBottom: '20px',
        position: 'relative'
      }}
    >
      {/* Hidden file input */}
      <input
        ref={fileInputRef}
        type="file"
        accept={supportedFormats.join(',')}
        onChange={onFileInputChange}
        style={{ display: 'none' }}
      />

      {/* Upload Icon */}
      <div style={{
        fontSize: '64px',
        color: isDragOver ? '#d32f2f' : '#ccc',
        marginBottom: '20px',
        transition: 'color 0.3s ease'
      }}>
        📁
      </div>

      {/* Main Text */}
      <h3 style={{
        color: isDragOver ? '#d32f2f' : '#333',
        marginBottom: '10px',
        fontSize: '24px',
        fontWeight: 'bold'
      }}>
        {isDragOver ? 'Drop your video here!' : 'Choose or drop your video'}
      </h3>

      <p style={{
        color: '#666',
        fontSize: '16px',
        marginBottom: '20px',
        lineHeight: '1.5'
      }}>
        {isDragOver 
          ? 'Release to select the file'
          : 'Click here to browse or drag and drop your video file'
        }
      </p>

      {/* File Requirements */}
      <div style={{
        backgroundColor: 'white',
        border: '1px solid #e0e0e0',
        borderRadius: '8px',
        padding: '20px',
        textAlign: 'left',
        maxWidth: '500px',
        margin: '0 auto'
      }}>
        <h4 style={{
          color: '#333',
          marginBottom: '15px',
          fontSize: '16px',
          fontWeight: 'bold'
        }}>
          📋 Requirements:
        </h4>

        <div style={{ marginBottom: '12px' }}>
          <strong style={{ color: '#333' }}>Maximum size:</strong>
          <span style={{ color: '#666', marginLeft: '8px' }}>
            {formatFileSize(maxFileSize)}
          </span>
        </div>

        <div style={{ marginBottom: '12px' }}>
          <strong style={{ color: '#333' }}>Supported formats:</strong>
        </div>

        <div style={{
          display: 'flex',
          flexWrap: 'wrap',
          gap: '6px',
          marginTop: '8px'
        }}>
          {supportedFormats.map((format) => (
            <span
              key={format}
              style={{
                backgroundColor: '#f5f5f5',
                color: '#666',
                padding: '4px 8px',
                borderRadius: '4px',
                fontSize: '12px',
                fontFamily: 'monospace'
              }}
            >
              {format.replace('video/', '').toUpperCase()}
            </span>
          ))}
        </div>

        <div style={{
          marginTop: '15px',
          padding: '10px',
          backgroundColor: '#fff3cd',
          borderRadius: '6px',
          border: '1px solid #ffeaa7'
        }}>
          <div style={{ fontSize: '14px', color: '#856404' }}>
            💡 <strong>Tip:</strong> Files ≥5MB will use chunked upload for better reliability
          </div>
        </div>
      </div>

      {/* Browse Button */}
      <button
        style={{
          marginTop: '20px',
          backgroundColor: '#d32f2f',
          color: 'white',
          border: 'none',
          padding: '12px 24px',
          borderRadius: '6px',
          fontSize: '16px',
          fontWeight: 'bold',
          cursor: 'pointer',
          transition: 'background-color 0.2s'
        }}
        onMouseOver={(e) => {
          e.target.style.backgroundColor = '#b71c1c';
        }}
        onMouseOut={(e) => {
          e.target.style.backgroundColor = '#d32f2f';
        }}
      >
        Browse Files
      </button>
    </div>
  );
};

export default FileDropZone; 