// Custom PKCE implementation - more reliable than oauth-pkce library
export const AUTH_CONFIG = {
  clientId: 'public-client',
  redirectUri: window.location.origin,
  authorizationEndpoint: '/oauth2/authorize',
  tokenEndpoint: '/oauth2/token',
  logoutEndpoint: '/oauth2/logout',
  scope: 'openid profile',
  apiBaseUrl:'http://localhost:8080'
};

/**
 * Generate a random string for code verifier
 */
const generateRandomString = (length) => {
  const possible = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~';
  let text = '';
  for (let i = 0; i < length; i++) {
    text += possible.charAt(Math.floor(Math.random() * possible.length));
  }
  return text;
};

/**
 * Generate code verifier for PKCE
 */
export const generateCodeVerifier = () => {
  return generateRandomString(128);
};

/**
 * Generate code challenge from verifier using SHA256
 */
export const generateCodeChallenge = async (codeVerifier) => {
  const encoder = new TextEncoder();
  const data = encoder.encode(codeVerifier);
  const digest = await window.crypto.subtle.digest('SHA-256', data);
  
  // Convert to base64url
  return btoa(String.fromCharCode(...new Uint8Array(digest)))
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=/g, '');
};

/**
 * Generate PKCE code challenge and verifier
 */
export const generatePKCE = async () => {
  const codeVerifier = generateCodeVerifier();
  const codeChallenge = await generateCodeChallenge(codeVerifier);
  
  return {
    codeVerifier,
    codeChallenge
  };
};

/**
 * Generate random state for OAuth2 flow
 */
export const generateState = () => {
  return Math.random().toString(36).substring(2, 15) + Math.random().toString(36).substring(2, 15);
};

/**
 * Store PKCE parameters in sessionStorage
 */
export const storePKCEParams = (codeVerifier, state) => {
  sessionStorage.setItem('pkce_code_verifier', codeVerifier);
  sessionStorage.setItem('oauth_state', state);
};

/**
 * Retrieve PKCE parameters from sessionStorage
 */
export const retrievePKCEParams = () => {
  return {
    codeVerifier: sessionStorage.getItem('pkce_code_verifier'),
    state: sessionStorage.getItem('oauth_state')
  };
};

/**
 * Clear PKCE parameters from sessionStorage
 */
export const clearPKCEParams = () => {
  sessionStorage.removeItem('pkce_code_verifier');
  sessionStorage.removeItem('oauth_state');
};

/**
 * Build authorization URL with PKCE parameters
 */
export const buildAuthorizationUrl = async () => {
  const { codeVerifier, codeChallenge } = await generatePKCE();
  const state = generateState();
  
  // Store PKCE params for later use
  storePKCEParams(codeVerifier, state);
  
  const params = new URLSearchParams({
    response_type: 'code',
    client_id: AUTH_CONFIG.clientId,
    redirect_uri: AUTH_CONFIG.redirectUri,
    scope: AUTH_CONFIG.scope,
    state: state,
    code_challenge: codeChallenge,
    code_challenge_method: 'S256'
  });
  
  // Use full URL to gateway instead of relative path
  return `${AUTH_CONFIG.apiBaseUrl}${AUTH_CONFIG.authorizationEndpoint}?${params.toString()}`;
};

/**
 * Exchange authorization code for tokens
 */
export const exchangeCodeForTokens = async (code, state) => {
  const { codeVerifier, state: storedState } = retrievePKCEParams();
  
  // Verify state parameter
  if (state !== storedState) {
    throw new Error('Invalid state parameter');
  }
  
  if (!codeVerifier) {
    throw new Error('Code verifier not found');
  }
  
  const params = new URLSearchParams({
    grant_type: 'authorization_code',
    client_id: AUTH_CONFIG.clientId,
    code: code,
    redirect_uri: AUTH_CONFIG.redirectUri,
    code_verifier: codeVerifier
  });
  
  const response = await fetch(`${AUTH_CONFIG.apiBaseUrl}${AUTH_CONFIG.tokenEndpoint}`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
    body: params.toString(),
    credentials: 'include' // Important for refresh token cookie
  });
  
  if (!response.ok) {
    throw new Error(`Token exchange failed: ${response.status}`);
  }
  
  const tokens = await response.json();
  
  // Clear PKCE params after successful exchange
  clearPKCEParams();
  
  return tokens;
};

/**
 * Refresh access token using refresh token cookie
 */
export const refreshAccessToken = async () => {
  const params = new URLSearchParams({
    grant_type: 'refresh_token',
    client_id: AUTH_CONFIG.clientId
  });
  
  const response = await fetch(`${AUTH_CONFIG.apiBaseUrl}${AUTH_CONFIG.tokenEndpoint}`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
    body: params.toString(),
    credentials: 'include' // Refresh token is in HTTP-only cookie
  });
  
  if (!response.ok) {
    throw new Error(`Token refresh failed: ${response.status}`);
  }
  
  return await response.json();
};

/**
 * Parse URL parameters
 */
export const parseUrlParams = () => {
  const params = new URLSearchParams(window.location.search);
  return {
    code: params.get('code'),
    state: params.get('state'),
    error: params.get('error'),
    error_description: params.get('error_description')
  };
}; 