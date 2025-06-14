import React, { useEffect } from 'react';
import { useAuth } from '../../hooks/useAuth';
import LoadingSpinner from '../Common/LoadingSpinner';
import ErrorMessage from '../Common/ErrorMessage';

const ProtectedRoute = ({ children }) => {
  const { isAuthenticated, isLoading, error, login, retry } = useAuth();

  // Debug log to track authentication state changes
  useEffect(() => {
    console.log('ProtectedRoute - Authentication state:', { isAuthenticated, isLoading, error });
  }, [isAuthenticated, isLoading, error]);

  if (isLoading) {
    return <LoadingSpinner message="Checking authentication..." />;
  }

  if (error) {
    return (
      <ErrorMessage 
        title="Authentication Error"
        message={error}
        showRetry={true}
        onRetry={retry}
      />
    );
  }

  if (!isAuthenticated) {
    return <LoginRequired onLogin={login} />;
  }

  return children;
};

const LoginRequired = ({ onLogin }) => {
  return (
    <div style={{
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'center',
      justifyContent: 'center',
      minHeight: '100vh',
      backgroundColor: '#fafafa',
      padding: '20px'
    }}>
      <div style={{
        backgroundColor: 'white',
        borderRadius: '8px',
        padding: '40px',
        boxShadow: '0 2px 10px rgba(0,0,0,0.1)',
        textAlign: 'center',
        maxWidth: '400px',
        width: '100%'
      }}>
        <h1 style={{
          color: '#d32f2f',
          marginBottom: '20px',
          fontSize: '24px'
        }}>
          Video Hosting Platform
        </h1>
        
        <p style={{
          color: '#666',
          marginBottom: '30px',
          fontSize: '16px'
        }}>
          Please sign in to access your videos
        </p>
        
        <button
          onClick={onLogin}
          style={{
            backgroundColor: '#d32f2f',
            color: 'white',
            border: 'none',
            borderRadius: '6px',
            padding: '12px 30px',
            fontSize: '16px',
            cursor: 'pointer',
            transition: 'background-color 0.2s',
            width: '100%'
          }}
          onMouseOver={(e) => e.target.style.backgroundColor = '#b71c1c'}
          onMouseOut={(e) => e.target.style.backgroundColor = '#d32f2f'}
        >
          Sign In
        </button>
        
        <p style={{
          marginTop: '20px',
          fontSize: '14px',
          color: '#999'
        }}>
          You will be redirected to the secure login page
        </p>
      </div>
    </div>
  );
};

export default ProtectedRoute; 