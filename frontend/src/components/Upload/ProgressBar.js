import React from 'react';

const ProgressBar = ({ progress, chunkProgress, fileName }) => {
  const isMultipart = chunkProgress.total > 0;

  // Calculate estimated time remaining
  const getTimeEstimate = () => {
    if (progress <= 0) return 'Calculating...';
    
    // This is a simple estimation - in real app you'd track upload speed
    const remainingPercent = 100 - progress;
    const estimatedSeconds = Math.round((remainingPercent / progress) * 30); // Rough estimate
    
    if (estimatedSeconds < 60) {
      return `~${estimatedSeconds}s remaining`;
    } else {
      const minutes = Math.floor(estimatedSeconds / 60);
      const seconds = estimatedSeconds % 60;
      return `~${minutes}m ${seconds}s remaining`;
    }
  };

  return (
    <div style={{
      border: '2px solid #e0e0e0',
      borderRadius: '8px',
      padding: '20px',
      marginBottom: '20px',
      backgroundColor: 'white'
    }}>
      {/* Header */}
      <div style={{
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        marginBottom: '15px'
      }}>
        <h3 style={{
          margin: 0,
          color: '#333',
          fontSize: '18px'
        }}>
          📤 Uploading: {fileName}
        </h3>
        <div style={{
          color: '#666',
          fontSize: '14px'
        }}>
          {progress < 100 ? getTimeEstimate() : 'Finalizing...'}
        </div>
      </div>

      {/* Overall Progress */}
      <div style={{ marginBottom: isMultipart ? '20px' : '10px' }}>
        <div style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          marginBottom: '8px'
        }}>
          <span style={{ fontWeight: 'bold', color: '#333' }}>
            Overall Progress
          </span>
          <span style={{
            fontWeight: 'bold',
            color: progress === 100 ? '#4caf50' : '#d32f2f',
            fontSize: '16px'
          }}>
            {progress}%
          </span>
        </div>

        {/* Progress Bar */}
        <div style={{
          width: '100%',
          height: '12px',
          backgroundColor: '#f0f0f0',
          borderRadius: '6px',
          overflow: 'hidden',
          position: 'relative'
        }}>
          <div
            style={{
              width: `${progress}%`,
              height: '100%',
              backgroundColor: progress === 100 ? '#4caf50' : '#d32f2f',
              borderRadius: '6px',
              transition: 'width 0.3s ease, background-color 0.3s ease',
              position: 'relative'
            }}
          >
            {/* Animated shine effect for active progress */}
            {progress > 0 && progress < 100 && (
              <div
                style={{
                  position: 'absolute',
                  top: 0,
                  left: '-100%',
                  width: '100%',
                  height: '100%',
                  background: 'linear-gradient(90deg, transparent, rgba(255,255,255,0.3), transparent)',
                  animation: 'shine 2s infinite'
                }}
              />
            )}
          </div>
        </div>
      </div>

      {/* Chunk Progress (for multipart uploads) */}
      {isMultipart && (
        <div style={{
          padding: '15px',
          backgroundColor: '#f8f9fa',
          borderRadius: '6px',
          border: '1px solid #e9ecef'
        }}>
          <div style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            marginBottom: '8px'
          }}>
            <span style={{ fontWeight: 'bold', color: '#333', fontSize: '14px' }}>
              📦 Chunk Progress
            </span>
            <span style={{ color: '#666', fontSize: '14px' }}>
              {chunkProgress.current} of {chunkProgress.total} chunks
            </span>
          </div>

          {/* Current Chunk Progress */}
          <div style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            marginBottom: '6px'
          }}>
            <span style={{ color: '#666', fontSize: '13px' }}>
              Current Chunk #{chunkProgress.current}
            </span>
            <span style={{ color: '#666', fontSize: '13px', fontWeight: 'bold' }}>
              {chunkProgress.chunkPercent}%
            </span>
          </div>

          <div style={{
            width: '100%',
            height: '8px',
            backgroundColor: '#e9ecef',
            borderRadius: '4px',
            overflow: 'hidden'
          }}>
            <div
              style={{
                width: `${chunkProgress.chunkPercent}%`,
                height: '100%',
                backgroundColor: '#6c757d',
                borderRadius: '4px',
                transition: 'width 0.3s ease'
              }}
            />
          </div>

          {/* Chunks Grid */}
          <div style={{
            marginTop: '12px',
            display: 'flex',
            flexWrap: 'wrap',
            gap: '2px'
          }}>
            {Array.from({ length: chunkProgress.total }, (_, i) => (
              <div
                key={i}
                style={{
                  width: '12px',
                  height: '12px',
                  borderRadius: '2px',
                  backgroundColor: i < chunkProgress.current 
                    ? '#4caf50' 
                    : i === chunkProgress.current - 1
                    ? '#ff9800'
                    : '#e0e0e0',
                  transition: 'background-color 0.3s ease'
                }}
                title={`Chunk ${i + 1} ${
                  i < chunkProgress.current 
                    ? '(completed)' 
                    : i === chunkProgress.current - 1
                    ? '(uploading)'
                    : '(pending)'
                }`}
              />
            ))}
          </div>
        </div>
      )}

      {/* Status Messages */}
      <div style={{
        marginTop: '15px',
        display: 'flex',
        alignItems: 'center',
        gap: '10px'
      }}>
        {progress < 100 ? (
          <>
            <div style={{
              width: '16px',
              height: '16px',
              border: '2px solid #d32f2f',
              borderTop: '2px solid transparent',
              borderRadius: '50%',
              animation: 'spin 1s linear infinite'
            }} />
            <span style={{ color: '#666', fontSize: '14px' }}>
              {isMultipart 
                ? `Uploading chunk ${chunkProgress.current} of ${chunkProgress.total}...`
                : 'Uploading...'
              }
            </span>
          </>
        ) : (
          <>
            <div style={{
              width: '16px',
              height: '16px',
              backgroundColor: '#4caf50',
              borderRadius: '50%',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              color: 'white',
              fontSize: '10px',
              fontWeight: 'bold'
            }}>
              ✓
            </div>
            <span style={{ color: '#4caf50', fontSize: '14px', fontWeight: 'bold' }}>
              Upload completed! Processing video...
            </span>
          </>
        )}
      </div>

      {/* CSS for animations */}
      <style jsx>{`
        @keyframes spin {
          0% { transform: rotate(0deg); }
          100% { transform: rotate(360deg); }
        }
        
        @keyframes shine {
          0% { left: -100%; }
          100% { left: 100%; }
        }
      `}</style>
    </div>
  );
};

export default ProgressBar; 