import React from 'react';
import VideoCard from './VideoCard';

const VideoGrid = ({ videos, loading, error }) => {
  if (loading) {
    return (
      <div style={{
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))',
        gap: '20px',
        padding: '20px'
      }}>
        {/* Loading skeleton */}
        {Array(12).fill(0).map((_, index) => (
          <div key={index} style={{
            backgroundColor: 'white',
            borderRadius: '12px',
            overflow: 'hidden',
            boxShadow: '0 2px 8px rgba(0, 0, 0, 0.1)'
          }}>
            {/* Skeleton thumbnail */}
            <div style={{
              width: '100%',
              paddingBottom: '56.25%',
              backgroundColor: '#f0f0f0',
              position: 'relative'
            }}>
              <div style={{
                position: 'absolute',
                top: '50%',
                left: '50%',
                transform: 'translate(-50%, -50%)',
                width: '40px',
                height: '40px',
                border: '3px solid #e0e0e0',
                borderTop: '3px solid #d32f2f',
                borderRadius: '50%',
                animation: 'spin 1s linear infinite'
              }} />
            </div>
            
            {/* Skeleton content */}
            <div style={{ padding: '12px' }}>
              <div style={{
                height: '16px',
                backgroundColor: '#f0f0f0',
                borderRadius: '4px',
                marginBottom: '8px'
              }} />
              <div style={{
                height: '12px',
                backgroundColor: '#f5f5f5',
                borderRadius: '4px',
                width: '80%',
                marginBottom: '8px'
              }} />
              <div style={{
                height: '12px',
                backgroundColor: '#f5f5f5',
                borderRadius: '4px',
                width: '60%'
              }} />
            </div>
          </div>
        ))}
        
        <style>
          {`
            @keyframes spin {
              0% { transform: rotate(0deg); }
              100% { transform: rotate(360deg); }
            }
          `}
        </style>
      </div>
    );
  }

  if (error) {
    return (
      <div style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        padding: '60px 20px',
        textAlign: 'center'
      }}>
        <div style={{
          width: '80px',
          height: '80px',
          backgroundColor: '#ffebee',
          borderRadius: '50%',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          marginBottom: '20px'
        }}>
          <span style={{ fontSize: '32px' }}>⚠️</span>
        </div>
        <h3 style={{
          margin: '0 0 10px 0',
          color: '#d32f2f',
          fontSize: '18px'
        }}>
          Failed to load videos
        </h3>
        <p style={{
          margin: '0 0 20px 0',
          color: '#666',
          fontSize: '14px',
          maxWidth: '400px'
        }}>
          {error}
        </p>
        <button
          onClick={() => window.location.reload()}
          style={{
            backgroundColor: '#d32f2f',
            color: 'white',
            border: 'none',
            padding: '10px 20px',
            borderRadius: '6px',
            cursor: 'pointer',
            fontSize: '14px',
            fontWeight: '500'
          }}
        >
          Try Again
        </button>
      </div>
    );
  }

  if (!videos || videos.length === 0) {
    return (
      <div style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        padding: '60px 20px',
        textAlign: 'center'
      }}>
        <div style={{
          width: '80px',
          height: '80px',
          backgroundColor: '#f5f5f5',
          borderRadius: '50%',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          marginBottom: '20px'
        }}>
          <span style={{ fontSize: '32px' }}>📹</span>
        </div>
        <h3 style={{
          margin: '0 0 10px 0',
          color: '#666',
          fontSize: '18px'
        }}>
          No videos found
        </h3>
        <p style={{
          margin: '0',
          color: '#888',
          fontSize: '14px'
        }}>
          There are no videos to display at the moment.
        </p>
      </div>
    );
  }

  return (
    <div style={{
      display: 'grid',
      gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))',
      gap: '20px',
      padding: '20px'
    }}>
      {videos.map((video) => (
        <VideoCard key={video.id} video={video} />
      ))}
    </div>
  );
};

export default VideoGrid; 