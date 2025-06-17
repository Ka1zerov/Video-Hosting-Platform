# Technical Documentation: Video Hosting Platform - Frontend Microservice

## 1. Overview

### Microservice Name
`video-hosting-frontend`

### Purpose and Responsibilities
The frontend microservice serves as the user interface layer for the distributed video hosting platform. It provides a React-based web application that handles user authentication, video browsing, video playback, and video upload functionality. The service acts as the primary client interface, orchestrating communication with backend microservices through an API Gateway.

### Key Technologies and Frameworks
- **Frontend Framework**: React 18.2.0
- **UI Framework**: Material-UI (MUI) 5.15.0
- **Routing**: React Router DOM 6.8.0
- **HTTP Client**: Axios 1.6.0
- **Video Streaming**: HLS.js 1.4.12, React Player 2.13.0
- **Authentication**: Custom OAuth2 PKCE implementation
- **Build System**: Create React App (react-scripts 5.0.1)

## 2. API Specification

### External API Endpoints (Consumed)
The frontend consumes APIs through the API Gateway on port 8080:

#### Authentication Endpoints
- **POST** `/oauth2/token` - Exchange authorization code for tokens
- **POST** `/oauth2/logout` - Clear refresh token cookie
- **GET** `/oauth2/authorize` - OAuth2 authorization endpoint

#### Video Metadata Endpoints
- **GET** `/api/metadata/videos?page={page}&size={size}` - Get paginated video list
- **GET** `/api/metadata/videos/search?query={query}&page={page}&size={size}` - Search videos
- **GET** `/api/metadata/videos/popular?page={page}&size={size}` - Get popular videos
- **GET** `/api/metadata/videos/recent?page={page}&size={size}` - Get recently watched videos
- **GET** `/api/metadata/videos/{videoId}` - Get video by ID

#### Video Streaming Endpoints
- **POST** `/api/streaming/play` - Request video stream information
- **GET** `/api/streaming/playlist/{videoId}/master.m3u8` - Get HLS master playlist

#### Video Upload Endpoints
- **POST** `/api/upload/presigned-url` - Request presigned upload URL
- **POST** `/api/upload/complete` - Complete multipart upload
- **POST** `/api/upload/metadata` - Save video metadata

### Request/Response Format
- **Content-Type**: `application/json` for API requests, `application/x-www-form-urlencoded` for OAuth2
- **Authentication**: Bearer token in Authorization header
- **Response Format**: JSON with consistent error handling

### Authentication/Authorization
- **OAuth2 PKCE Flow**: Proof Key for Code Exchange for secure authentication
- **Access Token**: 5-minute TTL, stored in memory
- **Refresh Token**: 7-day TTL, stored in HTTP-only cookie
- **Automatic Token Refresh**: Every 4.5 minutes

## 3. Architectural Patterns

### Single Page Application (SPA) Pattern
- React-based SPA with client-side routing
- Protected routes requiring authentication
- Component-based architecture with reusable UI elements

### Service Layer Pattern
- Dedicated service classes for API communication:
  - `AuthService` - Authentication management
  - `VideoService` - Video metadata operations
  - `UploadService` - File upload operations

### Observer Pattern (React Hooks)
- Custom hooks for state management (`useAuth`)
- React Context for global state sharing

### Proxy Pattern
- API Gateway abstraction for backend service communication
- Service layer abstracts direct API calls from components

## 4. Communication Protocols

### Internal Communication
- **HTTP/HTTPS**: All API communication through REST endpoints
- **WebSocket**: Not currently implemented (prepared for real-time features)

### External Communication
- **HTTP/HTTPS**: Communication with API Gateway (localhost:8080 in development)
- **HLS (HTTP Live Streaming)**: Video content delivery via .m3u8 playlists
- **Multipart Upload**: Large file upload via presigned URLs to S3

### Protocol Standards
- RESTful API design principles
- OAuth2 authorization framework
- HLS streaming protocol for video delivery

## 5. IETF RFC References

### OAuth2 and Security
- **RFC 6749**: OAuth 2.0 Authorization Framework
- **RFC 7636**: Proof Key for Code Exchange (PKCE)
- **RFC 7519**: JSON Web Token (JWT) - for access tokens
- **RFC 6265**: HTTP State Management Mechanism (Cookies) - for refresh tokens

