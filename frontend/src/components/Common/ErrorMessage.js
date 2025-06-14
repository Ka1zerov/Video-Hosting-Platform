import React from 'react';

const ErrorMessage = ({ 
  title = 'Error', 
  message, 
  showRetry = false, 
  onRetry = null,
  type = 'error' 
}) => {
  const getTypeColor = () => {
    switch (type) {
      case 'warning':
        return '#f57c00';
      case 'info':
        return '#1976d2';
      default:
        return '#d32f2f';
    }
  };

  return (
    <div style={{
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'center',
      justifyContent: 'center',
      minHeight: '200px',
      padding: '20px'
    }}>
      <div style={{
        backgroundColor: 'white',
        borderRadius: '8px',
        padding: '30px',
        boxShadow: '0 2px 10px rgba(0,0,0,0.1)',
        textAlign: 'center',
        maxWidth: '500px',
        width: '100%',
        border: `2px solid ${getTypeColor()}`
      }}>
        <div style={{
          color: getTypeColor(),
          fontSize: '48px',
          marginBottom: '15px'
        }}>
          {type === 'warning' ? '⚠️' : type === 'info' ? 'ℹ️' : '❌'}
        </div>
        
        <h2 style={{
          color: getTypeColor(),
          marginBottom: '15px',
          fontSize: '20px'
        }}>
          {title}
        </h2>
        
        <p style={{
          color: '#666',
          marginBottom: showRetry ? '25px' : '0',
          fontSize: '16px',
          lineHeight: '1.5'
        }}>
          {message}
        </p>
        
        {showRetry && onRetry && (
          <button
            onClick={onRetry}
            style={{
              backgroundColor: getTypeColor(),
              color: 'white',
              border: 'none',
              borderRadius: '6px',
              padding: '10px 25px',
              fontSize: '14px',
              cursor: 'pointer',
              transition: 'opacity 0.2s'
            }}
            onMouseOver={(e) => e.target.style.opacity = '0.8'}
            onMouseOut={(e) => e.target.style.opacity = '1'}
          >
            Try Again
          </button>
        )}
      </div>
    </div>
  );
};

export default ErrorMessage; 