import React, { useState, useEffect } from 'react';
import { useAuth } from '../hooks/useAuth';
import uploadService from '../services/uploadService';

const DebugPage = () => {
  const { isAuthenticated, isLoading, error, getAccessToken, login, logout } = useAuth();
  const [apiTestResult, setApiTestResult] = useState(null);
  const [testLoading, setTestLoading] = useState(false);
  const [localStorageToken, setLocalStorageToken] = useState(null);
  const [refreshCount, setRefreshCount] = useState(0);

  const token = getAccessToken();

  // Check localStorage token separately
  useEffect(() => {
    const checkLocalStorage = () => {
      try {
        const tokens = JSON.parse(localStorage.getItem('auth_tokens'));
        setLocalStorageToken(tokens);
      } catch (error) {
        setLocalStorageToken(null);
      }
    };

    checkLocalStorage();
    
    // Check localStorage every second to see changes
    const interval = setInterval(checkLocalStorage, 1000);
    return () => clearInterval(interval);
  }, [refreshCount]);

  const handleRefresh = () => {
    setRefreshCount(prev => prev + 1);
  };

  const clearLocalStorage = () => {
    localStorage.removeItem('auth_tokens');
    handleRefresh();
  };

  const testApiCall = async () => {
    setTestLoading(true);
    setApiTestResult(null);
    
    try {
      console.log('Testing API call with token:', token);
      
      // Test simple GET request first
      const response = await uploadService.getUserVideos();
      setApiTestResult({
        success: true,
        data: response,
        message: 'API call successful!'
      });
    } catch (err) {
      console.error('API test failed:', err);
      setApiTestResult({
        success: false,
        error: err.message,
        message: 'API call failed!'
      });
    } finally {
      setTestLoading(false);
    }
  };

  const testMultipartInitiate = async () => {
    setTestLoading(true);
    setApiTestResult(null);
    
    try {
      console.log('Testing multipart initiate with token:', token);
      
      const testFile = {
        name: 'test.mp4',
        size: 1000000,
        type: 'video/mp4'
      };
      
      const metadata = {
        title: 'Test Video',
        description: 'Test Description'
      };
      
      const response = await uploadService.initiateMultipartUpload(testFile, metadata);
      setApiTestResult({
        success: true,
        data: response,
        message: 'Multipart initiate successful!'
      });
    } catch (err) {
      console.error('Multipart initiate test failed:', err);
      setApiTestResult({
        success: false,
        error: err.message,
        message: 'Multipart initiate failed!'
      });
    } finally {
      setTestLoading(false);
    }
  };

  return (
    <div style={{ padding: '20px', fontFamily: 'monospace' }}>
      <h1>Authentication Debug</h1>
      
      <div style={{ background: '#f5f5f5', padding: '15px', margin: '10px 0', borderRadius: '5px' }}>
        <h2>Authentication State</h2>
        <p><strong>Is Authenticated:</strong> {isAuthenticated ? '✅ Yes' : '❌ No'}</p>
        <p><strong>Is Loading:</strong> {isLoading ? '⏳ Loading...' : '✅ Ready'}</p>
        <p><strong>Error:</strong> {error || 'None'}</p>
        <p><strong>Has Token (AuthService):</strong> {token ? '✅ Yes' : '❌ No'}</p>
        {token && (
          <p><strong>Token Preview:</strong> {token.substring(0, 50)}...</p>
        )}
      </div>

      <div style={{ background: '#e8f5e8', padding: '15px', margin: '10px 0', borderRadius: '5px' }}>
        <h2>localStorage Status</h2>
        <p><strong>Has localStorage Token:</strong> {localStorageToken?.access_token ? '✅ Yes' : '❌ No'}</p>
        {localStorageToken?.access_token && (
          <>
            <p><strong>localStorage Token Preview:</strong> {localStorageToken.access_token.substring(0, 50)}...</p>
            <p><strong>Token Timestamp:</strong> {localStorageToken.timestamp ? new Date(localStorageToken.timestamp).toLocaleString() : 'Not set'}</p>
            <p><strong>User Info:</strong> {localStorageToken.user_info ? '✅ Present' : '❌ Missing'}</p>
            <p><strong>User ID:</strong> {localStorageToken.user_id || localStorageToken.user_info?.sub || 'Not set'}</p>
          </>
        )}
        
        <div style={{ marginTop: '10px' }}>
          <button 
            onClick={handleRefresh}
            style={{ 
              backgroundColor: '#28a745', 
              color: 'white', 
              border: 'none', 
              padding: '5px 10px', 
              margin: '2px',
              borderRadius: '3px',
              cursor: 'pointer',
              fontSize: '12px'
            }}
          >
            Refresh Debug Info
          </button>
          
          <button 
            onClick={clearLocalStorage}
            style={{ 
              backgroundColor: '#dc3545', 
              color: 'white', 
              border: 'none', 
              padding: '5px 10px', 
              margin: '2px',
              borderRadius: '3px',
              cursor: 'pointer',
              fontSize: '12px'
            }}
          >
            Clear localStorage
          </button>
        </div>
      </div>

      <div style={{ background: '#f0f8ff', padding: '15px', margin: '10px 0', borderRadius: '5px' }}>
        <h2>Actions</h2>
        <button 
          onClick={login}
          style={{ 
            backgroundColor: '#007bff', 
            color: 'white', 
            border: 'none', 
            padding: '10px 15px', 
            margin: '5px',
            borderRadius: '3px',
            cursor: 'pointer'
          }}
        >
          Login
        </button>
        
        <button 
          onClick={logout}
          style={{ 
            backgroundColor: '#dc3545', 
            color: 'white', 
            border: 'none', 
            padding: '10px 15px', 
            margin: '5px',
            borderRadius: '3px',
            cursor: 'pointer'
          }}
        >
          Logout
        </button>
      </div>

      <div style={{ background: '#ffe6e6', padding: '15px', margin: '10px 0', borderRadius: '5px' }}>
        <h2>API Tests</h2>
        <button 
          onClick={testApiCall}
          disabled={testLoading || !isAuthenticated}
          style={{ 
            backgroundColor: '#28a745', 
            color: 'white', 
            border: 'none', 
            padding: '10px 15px', 
            margin: '5px',
            borderRadius: '3px',
            cursor: testLoading || !isAuthenticated ? 'not-allowed' : 'pointer',
            opacity: testLoading || !isAuthenticated ? 0.6 : 1
          }}
        >
          {testLoading ? 'Testing...' : 'Test GET /api/upload/videos'}
        </button>

        <button 
          onClick={testMultipartInitiate}
          disabled={testLoading || !isAuthenticated}
          style={{ 
            backgroundColor: '#17a2b8', 
            color: 'white', 
            border: 'none', 
            padding: '10px 15px', 
            margin: '5px',
            borderRadius: '3px',
            cursor: testLoading || !isAuthenticated ? 'not-allowed' : 'pointer',
            opacity: testLoading || !isAuthenticated ? 0.6 : 1
          }}
        >
          {testLoading ? 'Testing...' : 'Test POST /api/upload/multipart/initiate'}
        </button>

        {apiTestResult && (
          <div style={{
            marginTop: '15px',
            padding: '10px',
            backgroundColor: apiTestResult.success ? '#d4edda' : '#f8d7da',
            border: `1px solid ${apiTestResult.success ? '#c3e6cb' : '#f5c6cb'}`,
            borderRadius: '5px'
          }}>
            <h3 style={{ color: apiTestResult.success ? '#155724' : '#721c24' }}>
              {apiTestResult.message}
            </h3>
            {apiTestResult.success ? (
              <pre style={{ color: '#155724', fontSize: '12px' }}>
                {JSON.stringify(apiTestResult.data, null, 2)}
              </pre>
            ) : (
              <p style={{ color: '#721c24' }}>
                <strong>Error:</strong> {apiTestResult.error}
              </p>
            )}
          </div>
        )}
      </div>

      <div style={{ background: '#f8f9fa', padding: '15px', margin: '10px 0', borderRadius: '5px' }}>
        <h2>Current URL</h2>
        <p>{window.location.href}</p>
        
        <h2>URL Parameters</h2>
        <pre>{JSON.stringify(Object.fromEntries(new URLSearchParams(window.location.search)), null, 2)}</pre>
      </div>
    </div>
  );
};

export default DebugPage; 