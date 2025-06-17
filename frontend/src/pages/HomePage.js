import React from 'react';
import Header from '../components/Layout/Header';

const HomePage = () => {
  return (
    <div style={{ minHeight: '100vh', backgroundColor: '#fafafa' }}>
      <Header />
      
      <main style={{ padding: '40px 20px' }}>
        <div style={{
          maxWidth: '1200px',
          margin: '0 auto',
          textAlign: 'center'
        }}>
          <h1 style={{
            color: '#d32f2f',
            fontSize: '48px',
            marginBottom: '20px'
          }}>
            Welcome to Video Platform! 🎥
          </h1>
          
          <p style={{
            fontSize: '18px',
            color: '#666',
            marginBottom: '40px',
            maxWidth: '600px',
            margin: '0 auto 40px auto'
          }}>
            Your personal video hosting platform. Upload, manage, and stream your videos 
            with professional quality and security.
          </p>

          <div style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))',
            gap: '30px',
            marginTop: '60px'
          }}>
            <FeatureCard
              icon="📤"
              title="Upload Videos"
              description="Secure multipart upload with progress tracking and automatic encoding to multiple qualities."
              linkText="Start Uploading"
              linkHref="/upload"
            />
            
            <FeatureCard
              icon="🎬"
              title="Stream Videos"
              description="High-quality HLS streaming with adaptive bitrate for optimal viewing experience."
              linkText="Browse Videos"
              linkHref="/videos"
            />
          </div>

          <div style={{
            backgroundColor: 'white',
            borderRadius: '8px',
            padding: '40px',
            marginTop: '60px',
            boxShadow: '0 2px 10px rgba(0,0,0,0.1)'
          }}>
            <h2 style={{
              color: '#d32f2f',
              marginBottom: '20px'
            }}>
              System Status
            </h2>
            
            <div style={{
              display: 'flex',
              justifyContent: 'center',
              gap: '40px',
              flexWrap: 'wrap'
            }}>
              <StatusItem label="Authentication" status="active" />
              <StatusItem label="Upload Service" status="active" />
              <StatusItem label="Encoding Service" status="active" />
              <StatusItem label="Streaming Service" status="active" />
            </div>
          </div>
        </div>
      </main>
    </div>
  );
};

const FeatureCard = ({ icon, title, description, linkText, linkHref }) => (
  <div style={{
    backgroundColor: 'white',
    borderRadius: '8px',
    padding: '30px',
    boxShadow: '0 2px 10px rgba(0,0,0,0.1)',
    textAlign: 'center',
    transition: 'transform 0.2s, box-shadow 0.2s'
  }}
  onMouseOver={(e) => {
    e.currentTarget.style.transform = 'translateY(-5px)';
    e.currentTarget.style.boxShadow = '0 4px 20px rgba(0,0,0,0.15)';
  }}
  onMouseOut={(e) => {
    e.currentTarget.style.transform = 'translateY(0)';
    e.currentTarget.style.boxShadow = '0 2px 10px rgba(0,0,0,0.1)';
  }}>
    <div style={{ fontSize: '48px', marginBottom: '20px' }}>
      {icon}
    </div>
    
    <h3 style={{
      color: '#d32f2f',
      marginBottom: '15px',
      fontSize: '20px'
    }}>
      {title}
    </h3>
    
    <p style={{
      color: '#666',
      marginBottom: '25px',
      lineHeight: '1.6'
    }}>
      {description}
    </p>
    
    <a
      href={linkHref}
      style={{
        display: 'inline-block',
        backgroundColor: '#d32f2f',
        color: 'white',
        textDecoration: 'none',
        padding: '10px 20px',
        borderRadius: '6px',
        fontSize: '14px',
        fontWeight: 'bold',
        transition: 'background-color 0.2s'
      }}
      onMouseOver={(e) => e.target.style.backgroundColor = '#b71c1c'}
      onMouseOut={(e) => e.target.style.backgroundColor = '#d32f2f'}
    >
      {linkText}
    </a>
  </div>
);

const StatusItem = ({ label, status }) => (
  <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
    <div style={{
      width: '12px',
      height: '12px',
      borderRadius: '50%',
      backgroundColor: status === 'active' ? '#4caf50' : '#f44336'
    }} />
    <span style={{ color: '#666', fontSize: '14px' }}>
      {label}
    </span>
  </div>
);

export default HomePage; 