# Authentication Microservice Technical Documentation

## 1. Overview

### Microservice Name
**auth-service** - Authentication and Authorization microservice

### Purpose and Responsibilities
The authentication microservice serves as the central identity provider for the distributed video hosting platform. It handles:
- User registration and authentication
- OAuth2 authorization server implementation
- JWT token generation and validation
- Google OAuth2/OIDC integration
- Role-based access control (RBAC)
- Session management and token refresh

### Key Technologies and Frameworks
- **Java 21** - Primary programming language
- **Spring Boot 3.4.4** - Application framework
- **Spring Security** - Security framework
- **Spring OAuth2 Authorization Server** - OAuth2/OIDC implementation
- **Spring Data JPA** - Data persistence layer
- **PostgreSQL** - Primary database
- **Liquibase** - Database migration management
- **Lombok** - Code generation
- **Thymeleaf** - Template engine for login pages
- **SpringDoc OpenAPI** - API documentation
- **TestContainers** - Integration testing
- **Gradle** - Build automation tool

## 2. API Specification

### REST Endpoints

#### Authentication Controller (`/api/auth`)
```http
POST /api/auth/register
Content-Type: application/json

Request Body: RegistrationRequest
{
    "username": "string",
    "email": "string", 
    "password": "string"
}

Response: 200 OK
"User registered successfully"
```

#### Login Controller
```http
GET /login
Content-Type: text/html

Query Parameters:
- error (optional): Error type for failed authentication

Response: 
- Login page (HTML) if PKCE parameters detected
- Redirect to frontend if direct access
```

#### OAuth2 Authorization Server Endpoints
```http
# Authorization endpoint
GET /oauth2/authorize
Parameters: Standard OAuth2 PKCE flow parameters

# Token endpoint  
POST /oauth2/token
Content-Type: application/x-www-form-urlencoded

# JWK Set endpoint
GET /.well-known/jwks.json
Response: JSON Web Key Set

# Logout endpoint
POST /oauth2/logout
Response: 204 No Content
```

#### Actuator Endpoints
```http
GET /actuator/health
Response: Service health status
```

### Authentication/Authorization Methods
- **OAuth2 PKCE Flow**: For public clients (frontend applications)
- **Google OAuth2/OIDC**: Social login integration
- **JWT Tokens**: Stateless authentication
- **Form-based Authentication**: Traditional username/password login
- **Role-based Access Control**: Permission-based authorization

## 3. Architectural Patterns

### Layered Architecture
The service implements a classic layered architecture:
- **Controller Layer**: REST endpoints and web controllers
- **Service Layer**: Business logic implementation
- **Repository Layer**: Data access abstraction
- **Configuration Layer**: Security and application configuration

### Security Patterns
- **OAuth2 Authorization Server Pattern**: Central identity provider
- **JWT Token Pattern**: Stateless authentication
- **PKCE (Proof Key for Code Exchange)**: Secure public client authentication
- **Multi-Filter Chain Pattern**: Separate security configurations for different endpoints

### Data Access Patterns
- **Repository Pattern**: JPA repositories for data access
- **Entity Auditing**: Automatic creation and modification timestamps
- **UUID Primary Keys**: Distributed system-friendly identifiers

## 4. Communication Protocols

### Internal Communication
- **HTTP/HTTPS**: RESTful API endpoints
- **JWT**: Token-based authentication for inter-service communication

### External Communication
- **OAuth2/OIDC**: Standard authentication protocols
- **HTTP/HTTPS**: Client-server communication
- **Google OAuth2 API**: External identity provider integration

### Protocol Standards
- HTTP/1.1 for REST API communication
- OAuth2 Authorization Code Flow with PKCE
- OpenID Connect for identity information
- JSON Web Tokens (JWT) for token format

## 5. IETF RFC References

- **RFC 6749**: OAuth 2.0 Authorization Framework
- **RFC 7636**: Proof Key for Code Exchange by OAuth Public Clients (PKCE)
- **RFC 7519**: JSON Web Token (JWT)
- **RFC 7517**: JSON Web Key (JWK)
- **RFC 6750**: OAuth 2.0 Bearer Token Usage
- **RFC 7662**: OAuth 2.0 Token Introspection
- **OpenID Connect Core 1.0**: OpenID Connect specification

## 6. Configuration & Environment

