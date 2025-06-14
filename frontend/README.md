# Video Hosting Platform - Frontend

React-based frontend for the Video Hosting Platform with OAuth2 PKCE authentication.

## 🚀 Features

- **OAuth2 PKCE Authentication** - Secure authentication flow with access tokens (5min TTL) and refresh tokens (HTTP-only cookies)
- **Automatic Token Refresh** - Seamless token renewal every 4.5 minutes
- **Protected Routes** - All routes require authentication
- **Responsive Design** - Modern UI with red & white color scheme
- **Error Handling** - Comprehensive error handling and retry mechanisms

## 🏗️ Architecture

```
src/
├── components/
│   ├── Auth/
│   │   └── ProtectedRoute.js     # Route protection wrapper
│   ├── Common/
│   │   ├── LoadingSpinner.js     # Loading indicator
│   │   └── ErrorMessage.js       # Error display component
│   └── Layout/
│       └── Header.js             # Navigation header
├── hooks/
│   └── useAuth.js                # Authentication hook
├── pages/
│   └── HomePage.js               # Main dashboard page
├── services/
│   └── authService.js            # OAuth2 service singleton
├── utils/
│   └── pkce.js                   # PKCE utilities
├── App.js                        # Main app component with routing
└── index.js                      # Application entry point
```

## 🔐 Authentication Flow

1. **Initial Load**: Check for valid access token in memory
2. **Silent Login**: Attempt token refresh using HTTP-only refresh token cookie
3. **Redirect to Login**: If no valid token, redirect to OAuth2 authorization endpoint
4. **OAuth2 PKCE Flow**: 
   - Generate code challenge/verifier
   - Redirect to backend login page (`/oauth2/authorization/public-client`)
   - Handle authorization code callback
   - Exchange code for tokens
5. **Token Management**:
   - Store access token in memory (5 min TTL)
   - Refresh token stored in HTTP-only cookie (7 days TTL)
   - Auto-refresh every 4.5 minutes
6. **API Requests**: All API calls include Bearer token and handle 401 responses

## 📋 Prerequisites

- Node.js 16+ 
- Backend services running (Gateway on port 8080)
- Valid OAuth2 configuration in authentication service

## 🛠️ Installation

1. **Install dependencies**:
```bash
npm install
```

2. **Start development server**:
```bash
npm start
```

The app will be available at `http://localhost:3000`

## ⚙️ Configuration

The app is configured to work with:
- **API Gateway**: `http://localhost:8080` (proxied)
- **OAuth2 Client ID**: `public-client`
- **OAuth2 Endpoints**:
  - Authorization: `/oauth2/authorization/public-client`
  - Token: `/oauth2/token`
  - Logout: `/oauth2/logout`

## 🔧 Development

### Environment Variables

No environment variables required - configuration is handled automatically:
- Development: Uses `http://localhost:8080` as API base
- Production: Uses current origin

### Testing Authentication

1. Start backend services
2. Start frontend: `npm start`
3. Navigate to `http://localhost:3000`
4. You'll be redirected to login if not authenticated
5. Complete OAuth2 flow (login/Google)
6. You'll be redirected back with valid tokens

### Token Debugging

Open browser console to see authentication flow:
- Token refresh logs every 4.5 minutes
- OAuth2 callback handling
- API request/response logs

## 📚 Available Scripts

- `npm start` - Start development server
- `npm build` - Build for production
- `npm test` - Run tests
- `npm eject` - Eject from Create React App

## 🔄 OAuth2 PKCE Implementation

Uses `oauth-pkce` library for secure PKCE flow:

```javascript
// Generate PKCE parameters
const { codeVerifier, codeChallenge } = await generatePKCE();

// Build authorization URL
const authUrl = `/oauth2/authorization/public-client?${params}`;

// Exchange code for tokens
const tokens = await exchangeCodeForTokens(code, state);
```

## 🎨 UI Components

### Color Scheme
- **Primary**: `#d32f2f` (Red)
- **Primary Dark**: `#b71c1c` 
- **Background**: `#fafafa` (Light Gray)
- **Text**: `#333` / `#666`

### Interactive Elements
- Hover effects on buttons and links
- Loading spinners during authentication
- Error messages with retry functionality
- Responsive design for mobile/desktop

## 🔒 Security Features

- **PKCE Flow**: Proof Key for Code Exchange prevents authorization code interception
- **Memory-only Access Tokens**: No persistent storage of access tokens
- **HTTP-only Refresh Tokens**: Secure cookie storage prevents XSS attacks
- **State Parameter**: CSRF protection during OAuth2 flow
- **Automatic Cleanup**: Clear tokens on logout and errors

## 🚧 Future Implementation

The following placeholders are ready for implementation:
- Video upload functionality
- Video list/browse page
- Video player with HLS streaming
- Analytics dashboard

## 🐛 Troubleshooting

### Authentication Issues
- Check backend services are running
- Verify OAuth2 client configuration
- Check browser console for error messages
- Clear cookies if needed: `document.cookie.split(";").forEach(c => document.cookie = c.replace(/^ +/, "").replace(/=.*/, "=;expires=Thu, 01 Jan 1970 00:00:00 UTC;path=/;"));`

### CORS Issues
- Ensure gateway has proper CORS configuration
- Check `proxy` setting in package.json

### Token Refresh Issues
- Verify refresh token cookie is present
- Check if refresh token hasn't expired (7 days)
- Look for network errors in browser dev tools 