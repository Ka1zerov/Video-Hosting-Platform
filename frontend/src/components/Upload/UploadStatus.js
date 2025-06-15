import React from 'react';

const UploadStatus = ({ error, uploadResult, uploading, onReset }) => {
  // Don't render anything if there's no status to show
  if (!error && !uploadResult && !uploading) {
    return null;
  }

  // Error Message
  if (error) {
    return (
      <div style={{
        border: '2px solid #f44336',
        borderRadius: '8px',
        padding: '20px',
        backgroundColor: '#ffebee',
        marginBottom: '20px'
      }}>
        <div style={{
          display: 'flex',
          alignItems: 'flex-start',
          gap: '12px'
        }}>
          <div style={{
            fontSize: '24px',
            color: '#f44336',
            flexShrink: 0
          }}>
            ❌
          </div>
          <div style={{ flex: 1 }}>
            <h3 style={{
              color: '#f44336',
              margin: '0 0 8px 0',
              fontSize: '18px',
              fontWeight: 'bold'
            }}>
              Upload Failed
            </h3>
            <p style={{
              color: '#d32f2f',
              margin: '0 0 15px 0',
              fontSize: '14px',
              lineHeight: '1.4'
            }}>
              {error}
            </p>
            <button
              onClick={onReset}
              style={{
                backgroundColor: '#f44336',
                color: 'white',
                border: 'none',
                padding: '8px 16px',
                borderRadius: '4px',
                fontSize: '14px',
                cursor: 'pointer',
                fontWeight: 'bold'
              }}
            >
              Try Again
            </button>
          </div>
        </div>
      </div>
    );
  }

  // Success Message
  if (uploadResult && !uploading) {
    return (
      <div style={{
        border: '2px solid #4caf50',
        borderRadius: '8px',
        padding: '20px',
        backgroundColor: '#e8f5e8',
        marginBottom: '20px'
      }}>
        <div style={{
          display: 'flex',
          alignItems: 'flex-start',
          gap: '12px'
        }}>
          <div style={{
            fontSize: '24px',
            color: '#4caf50',
            flexShrink: 0
          }}>
            ✅
          </div>
          <div style={{ flex: 1 }}>
            <h3 style={{
              color: '#2e7d32',
              margin: '0 0 12px 0',
              fontSize: '18px',
              fontWeight: 'bold'
            }}>
              Upload Successful!
            </h3>
            
            {/* Upload Details */}
            <div style={{
              backgroundColor: 'white',
              border: '1px solid #c8e6c9',
              borderRadius: '6px',
              padding: '15px',
              marginBottom: '15px'
            }}>
              <div style={{
                display: 'grid',
                gridTemplateColumns: 'auto 1fr',
                gap: '8px 15px',
                fontSize: '14px'
              }}>
                <strong style={{ color: '#2e7d32' }}>Video ID:</strong>
                <span style={{ color: '#333', fontFamily: 'monospace', fontSize: '13px' }}>
                  {uploadResult.id}
                </span>
                
                <strong style={{ color: '#2e7d32' }}>Title:</strong>
                <span style={{ color: '#333' }}>
                  {uploadResult.title}
                </span>
                
                {uploadResult.description && (
                  <>
                    <strong style={{ color: '#2e7d32' }}>Description:</strong>
                    <span style={{ color: '#333' }}>
                      {uploadResult.description}
                    </span>
                  </>
                )}
                
                <strong style={{ color: '#2e7d32' }}>File:</strong>
                <span style={{ color: '#333' }}>
                  {uploadResult.originalFilename}
                </span>
                
                {uploadResult.fileSize && (
                  <>
                    <strong style={{ color: '#2e7d32' }}>Size:</strong>
                    <span style={{ color: '#333' }}>
                      {formatFileSize(uploadResult.fileSize)}
                    </span>
                  </>
                )}
                
                <strong style={{ color: '#2e7d32' }}>Status:</strong>
                <span style={{ 
                  color: uploadResult.status === 'UPLOADED' ? '#4caf50' : '#ff9800',
                  fontWeight: 'bold'
                }}>
                  {uploadResult.status}
                </span>
              </div>
            </div>

            {/* Next Steps Info */}
            <div style={{
              backgroundColor: '#fff3cd',
              border: '1px solid #ffeaa7',
              borderRadius: '6px',
              padding: '12px',
              marginBottom: '15px'
            }}>
              <div style={{ fontSize: '14px', color: '#856404' }}>
                <strong>🎬 What happens next?</strong>
                <ul style={{ margin: '8px 0 0 20px', paddingLeft: 0 }}>
                  <li>Your video is being processed and encoded into multiple qualities</li>
                  <li>This may take a few minutes depending on video length</li>
                  <li>You'll be able to watch it once processing is complete</li>
                </ul>
              </div>
            </div>

            {/* Action Buttons */}
            <div style={{
              display: 'flex',
              gap: '10px',
              flexWrap: 'wrap'
            }}>
              <button
                onClick={onReset}
                style={{
                  backgroundColor: '#4caf50',
                  color: 'white',
                  border: 'none',
                  padding: '10px 20px',
                  borderRadius: '6px',
                  fontSize: '14px',
                  cursor: 'pointer',
                  fontWeight: 'bold'
                }}
              >
                📤 Upload Another Video
              </button>
              
              <button
                onClick={() => {
                  // In a real app, this would navigate to the video page
                  window.open(`/video/${uploadResult.id}`, '_blank');
                }}
                style={{
                  backgroundColor: 'transparent',
                  color: '#4caf50',
                  border: '2px solid #4caf50',
                  padding: '10px 20px',
                  borderRadius: '6px',
                  fontSize: '14px',
                  cursor: 'pointer',
                  fontWeight: 'bold'
                }}
              >
                🎥 View Video
              </button>
            </div>
          </div>
        </div>
      </div>
    );
  }

  return null;
};

// Helper function to format file size
const formatFileSize = (bytes) => {
  if (bytes === 0) return '0 Bytes';
  const k = 1024;
  const sizes = ['Bytes', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
};

export default UploadStatus; 