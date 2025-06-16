import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import videoService from '../../services/videoService';

const VideoCard = ({ video }) => {
  const navigate = useNavigate();
  const [imageError, setImageError] = useState(false);
  const [imageLoaded, setImageLoaded] = useState(false);

  const handleVideoClick = () => {
    navigate(`/video/${video.id}`);
  };

  const handleImageError = () => {
    setImageError(true);
  };

  const handleImageLoad = () => {
    setImageLoaded(true);
  };

  const thumbnailUrl = videoService.getThumbnailUrl(video);
  const formattedDuration = videoService.formatDuration(video.duration);
  const formattedDate = videoService.formatUploadDate(video.uploadedAt);

  return (
    <div 
      onClick={handleVideoClick}
      style={{
        cursor: 'pointer',
        backgroundColor: 'white',
        borderRadius: '12px',
        overflow: 'hidden',
        boxShadow: '0 2px 8px rgba(0, 0, 0, 0.1)',
        transition: 'all 0.2s ease',
        height: 'fit-content'
      }}
      onMouseEnter={(e) => {
        e.currentTarget.style.transform = 'translateY(-2px)';
        e.currentTarget.style.boxShadow = '0 4px 16px rgba(0, 0, 0, 0.15)';
      }}
      onMouseLeave={(e) => {
        e.currentTarget.style.transform = 'translateY(0)';
        e.currentTarget.style.boxShadow = '0 2px 8px rgba(0, 0, 0, 0.1)';
      }}
    >
      {/* Thumbnail container */}
      <div style={{
        position: 'relative',
        width: '100%',
        paddingBottom: '56.25%', // 16:9 aspect ratio
        backgroundColor: '#f0f0f0',
        overflow: 'hidden'
      }}>
        {!imageError ? (
          <>
            {!imageLoaded && (
              <div style={{
                position: 'absolute',
                top: 0,
                left: 0,
                width: '100%',
                height: '100%',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                backgroundColor: '#f5f5f5'
              }}>
                <div style={{
                  width: '40px',
                  height: '40px',
                  border: '3px solid #e0e0e0',
                  borderTop: '3px solid #d32f2f',
                  borderRadius: '50%',
                  animation: 'spin 1s linear infinite'
                }} />
              </div>
            )}
            <img 
              src={thumbnailUrl}
              alt={video.title}
              onError={handleImageError}
              onLoad={handleImageLoad}
              style={{
                position: 'absolute',
                top: 0,
                left: 0,
                width: '100%',
                height: '100%',
                objectFit: 'cover',
                display: imageLoaded ? 'block' : 'none'
              }}
            />
          </>
        ) : (
          // Fallback placeholder
          <div style={{
            position: 'absolute',
            top: 0,
            left: 0,
            width: '100%',
            height: '100%',
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            justifyContent: 'center',
            backgroundColor: '#f5f5f5',
            color: '#999'
          }}>
            <div style={{
              width: '48px',
              height: '48px',
              backgroundColor: '#e0e0e0',
              borderRadius: '8px',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              marginBottom: '8px'
            }}>
              📹
            </div>
            <span style={{ fontSize: '12px' }}>No preview</span>
          </div>
        )}

        {/* Duration badge */}
        {video.duration && (
          <div style={{
            position: 'absolute',
            bottom: '8px',
            right: '8px',
            backgroundColor: 'rgba(0, 0, 0, 0.8)',
            color: 'white',
            padding: '2px 6px',
            borderRadius: '4px',
            fontSize: '12px',
            fontWeight: '500'
          }}>
            {formattedDuration}
          </div>
        )}

        {/* CDN indicator */}
        {video.cdnUrls && video.cdnUrls.cdnEnabled && (
          <div style={{
            position: 'absolute',
            top: '8px',
            left: '8px',
            backgroundColor: 'rgba(46, 125, 50, 0.9)',
            color: 'white',
            padding: '2px 6px',
            borderRadius: '4px',
            fontSize: '10px',
            fontWeight: '500'
          }}>
            CDN
          </div>
        )}
      </div>

      {/* Video info */}
      <div style={{
        padding: '12px'
      }}>
        {/* Title */}
        <h3 style={{
          margin: '0 0 8px 0',
          fontSize: '14px',
          fontWeight: '600',
          color: '#1a1a1a',
          lineHeight: '1.3',
          overflow: 'hidden',
          display: '-webkit-box',
          WebkitLineClamp: 2,
          WebkitBoxOrient: 'vertical',
          minHeight: '36px' // Ensure consistent height for 2 lines
        }}>
          {video.title}
        </h3>

        {/* Description (if available) */}
        {video.description && (
          <p style={{
            margin: '0 0 8px 0',
            fontSize: '12px',
            color: '#666',
            lineHeight: '1.4',
            overflow: 'hidden',
            display: '-webkit-box',
            WebkitLineClamp: 2,
            WebkitBoxOrient: 'vertical'
          }}>
            {video.description}
          </p>
        )}

        {/* Meta info */}
        <div style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          fontSize: '12px',
          color: '#888'
        }}>
          <span>{formattedDate}</span>
          {video.viewsCount !== undefined && (
            <span>
              {video.viewsCount === 0 ? 'No views' : 
               video.viewsCount === 1 ? '1 view' : 
               `${video.viewsCount.toLocaleString()} views`}
            </span>
          )}
        </div>
      </div>

      {/* CSS for spinner animation */}
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
};

export default VideoCard; 