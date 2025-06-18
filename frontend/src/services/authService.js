import {
  buildAuthorizationUrl,
  exchangeCodeForTokens,
  refreshAccessToken,
  parseUrlParams,
  AUTH_CONFIG
} from '../utils/pkce';

class AuthService {
  constructor() {
    this.accessToken = null;
    this.refreshTimer = null;
    this.isRefreshing = false;
    this.refreshPromise = null;
    
    // Try to restore token from localStorage on initialization
    this.loadTokenFromStorage();
  }

  /**
   * Load access token from localStorage
   */
  loadTokenFromStorage() {
    try {
      const tokens = JSON.parse(localStorage.getItem('auth_tokens'));
      if (tokens?.access_token) {
        this.accessToken = tokens.access_token;
        this.startRefreshTimer();
        console.log('Access token restored from localStorage');
      }
    } catch (error) {
      console.error('Failed to load token from localStorage:', error);
      this.clearTokenFromStorage();
    }
  }

  /**
   * Save access token to localStorage
   */
  saveTokenToStorage(token, additionalData = {}) {
    try {
      const tokenData = {
        access_token: token,
        timestamp: Date.now(),
        ...additionalData
      };
      localStorage.setItem('auth_tokens', JSON.stringify(tokenData));
      console.log('Access token saved to localStorage');
    } catch (error) {
      console.error('Failed to save token to localStorage:', error);
    }
  }

  /**
   * Clear access token from localStorage
   */
  clearTokenFromStorage() {
    try {
      localStorage.removeItem('auth_tokens');
      console.log('Access token cleared from localStorage');
    } catch (error) {
      console.error('Failed to clear token from localStorage:', error);
    }
  }

  /**
   * Check if user is authenticated
   */
  isAuthenticated() {
    return !!this.accessToken;
  }

  /**
   * Get current access token
   */
  getAccessToken() {
    return this.accessToken;
  }

  /**
   * Set access token and start refresh timer
   */
  setAccessToken(token, additionalData = {}) {
    this.accessToken = token;
    this.saveTokenToStorage(token, additionalData);
    this.startRefreshTimer();
  }

  /**
   * Clear access token and stop refresh timer
   */
  clearAccessToken() {
    this.accessToken = null;
    this.clearTokenFromStorage();
    this.stopRefreshTimer();
  }

  /**
   * Start OAuth2 PKCE login flow
   */
  async login() {
    try {
      const authUrl = await buildAuthorizationUrl();
      window.location.href = authUrl;
    } catch (error) {
      console.error('Failed to start login flow:', error);
      throw error;
    }
  }

  /**
   * Handle OAuth2 callback
   */
  async handleCallback() {
    const { code, state, error, error_description } = parseUrlParams();

    if (error) {
      throw new Error(`OAuth2 error: ${error} - ${error_description}`);
    }

    if (!code || !state) {
      throw new Error('Missing authorization code or state');
    }

    try {
      const tokens = await exchangeCodeForTokens(code, state);
      
      // Save access token with additional token information
      const additionalData = {};
      if (tokens.user_info) {
        additionalData.user_info = tokens.user_info;
      }
      if (tokens.user_id) {
        additionalData.user_id = tokens.user_id;
      }
      if (tokens.id_token) {
        additionalData.id_token = tokens.id_token;
      }
      
      this.setAccessToken(tokens.access_token, additionalData);
      
      // Clean up URL parameters
      window.history.replaceState({}, document.title, window.location.pathname);
      
      return tokens;
    } catch (error) {
      console.error('Token exchange failed:', error);
      throw error;
    }
  }

  /**
   * Refresh access token
   */
  async refreshToken() {
    if (this.isRefreshing) {
      return this.refreshPromise;
    }

    this.isRefreshing = true;
    this.refreshPromise = this._performRefresh();

    try {
      const result = await this.refreshPromise;
      return result;
    } finally {
      this.isRefreshing = false;
      this.refreshPromise = null;
    }
  }

