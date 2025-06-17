# Gateway Microservice Technical Documentation

## 1. Overview

### Microservice Name
**Video Platform Gateway Service**

### Purpose and Responsibilities
The Gateway microservice serves as the single entry point and API gateway for the distributed video hosting platform. It handles request routing, authentication, authorization, CORS configuration, and provides centralized security for all downstream services.

Key responsibilities:
- Request routing to appropriate backend services
- JWT-based authentication and authorization
- Cross-Origin Resource Sharing (CORS) configuration
- Security enforcement and user context propagation
- Load balancing and service discovery

### Key Technologies and Frameworks Used
- **Java 21** - Primary programming language
- **Spring Boot 3.4.5** - Application framework
- **Spring Cloud Gateway 4.2.2** - API Gateway implementation
- **Spring Security** - Security framework
- **Spring WebFlux** - Reactive web framework
- **OAuth2 Resource Server** - JWT authentication
- **Spring Boot Actuator** - Monitoring and management
- **Netty** - Asynchronous event-driven network framework
- **Gradle** - Build automation tool

## 2. API Specification

The Gateway does not expose direct REST endpoints but routes requests to the following backend services:

### Routing Configuration

#### Authentication Service Routes
- **Base URI**: `http://localhost:8081`
- **Paths**: `/oauth2/**`, `/login`, `/logout`, `/api/auth/**`
- **Security**: Public access (no JWT required)

#### Upload Service Routes  
- **Base URI**: `http://localhost:8082`
- **Paths**: `/api/upload/**`
- **Security**: JWT authentication required
- **Filters**: JwtHeaderFilter (adds X-User-Id header)

#### Metadata Service Routes
- **Base URI**: `http://localhost:8083` 
- **Paths**: `/api/metadata/**`
- **Security**: JWT authentication required
- **Filters**: JwtHeaderFilter (adds X-User-Id header)

#### Streaming Service Routes
- **Base URI**: `http://localhost:8084`
- **Paths**: `/api/streaming/**` 
- **Security**: JWT authentication required
- **Filters**: JwtHeaderFilter (adds X-User-Id header)

#### Encoding Service Routes
- **Base URI**: `http://localhost:8085`
- **Paths**: `/api/encoding/**`
- **Security**: JWT authentication required  
- **Filters**: JwtHeaderFilter (adds X-User-Id header)

### HTTP Methods Supported
- GET, POST, PUT, DELETE, OPTIONS, PATCH, HEAD

### Authentication/Authorization
- **Type**: OAuth2 JWT Bearer Token
- **JWK Set URI**: `http://localhost:8081/.well-known/jwks.json`
- **Issuer URI**: `http://localhost:8081`
- **User Context**: JWT userId claim extracted and passed as X-User-Id header

## 3. Architectural Patterns

### Gateway Pattern
Implements the API Gateway architectural pattern, providing:
- Single entry point for all client requests
- Request/response transformation
- Protocol translation
- Service aggregation capabilities

### Filter Chain Pattern
Uses Spring Cloud Gateway's filter chain pattern for:
- Request/response processing
- Authentication/authorization
- Header manipulation
- Cross-cutting concerns

### Reactive Architecture
Built on Spring WebFlux reactive stack:
- Non-blocking I/O operations
- Asynchronous request processing
- Backpressure handling
- Scalable concurrent request handling

## 4. Communication Protocols

### External Communication
- **Protocol**: HTTP/HTTPS
- **Port**: 8080
- **Format**: JSON over REST
- **Security**: JWT Bearer tokens

### Internal Communication  
- **Protocol**: HTTP (service-to-service)
- **Load Balancing**: Round-robin (default)
- **Service Discovery**: Static configuration (localhost-based)
- **Header Propagation**: X-User-Id header injection

## 5. IETF RFC References

- **RFC 7519** - JSON Web Token (JWT) standard
- **RFC 6749** - OAuth 2.0 Authorization Framework  
- **RFC 7617** - The 'Basic' HTTP Authentication Scheme
- **RFC 6454** - The Web Origin Concept (CORS)
- **RFC 2616** - HTTP/1.1 specification
- **RFC 7540** - HTTP/2 specification (Netty support)

