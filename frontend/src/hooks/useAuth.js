import { useState, useEffect, useCallback } from 'react';
import authService from '../services/authService';
import { parseUrlParams } from '../utils/pkce';

export const useAuth = () => {
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);

  /**
   * Initialize authentication state
   */
  const initializeAuth = useCallback(async () => {
    setIsLoading(true);
    setError(null);

    try {
      // Check if this is an OAuth callback
      const { code, state, error: oauthError } = parseUrlParams();
      
      if (oauthError) {
        throw new Error(`OAuth error: ${oauthError}`);
      }

      if (code && state) {
        // Handle OAuth callback
        console.log('Handling OAuth callback...');
        await authService.handleCallback();
        setIsAuthenticated(true);
        console.log('OAuth callback handled successfully');
      } else {
        // Try silent login with refresh token
        console.log('Attempting silent login...');
        const success = await authService.tryRefreshOnStartup();
        setIsAuthenticated(success);
        
        if (success) {
          console.log('Silent login successful');
        } else {
          console.log('Silent login failed - user needs to authenticate');
        }
      }
    } catch (err) {
      console.error('Authentication initialization failed:', err);
      setError(err.message);
      setIsAuthenticated(false);
      authService.clearAccessToken();
    } finally {
      setIsLoading(false);
    }
  }, []);

  /**
   * Login user
   */
  const login = useCallback(async () => {
    try {
      setError(null);
      await authService.login();
    } catch (err) {
      console.error('Login failed:', err);
      setError(err.message);
    }
  }, []);

  /**
   * Logout user
   */
  const logout = useCallback(async () => {
    try {
      setIsLoading(true);
      await authService.logout();
      
      // Force state update
      setIsAuthenticated(false);
      setError(null);
      
      console.log('User logged out, refreshing page...');
    } catch (err) {
      console.error('Logout failed:', err);
      setError(err.message);
      // Even if logout fails, clear local state
      setIsAuthenticated(false);
    } finally {
      // Always refresh page to ensure clean state
      window.location.reload();
    }
  }, []);

  /**
   * Make authenticated API request
   */
  const apiRequest = useCallback(async (url, options = {}) => {
    try {
      return await authService.apiRequest(url, options);
    } catch (err) {
      if (err.message === 'Authentication failed') {
        setIsAuthenticated(false);
      }
      throw err;
    }
  }, []);

  /**
   * Get current access token
   */
  const getAccessToken = useCallback(() => {
    return authService.getAccessToken();
  }, []);

  // Initialize auth on mount
  useEffect(() => {
    initializeAuth();
  }, [initializeAuth]);

  return {
    isAuthenticated,
    isLoading,
    error,
    login,
    logout,
    apiRequest,
    getAccessToken,
    retry: initializeAuth
  };
}; 