  async _performRefresh() {
    try {
      const tokens = await refreshAccessToken();
      
      // Preserve existing additional data from localStorage
      let additionalData = {};
      try {
        const existingTokens = JSON.parse(localStorage.getItem('auth_tokens'));
        if (existingTokens) {
          // Keep user_info and other data that doesn't change during refresh
          if (existingTokens.user_info) {
            additionalData.user_info = existingTokens.user_info;
          }
          if (existingTokens.user_id) {
            additionalData.user_id = existingTokens.user_id;
          }
          if (existingTokens.id_token) {
            additionalData.id_token = existingTokens.id_token;
          }
        }
      } catch (error) {
        console.warn('Failed to preserve existing token data:', error);
      }
      
      // Add new token information if available
      if (tokens.user_info) {
        additionalData.user_info = tokens.user_info;
      }
      if (tokens.user_id) {
        additionalData.user_id = tokens.user_id;
      }
      if (tokens.id_token) {
        additionalData.id_token = tokens.id_token;
      }
      
      this.setAccessToken(tokens.access_token, additionalData);
      console.log('Access token refreshed successfully');
      return tokens;
    } catch (error) {
      console.error('Failed to refresh token:', error);
      this.logout();
      throw error;
    }
  }

  /**
   * Start automatic token refresh timer (4.5 minutes for 5 minute token)
   */
  startRefreshTimer() {
    this.stopRefreshTimer();
    
    // Refresh 30 seconds before expiry (4.5 minutes for 5 minute token)
    const refreshInterval = 4.5 * 60 * 1000; // 4.5 minutes in milliseconds
    
    this.refreshTimer = setTimeout(async () => {
      try {
        await this.refreshToken();
      } catch (error) {
        console.error('Automatic token refresh failed:', error);
      }
    }, refreshInterval);

    console.log('Token refresh timer started (4.5 minutes)');
  }

  /**
   * Stop automatic token refresh timer
   */
  stopRefreshTimer() {
    if (this.refreshTimer) {
      clearTimeout(this.refreshTimer);
      this.refreshTimer = null;
      console.log('Token refresh timer stopped');
    }
  }

  /**
   * Logout user
   */
  async logout() {
    try {
      console.log('Starting logout process...');
      
      // Clear local tokens first
      this.clearAccessToken();

      // Call backend logout endpoint to clear refresh token cookie
      await fetch(`${AUTH_CONFIG.apiBaseUrl}${AUTH_CONFIG.logoutEndpoint}`, {
        method: 'POST',
        credentials: 'include'
      });

      console.log('Backend logout call completed');
    } catch (error) {
      console.error('Backend logout error:', error);
      // Even if backend fails, we still clear local tokens
    } finally {
      // Ensure tokens are cleared regardless of backend response
      this.clearAccessToken();
      console.log('Local tokens cleared, logout completed');
    }
  }

  /**
   * Try to refresh token on app startup (silent login)
   */
  async tryRefreshOnStartup() {
    try {
      // If we already have a token loaded from localStorage, check if we need to refresh
      if (this.accessToken) {
        console.log('Access token found in localStorage, checking if refresh needed...');
        
        // Check if token was saved recently (within last 4 minutes)
        const tokens = JSON.parse(localStorage.getItem('auth_tokens'));
        const tokenAge = Date.now() - (tokens?.timestamp || 0);
        const fourMinutes = 4 * 60 * 1000;
        
        if (tokenAge < fourMinutes) {
          console.log('Token is recent, no refresh needed');
          return true;
        }
        
        console.log('Token is older than 4 minutes, attempting refresh...');
      }
      
      // Try to refresh the token
      await this.refreshToken();
      return true;
    } catch (error) {
      console.log('Silent login failed:', error.message);
      return false;
    }
  }

  /**
   * Make authenticated API request
   */
  async apiRequest(url, options = {}) {
    const token = this.getAccessToken();
    
    if (!token) {
      throw new Error('No access token available');
    }

    // Build full URL if relative path is provided
    const fullUrl = url.startsWith('http') ? url : `${AUTH_CONFIG.apiBaseUrl}${url}`;

    const config = {
      ...options,
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json',
        ...options.headers
      },
      credentials: 'include'
    };

    try {
      const response = await fetch(fullUrl, config);
      
      if (response.status === 401) {
        // Token might be expired, try to refresh
        try {
          await this.refreshToken();
          // Retry the original request with new token
          config.headers['Authorization'] = `Bearer ${this.getAccessToken()}`;
          return await fetch(fullUrl, config);
        } catch (refreshError) {
          // Refresh failed, redirect to login
          this.login();
          throw new Error('Authentication failed');
        }
      }
      
      return response;
    } catch (error) {
      console.error('API request failed:', error);
      throw error;
    }
  }
}

// Export singleton instance
export default new AuthService(); 