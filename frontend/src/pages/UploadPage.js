import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Header from '../components/Layout/Header';
import VideoUpload from '../components/Upload/VideoUpload';

const UploadPage = () => {
  const navigate = useNavigate();
  const [recentUploads, setRecentUploads] = useState([]);

  // Handle successful upload
  const handleUploadComplete = (uploadResult) => {
    console.log('Upload completed:', uploadResult);
    
    // Add to recent uploads
    setRecentUploads(prev => [uploadResult, ...prev.slice(0, 4)]); // Keep last 5
    
    // You could show a notification here
    // toast.success('Video uploaded successfully!');
  };

  // Handle upload error
  const handleUploadError = (error) => {
    console.error('Upload failed:', error);
    
    // You could show a notification here
    // toast.error('Upload failed: ' + error.message);
  };

  return (
    <div style={{
      minHeight: '100vh',
      backgroundColor: '#fafafa'
    }}>
      <Header />
      
      <div style={{
        padding: '20px',
        maxWidth: '1200px',
        margin: '0 auto'
      }}>
        {/* Page Header */}
        <div style={{
          textAlign: 'center',
          marginBottom: '40px'
        }}>
          <h1 style={{
            color: '#d32f2f',
            fontSize: '36px',
            fontWeight: 'bold',
            margin: '0 0 10px 0'
          }}>
            📤 Upload Your Video
          </h1>
          <p style={{
            color: '#666',
            fontSize: '18px',
            margin: 0,
            lineHeight: '1.5'
          }}>
            Share your content with the world. Upload videos up to 2GB in size.
          </p>
        </div>

        {/* Upload Component */}
        <VideoUpload
          onUploadComplete={handleUploadComplete}
          onUploadError={handleUploadError}
        />

        {/* Recent Uploads */}
        {recentUploads.length > 0 && (
          <div style={{
            marginTop: '40px',
            maxWidth: '800px',
            margin: '40px auto 0'
          }}>
            <h2 style={{
              color: '#333',
              fontSize: '24px',
              fontWeight: 'bold',
              marginBottom: '20px',
              textAlign: 'center'
            }}>
              📊 Recent Uploads
            </h2>
            
            <div style={{
              backgroundColor: 'white',
              border: '2px solid #e0e0e0',
              borderRadius: '8px',
              padding: '20px'
            }}>
              {recentUploads.map((upload, index) => (
                <div
                  key={upload.id || index}
                  style={{
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center',
                    padding: '12px 0',
                    borderBottom: index < recentUploads.length - 1 ? '1px solid #f0f0f0' : 'none'
                  }}
                >
                  <div style={{ flex: 1 }}>
                    <div style={{
                      fontWeight: 'bold',
                      color: '#333',
                      marginBottom: '4px'
                    }}>
                      {upload.title}
                    </div>
                    <div style={{
                      color: '#666',
                      fontSize: '14px'
                    }}>
                      {upload.originalFilename} • {upload.status}
                    </div>
                  </div>
                  
                  <div style={{
                    display: 'flex',
                    gap: '8px'
                  }}>
                    <button
                      onClick={() => navigate(`/video/${upload.id}`)}
                      style={{
                        backgroundColor: 'transparent',
                        color: '#d32f2f',
                        border: '1px solid #d32f2f',
                        padding: '6px 12px',
                        borderRadius: '4px',
                        fontSize: '12px',
                        cursor: 'pointer',
                        fontWeight: 'bold'
                      }}
                    >
                      View
                    </button>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Upload Tips */}
        <div style={{
          marginTop: '40px',
          maxWidth: '800px',
          margin: '40px auto 0'
        }}>
          <h2 style={{
            color: '#333',
            fontSize: '24px',
            fontWeight: 'bold',
            marginBottom: '20px',
            textAlign: 'center'
          }}>
            💡 Upload Tips
          </h2>
          
          <div style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))',
            gap: '20px'
          }}>
            <div style={{
              backgroundColor: 'white',
              border: '2px solid #e0e0e0',
              borderRadius: '8px',
              padding: '20px'
            }}>
              <h3 style={{
                color: '#d32f2f',
                fontSize: '18px',
                fontWeight: 'bold',
                marginBottom: '10px'
              }}>
                🎥 Video Quality
              </h3>
              <ul style={{
                color: '#666',
                fontSize: '14px',
                lineHeight: '1.6',
                margin: 0,
                paddingLeft: '18px'
              }}>
                <li>Use high resolution (1080p recommended)</li>
                <li>Maintain good lighting and audio quality</li>
                <li>Keep file size under 2GB</li>
                <li>MP4 format works best</li>
              </ul>
            </div>

            <div style={{
              backgroundColor: 'white',
              border: '2px solid #e0e0e0',
              borderRadius: '8px',
              padding: '20px'
            }}>
              <h3 style={{
                color: '#d32f2f',
                fontSize: '18px',
                fontWeight: 'bold',
                marginBottom: '10px'
              }}>
                📝 Metadata
              </h3>
              <ul style={{
                color: '#666',
                fontSize: '14px',
                lineHeight: '1.6',
                margin: 0,
                paddingLeft: '18px'
              }}>
                <li>Choose a clear, descriptive title</li>
                <li>Add detailed description</li>
                <li>Use relevant keywords</li>
                <li>Consider your audience</li>
              </ul>
            </div>

            <div style={{
              backgroundColor: 'white',
              border: '2px solid #e0e0e0',
              borderRadius: '8px',
              padding: '20px'
            }}>
              <h3 style={{
                color: '#d32f2f',
                fontSize: '18px',
                fontWeight: 'bold',
                marginBottom: '10px'
              }}>
                ⚡ Performance
              </h3>
              <ul style={{
                color: '#666',
                fontSize: '14px',
                lineHeight: '1.6',
                margin: 0,
                paddingLeft: '18px'
              }}>
                <li>Large files use chunked upload</li>
                <li>Upload can be resumed if interrupted</li>
                <li>Processing starts automatically</li>
                <li>Multiple qualities generated</li>
              </ul>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default UploadPage; 