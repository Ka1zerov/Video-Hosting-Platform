import React from 'react';
import { useAuth } from '../../hooks/useAuth';

const Header = () => {
  const { logout } = useAuth();
  
  const handleLogout = async () => {
    if (window.confirm('Are you sure you want to logout?')) {
      await logout();
    }
  };

  return (
    <header style={{
      backgroundColor: '#d32f2f',
      color: 'white',
      padding: '15px 20px',
      boxShadow: '0 2px 4px rgba(0,0,0,0.1)',
      display: 'flex',
      justifyContent: 'space-between',
      alignItems: 'center'
    }}>
      <div style={{ display: 'flex', alignItems: 'center' }}>
        <h1 style={{
          margin: 0,
          fontSize: '24px',
          fontWeight: 'bold'
        }}>
          🎥 Video Platform
        </h1>
      </div>

      <nav style={{ display: 'flex', alignItems: 'center', gap: '20px' }}>
        <a 
          href="/" 
          style={{
            color: 'white',
            textDecoration: 'none',
            padding: '8px 16px',
            borderRadius: '4px',
            transition: 'background-color 0.2s'
          }}
          onMouseOver={(e) => e.target.style.backgroundColor = 'rgba(255,255,255,0.1)'}
          onMouseOut={(e) => e.target.style.backgroundColor = 'transparent'}
        >
          Home
        </a>
        
        <a 
          href="/videos" 
          style={{
            color: 'white',
            textDecoration: 'none',
            padding: '8px 16px',
            borderRadius: '4px',
            transition: 'background-color 0.2s'
          }}
          onMouseOver={(e) => e.target.style.backgroundColor = 'rgba(255,255,255,0.1)'}
          onMouseOut={(e) => e.target.style.backgroundColor = 'transparent'}
        >
          Videos
        </a>
        
        <a 
          href="/upload" 
          style={{
            color: 'white',
            textDecoration: 'none',
            padding: '8px 16px',
            borderRadius: '4px',
            transition: 'background-color 0.2s'
          }}
          onMouseOver={(e) => e.target.style.backgroundColor = 'rgba(255,255,255,0.1)'}
          onMouseOut={(e) => e.target.style.backgroundColor = 'transparent'}
        >
          Upload
        </a>

        <div style={{
          display: 'flex',
          alignItems: 'center',
          gap: '10px',
          marginLeft: '20px',
          paddingLeft: '20px',
          borderLeft: '1px solid rgba(255,255,255,0.3)'
        }}>
          <span style={{
            fontSize: '14px',
            opacity: 0.9
          }}>
            Welcome, User!
          </span>
          
          <button
            onClick={handleLogout}
            style={{
              backgroundColor: 'transparent',
              color: 'white',
              border: '1px solid rgba(255,255,255,0.5)',
              borderRadius: '4px',
              padding: '6px 12px',
              fontSize: '14px',
              cursor: 'pointer',
              transition: 'all 0.2s'
            }}
            onMouseOver={(e) => {
              e.target.style.backgroundColor = 'rgba(255,255,255,0.1)';
              e.target.style.borderColor = 'white';
            }}
            onMouseOut={(e) => {
              e.target.style.backgroundColor = 'transparent';
              e.target.style.borderColor = 'rgba(255,255,255,0.5)';
            }}
          >
            Logout
          </button>
        </div>
      </nav>
    </header>
  );
};

export default Header; 