## 6. Configuration & Environment

### Key Configuration Files
- `application.yaml` - Main configuration file
- `build.gradle` - Build and dependency configuration

### Important Configuration Properties

#### Server Configuration
```yaml
server:
  port: 8080
```

#### JWT Configuration
```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          jwk-set-uri: http://localhost:8081/.well-known/jwks.json
          issuer-uri: http://localhost:8081
```

#### Service Discovery
Static configuration with hardcoded service URIs:
- Auth Service: `localhost:8081`
- Upload Service: `localhost:8082`  
- Metadata Service: `localhost:8083`
- Streaming Service: `localhost:8084`
- Encoding Service: `localhost:8085`

### Dependencies
Key runtime dependencies include:
- Spring Boot WebFlux starter
- Spring Security
- Spring Cloud Gateway
- OAuth2 Resource Server
- Actuator for monitoring
- Netty DNS resolver (macOS compatibility)

## 7. Observability

### Logging
- **Framework**: SLF4J with Logback
- **Log Level**: DEBUG for development
- **Log Pattern**: Timestamp, thread, level, logger, message
- **Key Loggers**:
  - `com.tskrypko.gateway`: Application logs
  - `org.springframework.security`: Security events
  - `org.springframework.cloud.gateway`: Gateway routing
  - `org.springframework.web.cors`: CORS handling

### Monitoring
- **Spring Boot Actuator** endpoints enabled:
  - `/actuator/health` - Health check endpoint
  - `/actuator/refresh` - Configuration refresh
  - `/actuator/gateway` - Gateway route information

### Metrics
- Built-in Spring Boot metrics via Actuator
- Gateway-specific metrics for route performance
- Security metrics for authentication events

## 8. Security

### Security Mechanisms
1. **JWT Authentication**: OAuth2 resource server validates JWT tokens
2. **CORS Protection**: Configured for localhost development origins  
3. **CSRF Protection**: Disabled for stateless API architecture
4. **Path-based Authorization**: Public paths vs authenticated paths
5. **Header Injection**: User ID propagation to downstream services

### Security Best Practices
- Stateless authentication using JWTs
- Principle of least privilege (path-based access)
- Secure header propagation
- CORS whitelist approach

### Security Risks
- **Development Configuration**: Hardcoded localhost URIs
- **CORS Policy**: Permissive for development (allows credentials)
- **Service Communication**: Unencrypted HTTP between services
- **No Rate Limiting**: No built-in DDoS protection

## 9. AI/ML Usage

**Not applicable** - This microservice does not implement any AI/ML functionality. It serves as a routing and security gateway without machine learning capabilities.

## 10. Deployment & Runtime

### Build System
- **Tool**: Gradle 7.x+
- **Java Version**: OpenJDK 21
- **Build Command**: `./gradlew build`
- **Test Command**: `./gradlew test`

### Runtime Requirements
- **JVM**: Java 21+ compatible runtime
- **Memory**: Minimum 512MB heap (reactive architecture)
- **Port**: 8080 (configurable)
- **Network**: Access to all downstream services

### Entry Point
- **Main Class**: `com.tskrypko.gateway.GatewayApplication`
- **Spring Boot Application**: Auto-configuration enabled
- **Startup**: Standard Spring Boot jar execution

### Container Information
No Dockerfile found in the current codebase. The application can be containerized using standard Spring Boot practices:

```dockerfile
FROM openjdk:21-jre-slim
COPY build/libs/gateway-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

### Health Check
- **Endpoint**: `/actuator/health`
- **Response**: JSON health status
- **Dependencies**: Downstream service availability

### Environment Variables
Configuration can be externalized using standard Spring Boot environment variable naming:
- `SERVER_PORT`: Server port (default: 8080)
- `SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWKSETURI`: JWT validation endpoint
- `LOGGING_LEVEL_COM_TSKRYPKO_GATEWAY`: Application log level

---

*Documentation generated for Gateway microservice - part of Video Hosting Platform*
*Last updated: Generated from source code analysis* 