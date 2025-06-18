import authService from './authService';

// Base configuration - все запросы идут через Gateway
const GATEWAY_URL = process.env.NODE_ENV === 'development' 
  ? 'http://localhost:8080' 
  : window.location.origin;

class ViewSessionService {
  constructor() {
    this.currentSession = null;
    this.heartbeatInterval = null;
    this.baseUrl = `${GATEWAY_URL}/api/streaming`;
  }

  /**
   * Get auth token from authService (preferred) or fallback to localStorage
   * This ensures consistency with the main authentication system
   */
  getAuthToken() {
    // First try to get token from authService
    const token = authService.getAccessToken();
    if (token) {
      return token;
    }
    
    // Fallback to localStorage for compatibility
    try {
      const tokens = JSON.parse(localStorage.getItem('auth_tokens'));
      return tokens?.access_token;
    } catch (error) {
      return null;
    }
  }

  /**
   * Get current user ID from auth tokens
   */
  getCurrentUserId() {
    try {
      const tokens = JSON.parse(localStorage.getItem('auth_tokens'));
      return tokens?.user_info?.sub || tokens?.user_id;
    } catch (error) {
      return null;
    }
  }

  /**
   * Get common headers with authentication
   */
  getCommonHeaders() {
    const headers = {
      'Content-Type': 'application/json'
    };

    const token = this.getAuthToken();
    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }

    return headers;
  }

  /**
   * Start a new viewing session
   */
  async startViewSession(videoId, userId = null) {
    try {
      console.log('🚀 Starting view session through Gateway...');
      console.log('Video ID:', videoId);
      console.log('User ID:', userId);
      
      const url = `${this.baseUrl}/sessions/start?videoId=${videoId}${userId ? `&userId=${userId}` : ''}`;
      
      const response = await authService.apiRequest(url, {
        method: 'POST'
      });

      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(`Failed to start view session: ${response.status} - ${errorText}`);
      }

      const session = await response.json();
      this.currentSession = session;
      
      // Start heartbeat interval (send heartbeat every 10 seconds)
      this.startHeartbeat();
      
      console.log('✅ View session started:', session.sessionId);
      return session;
    } catch (error) {
      console.error('❌ Error starting view session:', error);
      throw error;
    }
  }

  /**
   * Send heartbeat to update session progress
   */
  async sendHeartbeat(currentPosition = 0, watchDuration = 0, quality = null) {
    if (!this.currentSession) {
      return;
    }

    try {
      const heartbeatData = {
        sessionId: this.currentSession.sessionId,
        currentPosition: Math.floor(currentPosition),
        watchDuration: Math.floor(watchDuration),
        quality: quality
      };

      const response = await authService.apiRequest(`${this.baseUrl}/sessions/heartbeat`, {
        method: 'POST',
        body: JSON.stringify(heartbeatData)
      });

      if (!response.ok) {
        console.warn('⚠️ Failed to send heartbeat:', response.status);
      }
    } catch (error) {
      console.error('❌ Error sending heartbeat:', error);
    }
  }

  /**
   * End viewing session
   */
  async endViewSession(isComplete = false) {
    if (!this.currentSession) {
      return;
    }

    try {
      const response = await authService.apiRequest(`${this.baseUrl}/sessions/end`, {
        method: 'POST',
        body: JSON.stringify({
          sessionId: this.currentSession.sessionId,
          isComplete: isComplete
        })
      });

      if (!response.ok) {
        console.warn('⚠️ Failed to end view session:', response.status);
      }

      console.log('✅ View session ended:', this.currentSession.sessionId);
    } catch (error) {
      console.error('❌ Error ending view session:', error);
    } finally {
      this.cleanup();
    }
  }

  /**
   * Start heartbeat interval
   */
  startHeartbeat() {
    // Clear existing interval if any
    this.stopHeartbeat();
    
    // Send heartbeat every 10 seconds
    this.heartbeatInterval = setInterval(() => {
      this.sendHeartbeat();
    }, 10000);
  }

  /**
   * Stop heartbeat interval
   */
  stopHeartbeat() {
    if (this.heartbeatInterval) {
      clearInterval(this.heartbeatInterval);
      this.heartbeatInterval = null;
    }
  }

  /**
   * Cleanup session data
   */
  cleanup() {
    this.stopHeartbeat();
    this.currentSession = null;
  }

  /**
   * Update session with current playback state
   */
  updatePlaybackState(currentPosition, watchDuration, quality) {
    if (this.currentSession) {
      this.sendHeartbeat(currentPosition, watchDuration, quality);
    }
  }

  /**
   * Get current session
   */
  getCurrentSession() {
    return this.currentSession;
  }
}

// Create singleton instance
const viewSessionService = new ViewSessionService();

export default viewSessionService; 