### Environment Variables
```yaml
# Database Configuration
SPRING_DATASOURCE_URL: jdbc:postgresql://localhost:5432/auth_db
SPRING_DATASOURCE_USERNAME: app_user
SPRING_DATASOURCE_PASSWORD: app_pass

# Google OAuth2
GOOGLE_CLIENT_ID: ${GOOGLE_CLIENT_ID}
GOOGLE_CLIENT_SECRET: ${GOOGLE_CLIENT_SECRET}

# Application URLs
frontend.base_url: http://localhost:3000
app.base_url: http://localhost:8081
auth.issuer-uri: http://localhost:8081
```

### Configuration Files
- `application.yaml`: Main application configuration
- `docker_postgres_service_setup.yml`: Database setup for development
- Liquibase changelog files for database schema management

### Dependencies
- PostgreSQL database server
- Java 21 runtime environment
- Network connectivity to Google OAuth2 services

## 7. Observability

### Logging Practices
- **Spring Boot Logging**: Default logging configuration
- **JPA Query Logging**: Enabled via `show-sql: true`
- **Security Event Logging**: Custom login failure handling
- **Structured Logging**: Using standard Spring Boot logging patterns

### Monitoring Integration
- **Spring Boot Actuator**: Health check endpoints
- **Endpoint Exposure**: Health and refresh endpoints enabled
- **Database Connection Monitoring**: Via Actuator health indicators

### Health Checks
```http
GET /actuator/health
Response: Application and database health status
```

## 8. Security

### Security Mechanisms
- **BCrypt Password Encoding**: Secure password hashing
- **PKCE Flow**: Protection against authorization code interception
- **JWT Token Security**: Signed tokens with configurable expiration
- **CORS Configuration**: Cross-origin request protection
- **CSRF Protection**: Enabled for form-based endpoints
- **Session Management**: Stateless for API endpoints, stateful for web UI

### Token Configuration
```java
TokenSettings:
- Access Token TTL: 5 minutes
- Refresh Token TTL: 7 days
- Require Proof Key: true (PKCE)
```

### Security Best Practices
- Separation of concerns with multiple security filter chains
- Public client authentication without client secrets
- Secure cookie handling for tokens
- Proper logout with cookie clearing

### Notable Security Considerations
- Sensitive configuration should be moved to environment variables
- HTTPS enforcement disabled for local development only
- Token refresh mechanism with secure cookie storage

## 9. AI/ML Usage

**Not Applicable** - This microservice does not implement AI/ML functionality. It focuses on authentication and authorization services.

## 10. Deployment & Runtime

### Containerization
The service is designed for containerized deployment, though no explicit Dockerfile was found in the analyzed code. Typical containerization would include:

```dockerfile
# Example Dockerfile structure
FROM openjdk:21-jre-slim
COPY build/libs/authentication-1.0.0.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

### Runtime Configuration
- **Port**: 8081 (configurable via `server.port`)
- **JVM**: Java 21 required
- **Memory**: Recommended minimum 512MB heap
- **Environment**: Supports development, test, and production profiles

### Dependencies
- PostgreSQL database (port 5432)
- Network access to Google OAuth2 services
- Frontend application (configured via `frontend.base_url`)

### Build and Deployment
```bash
# Build
./gradlew build

# Run tests
./gradlew test

# Run integration tests  
./gradlew integrationTest

# Create distribution
./gradlew bootJar
```

### Database Setup
- Liquibase manages database schema migrations
- Database initialization scripts included
- Supports PostgreSQL with connection pooling via HikariCP

---

## Architecture Diagram

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Frontend      │    │  Auth Service   │    │   PostgreSQL    │
│   (Port 3000)   │◄──►│   (Port 8081)   │◄──►│   Database      │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                              │
                              ▼
                       ┌─────────────────┐
                       │  Google OAuth2  │
                       │    Services     │
                       └─────────────────┘
```

## Security Flow Diagram

```
Client → Authorization Request → Auth Server
   ↑                                ↓
   ├─────── Authorization Code ──────┤
   │                                │
   ▼                                ▼
Token Request → Token Exchange → JWT Token
   ↑                                ↓
   ├────── Access/Refresh Token ─────┤
```

This documentation provides a comprehensive overview of the authentication microservice architecture, capabilities, and operational requirements within the distributed video hosting platform. 