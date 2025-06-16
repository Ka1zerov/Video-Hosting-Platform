import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import ReactPlayer from 'react-player';
import Header from '../components/Layout/Header';
import videoService from '../services/videoService';

const VideoPlayerPage = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const [videoData, setVideoData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [playerReady, setPlayerReady] = useState(false);
  const [playlistBlobUrl, setPlaylistBlobUrl] = useState(null);

  useEffect(() => {
    const fetchVideoData = async () => {
      try {
        setLoading(true);
        setError(null);

        // Get video stream information
        const streamInfo = await videoService.getVideoStreamInfo(id);
        
        // Get master playlist content with JWT authentication and create blob URL
        const blobUrl = await videoService.getMasterPlaylistBlob(id, 2);
        setPlaylistBlobUrl(blobUrl);
        
        setVideoData({
          ...streamInfo,
          masterPlaylistUrl: blobUrl
        });

      } catch (err) {
        console.error('Failed to load video:', err);
        setError(err.message || 'Failed to load video');
      } finally {
        setLoading(false);
      }
    };

    if (id) {
      fetchVideoData();
    }
  }, [id]);

  // Cleanup blob URL when component unmounts or video changes
  useEffect(() => {
    return () => {
      if (playlistBlobUrl) {
        videoService.cleanupBlobUrl(playlistBlobUrl);
      }
    };
  }, [playlistBlobUrl]);

  const handleBack = () => {
    navigate('/videos');
  };

  const handleHome = () => {
    navigate('/');
  };

  if (loading) {
    return (
      <div style={{
        minHeight: '100vh',
        backgroundColor: '#fafafa'
      }}>
        <Header />
        <div style={{
          maxWidth: '1200px',
          margin: '0 auto',
          padding: '40px 20px',
          textAlign: 'center'
        }}>
          <div style={{
            display: 'inline-block',
            width: '40px',
            height: '40px',
            border: '3px solid #e0e0e0',
            borderTop: '3px solid #d32f2f',
            borderRadius: '50%',
            animation: 'spin 1s linear infinite'
          }} />
          <p style={{ marginTop: '20px', color: '#666' }}>
            Loading video...
          </p>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div style={{
        minHeight: '100vh',
        backgroundColor: '#fafafa'
      }}>
        <Header />
        <div style={{
          maxWidth: '1200px',
          margin: '0 auto',
          padding: '40px 20px',
          textAlign: 'center'
        }}>
          <div style={{
            backgroundColor: 'white',
            padding: '40px',
            borderRadius: '12px',
            boxShadow: '0 2px 8px rgba(0, 0, 0, 0.1)'
          }}>
            <h1 style={{ color: '#d32f2f', marginBottom: '20px' }}>
              Error Loading Video
            </h1>
            <p style={{ color: '#666', marginBottom: '30px' }}>
              {error}
            </p>
            <div style={{ display: 'flex', gap: '12px', justifyContent: 'center' }}>
              <button
                onClick={handleBack}
                style={{
                  backgroundColor: '#d32f2f',
                  color: 'white',
                  border: 'none',
                  padding: '12px 24px',
                  borderRadius: '6px',
                  cursor: 'pointer',
                  fontSize: '14px',
                  fontWeight: '500'
                }}
              >
                Back to Videos
              </button>
              <button
                onClick={handleHome}
                style={{
                  backgroundColor: 'transparent',
                  color: '#d32f2f',
                  border: '2px solid #d32f2f',
                  padding: '12px 24px',
                  borderRadius: '6px',
                  cursor: 'pointer',
                  fontSize: '14px',
                  fontWeight: '500'
                }}
              >
                Home
              </button>
            </div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div style={{
      minHeight: '100vh',
      backgroundColor: '#000'
    }}>
      <Header />
      
      {/* Video Player Container */}
      <div style={{
        maxWidth: '1200px',
        margin: '0 auto',
        padding: '20px'
      }}>
        {/* Navigation buttons */}
        <div style={{
          marginBottom: '20px',
          display: 'flex',
          gap: '12px'
        }}>
          <button
            onClick={handleBack}
            style={{
              backgroundColor: 'rgba(255, 255, 255, 0.1)',
              color: 'white',
              border: '1px solid rgba(255, 255, 255, 0.3)',
              padding: '8px 16px',
              borderRadius: '6px',
              cursor: 'pointer',
              fontSize: '14px',
              fontWeight: '500',
              transition: 'all 0.2s'
            }}
            onMouseOver={(e) => {
              e.target.style.backgroundColor = 'rgba(255, 255, 255, 0.2)';
            }}
            onMouseOut={(e) => {
              e.target.style.backgroundColor = 'rgba(255, 255, 255, 0.1)';
            }}
          >
            ← Videos
          </button>
          <button
            onClick={handleHome}
            style={{
              backgroundColor: 'rgba(255, 255, 255, 0.1)',
              color: 'white',
              border: '1px solid rgba(255, 255, 255, 0.3)',
              padding: '8px 16px',
              borderRadius: '6px',
              cursor: 'pointer',
              fontSize: '14px',
              fontWeight: '500',
              transition: 'all 0.2s'
            }}
            onMouseOver={(e) => {
              e.target.style.backgroundColor = 'rgba(255, 255, 255, 0.2)';
            }}
            onMouseOut={(e) => {
              e.target.style.backgroundColor = 'rgba(255, 255, 255, 0.1)';
            }}
          >
            🏠 Home
          </button>
        </div>

        {/* Video Player */}
        <div style={{
          position: 'relative',
          paddingBottom: '56.25%', // 16:9 aspect ratio
          height: 0,
          backgroundColor: '#000',
          borderRadius: '12px',
          overflow: 'hidden',
          boxShadow: '0 4px 20px rgba(0, 0, 0, 0.3)'
        }}>
          {videoData && (
            <ReactPlayer
              url={videoData.masterPlaylistUrl}
              width="100%"
              height="100%"
              controls={true}
              playing={false}
              config={{
                file: {
                  forceHLS: true,
                  hlsOptions: {
                    enableWorker: true,
                    lowLatencyMode: false,
                    backBufferLength: 90
                  }
                }
              }}
              onReady={() => setPlayerReady(true)}
              onError={(error) => {
                console.error('Player error:', error);
                setError('Video playback failed. Please try again.');
              }}
              style={{
                position: 'absolute',
                top: 0,
                left: 0
              }}
            />
          )}
          
          {!playerReady && videoData && (
            <div style={{
              position: 'absolute',
              top: '50%',
              left: '50%',
              transform: 'translate(-50%, -50%)',
              color: 'white',
              textAlign: 'center'
            }}>
              <div style={{
                width: '40px',
                height: '40px',
                border: '3px solid rgba(255, 255, 255, 0.3)',
                borderTop: '3px solid white',
                borderRadius: '50%',
                animation: 'spin 1s linear infinite',
                margin: '0 auto 12px'
              }} />
              <p>Loading player...</p>
            </div>
          )}
        </div>

        {/* Video Info */}
        {videoData && (
          <div style={{
            backgroundColor: 'white',
            padding: '24px',
            borderRadius: '12px',
            marginTop: '20px',
            boxShadow: '0 2px 8px rgba(0, 0, 0, 0.1)'
          }}>
            <h1 style={{
              margin: '0 0 12px 0',
              fontSize: '24px',
              fontWeight: '700',
              color: '#1a1a1a',
              lineHeight: '1.3'
            }}>
              {videoData.title}
            </h1>
            
            <div style={{
              display: 'flex',
              gap: '20px',
              marginBottom: '16px',
              fontSize: '14px',
              color: '#666'
            }}>
              {videoData.duration && (
                <span>
                  <strong>Duration:</strong> {videoService.formatDuration(videoData.duration)}
                </span>
              )}
              {videoData.viewsCount !== undefined && (
                <span>
                  <strong>Views:</strong> {videoData.viewsCount.toLocaleString()}
                </span>
              )}
            </div>

            {videoData.description && (
              <div style={{
                fontSize: '14px',
                color: '#333',
                lineHeight: '1.5'
              }}>
                <strong>Description:</strong>
                <p style={{ margin: '8px 0 0 0' }}>
                  {videoData.description}
                </p>
              </div>
            )}

            {/* Quality options info */}
            {videoData.qualities && videoData.qualities.length > 0 && (
              <div style={{
                marginTop: '16px',
                padding: '12px',
                backgroundColor: '#f5f5f5',
                borderRadius: '8px'
              }}>
                <strong style={{ fontSize: '14px', color: '#333' }}>
                  Available Qualities:
                </strong>
                <div style={{
                  display: 'flex',
                  gap: '8px',
                  marginTop: '8px',
                  flexWrap: 'wrap'
                }}>
                  {videoData.qualities.map((quality, index) => (
                    <span
                      key={index}
                      style={{
                        backgroundColor: quality.available ? '#4caf50' : '#f44336',
                        color: 'white',
                        padding: '4px 8px',
                        borderRadius: '4px',
                        fontSize: '12px',
                        fontWeight: '500'
                      }}
                    >
                      {quality.qualityName} ({quality.width}x{quality.height})
                    </span>
                  ))}
                </div>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
};

export default VideoPlayerPage; 