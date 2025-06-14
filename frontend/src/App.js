import React from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import ProtectedRoute from './components/Auth/ProtectedRoute';
import HomePage from './pages/HomePage';

// Placeholder components for future pages
const UploadPage = () => (
  <div style={{ padding: '40px', textAlign: 'center' }}>
    <h1 style={{ color: '#d32f2f' }}>Upload Video</h1>
    <p style={{ color: '#666' }}>Video upload functionality will be implemented here.</p>
  </div>
);

const VideosPage = () => (
  <div style={{ padding: '40px', textAlign: 'center' }}>
    <h1 style={{ color: '#d32f2f' }}>All Videos</h1>
    <p style={{ color: '#666' }}>Video list functionality will be implemented here.</p>
  </div>
);

const VideoPage = () => (
  <div style={{ padding: '40px', textAlign: 'center' }}>
    <h1 style={{ color: '#d32f2f' }}>Video Player</h1>
    <p style={{ color: '#666' }}>Video player functionality will be implemented here.</p>
  </div>
);

const AnalyticsPage = () => (
  <div style={{ padding: '40px', textAlign: 'center' }}>
    <h1 style={{ color: '#d32f2f' }}>Analytics</h1>
    <p style={{ color: '#666' }}>Analytics functionality will be implemented here.</p>
  </div>
);

function App() {
  return (
    <Router>
      <div style={{
        fontFamily: '"Segoe UI", Tahoma, Geneva, Verdana, sans-serif',
        minHeight: '100vh',
        backgroundColor: '#fafafa'
      }}>
        <Routes>
          {/* All routes are protected and require authentication */}
          <Route 
            path="/" 
            element={
              <ProtectedRoute>
                <HomePage />
              </ProtectedRoute>
            } 
          />
          
          <Route 
            path="/upload" 
            element={
              <ProtectedRoute>
                <UploadPage />
              </ProtectedRoute>
            } 
          />
          
          <Route 
            path="/videos" 
            element={
              <ProtectedRoute>
                <VideosPage />
              </ProtectedRoute>
            } 
          />
          
          <Route 
            path="/video/:id" 
            element={
              <ProtectedRoute>
                <VideoPage />
              </ProtectedRoute>
            } 
          />
          
          <Route 
            path="/analytics" 
            element={
              <ProtectedRoute>
                <AnalyticsPage />
              </ProtectedRoute>
            } 
          />
          
          {/* Fallback route */}
          <Route 
            path="*" 
            element={
              <ProtectedRoute>
                <div style={{ 
                  padding: '40px', 
                  textAlign: 'center',
                  minHeight: '100vh',
                  display: 'flex',
                  flexDirection: 'column',
                  justifyContent: 'center',
                  alignItems: 'center'
                }}>
                  <h1 style={{ color: '#d32f2f', fontSize: '48px' }}>404</h1>
                  <p style={{ color: '#666', fontSize: '18px', marginBottom: '20px' }}>
                    Page not found
                  </p>
                  <a 
                    href="/" 
                    style={{
                      color: '#d32f2f',
                      textDecoration: 'none',
                      fontSize: '16px',
                      border: '2px solid #d32f2f',
                      padding: '10px 20px',
                      borderRadius: '6px',
                      transition: 'all 0.2s'
                    }}
                    onMouseOver={(e) => {
                      e.target.style.backgroundColor = '#d32f2f';
                      e.target.style.color = 'white';
                    }}
                    onMouseOut={(e) => {
                      e.target.style.backgroundColor = 'transparent';
                      e.target.style.color = '#d32f2f';
                    }}
                  >
                    Go Home
                  </a>
                </div>
              </ProtectedRoute>
            } 
          />
        </Routes>
      </div>
    </Router>
  );
}

export default App; 