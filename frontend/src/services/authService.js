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
  setAccessToken(token) {
    this.accessToken = token;
    this.startRefreshTimer();
  }

  /**
   * Clear access token and stop refresh timer
   */
  clearAccessToken() {
    this.accessToken = null;
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
      this.setAccessToken(tokens.access_token);
      
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
      this.setAccessToken(tokens.access_token);
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