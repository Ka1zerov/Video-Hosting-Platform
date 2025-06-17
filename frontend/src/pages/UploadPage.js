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
        padding: '15px',
        maxWidth: '1200px',
        margin: '0 auto'
      }}>
        {/* Page Header */}
        <div style={{
          textAlign: 'center',
          marginBottom: '25px'
        }}>
          <h1 style={{
            color: '#d32f2f',
            fontSize: '32px',
            fontWeight: 'bold',
            margin: '0 0 8px 0'
          }}>
            📤 Upload Your Video
          </h1>
          <p style={{
            color: '#666',
            fontSize: '16px',
            margin: 0,
            lineHeight: '1.4'
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
            marginTop: '25px',
            maxWidth: '800px',
            margin: '25px auto 0'
          }}>
            <h2 style={{
              color: '#333',
              fontSize: '20px',
              fontWeight: 'bold',
              marginBottom: '15px',
              textAlign: 'center'
            }}>
              📊 Recent Uploads
            </h2>
            
            <div style={{
              backgroundColor: 'white',
              border: '2px solid #e0e0e0',
              borderRadius: '8px',
              padding: '15px'
            }}>
              {recentUploads.map((upload, index) => (
                <div
                  key={upload.id || index}
                  style={{
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center',
                    padding: '8px 0',
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
      </div>
    </div>
  );
};

export default UploadPage; 