### HTTP Protocol
- **RFC 7231**: HTTP/1.1 Semantics and Content
- **RFC 7235**: HTTP/1.1 Authentication
- **RFC 6454**: The Web Origin Concept - for CORS handling

### Streaming Protocol
- **RFC 8216**: HTTP Live Streaming (HLS) - for video content delivery

## 6. Configuration & Environment

### Environment Variables
No explicit environment variables required. Configuration is handled automatically:
- **Development**: `http://localhost:8080` as API base URL
- **Production**: Uses current origin as API base URL

### Configuration Files
- `package.json` - Project dependencies and scripts
- `public/index.html` - HTML template with app metadata
- Service configurations embedded in code:
  ```javascript
  const AUTH_CONFIG = {
    clientId: 'public-client',
    redirectUri: window.location.origin,
    scope: 'openid profile',
    apiBaseUrl: 'http://localhost:8080'
  };
  ```

### Dependencies
- **Core Dependencies**: React ecosystem, Material-UI, Axios
- **Development Dependencies**: React Scripts for build tooling
- **Service Discovery**: Static configuration pointing to API Gateway

## 7. Observability

### Logging Practices
- **Console Logging**: Comprehensive logging for authentication flow
- **Error Logging**: Service-level error handling with context
- **Authentication Events**: Token refresh cycles, login/logout events
- **API Request Logging**: Request/response logging for debugging

### Monitoring Integration
- **Web Vitals**: Performance monitoring using `web-vitals` library
- **Browser DevTools**: Console logging for development debugging
- **Network Monitoring**: HTTP request/response monitoring via browser

### Error Handling
- Centralized error handling in service classes
- User-friendly error messages in UI components
- Automatic retry mechanisms for token refresh
- Graceful degradation for failed API calls

## 8. Security

### Security Mechanisms
- **OAuth2 PKCE**: Prevents authorization code interception attacks
- **Memory-only Access Tokens**: No persistent storage to prevent XSS
- **HTTP-only Refresh Tokens**: Secure cookie storage prevents client-side access
- **State Parameter**: CSRF protection during OAuth2 flow
- **Automatic Token Cleanup**: Clear tokens on logout and errors

### CORS Configuration
- Configured to work with API Gateway
- Credentials included for cookie-based refresh tokens

### Input Validation
- URL parameter validation for OAuth2 callbacks
- Query parameter encoding for search functionality
- File type validation for upload operations

### Notable Security Considerations
- **XSS Prevention**: No persistent token storage in localStorage
- **CSRF Protection**: State parameter validation in OAuth2 flow
- **Token Expiry**: Short-lived access tokens (5 minutes)
- **Secure Cookies**: HTTP-only refresh token cookies

## 9. AI/ML Usage

Currently, no AI/ML functionality is implemented in the frontend microservice. The architecture is prepared for future AI features:
- Video recommendation systems
- Content analysis and tagging
- Automated video processing status updates

## 10. Deployment & Runtime

### Application Structure
```
frontend/
├── src/
│   ├── components/          # React components
│   │   ├── Auth/           # Authentication components
│   │   ├── Common/         # Reusable UI components
│   │   ├── Layout/         # Layout components
│   │   ├── Upload/         # Upload-related components
│   │   └── Video/          # Video playback components
│   ├── pages/              # Page components
│   ├── services/           # API service classes
│   ├── hooks/              # Custom React hooks
│   └── utils/              # Utility functions
├── public/                 # Static assets
└── build/                  # Production build output
```

### Build Process
- **Development**: `npm start` - Starts development server on port 3000
- **Production**: `npm run build` - Creates optimized production build
- **Testing**: `npm test` - Runs Jest test suite

### Runtime Dependencies
- **Node.js**: Version 16+ required for development
- **Modern Browser**: ES6+ support required
- **API Gateway**: Must be accessible on configured endpoint

### Port Configuration
- **Development Server**: Port 3000
- **API Gateway**: Port 8080 (configurable)
- **Production**: Serves from any port via static hosting

### Entry Point
- `src/index.js` - Application entry point
- Renders `App` component into `#root` DOM element
- Includes React StrictMode for development checks

### Containerization
No Dockerfile present in current implementation. For containerization:
- Use Node.js base image for build stage
- Use nginx for serving static files
- Configure proxy rules for API Gateway communication

### Performance Optimizations
- Code splitting with React Router
- Component lazy loading for better performance
- Automatic bundle optimization via Create React App
- HLS streaming for efficient video delivery 