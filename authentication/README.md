## Authentication Service (OAuth2/OIDC, PKCE, JWT)

This microservice is the platform's dedicated Identity and Access Management (IAM) component. It runs as a standalone service and provides OAuth2 Authorization Server capabilities with PKCE, OIDC user identity, JWT issuance/validation, Google login integration, and basic email/password registration.

### Key Capabilities
- OAuth2 Authorization Server with Authorization Code + PKCE
- OpenID Connect (userinfo via ID Token claims)
- JWT access/refresh tokens, JWKs endpoint
- Google OAuth2/OIDC login
- Basic registration API (email + password)
- Role-based access control (RBAC)
- SpringDoc OpenAPI and Actuator health

### Tech Stack
- Java 21, Spring Boot 3.4.x
- Spring Security, OAuth2 Authorization Server, OAuth2 Resource Server
- Spring Data JPA, PostgreSQL, Liquibase
- Thymeleaf for login page
- SpringDoc OpenAPI

---

## PKCE Flow (High-Level)

PKCE (Proof Key for Code Exchange) augments the OAuth2 Authorization Code flow for public clients (e.g., SPA) to prevent authorization code interception.

```
┌────────────┐                                      ┌──────────────────┐
│  Frontend  │                                      │  Auth Service    │
└─────┬──────┘                                      └─────────┬────────┘
      │ create code_verifier                               │
      │ code_challenge = BASE64URL(SHA256(code_verifier))   │
      │                                                     │
      │ 1) /oauth2/authorize?...&code_challenge=...&        │
      │    code_challenge_method=S256&response_type=code    │
      ├────────────────────────────────────────────────────▶│
      │                                                     │ validate client, session, scopes
      │                                                     │ authenticate user (login page)
      │                                                     │
      │                       2) authorization_code         │
      ◀─────────────────────────────────────────────────────┤
      │                                                     │
      │ 3) POST /oauth2/token with code + code_verifier     │
      ├────────────────────────────────────────────────────▶│ verify code_verifier matches challenge
      │                                                     │ issue access_token (+ optional refresh_token, id_token)
      │                                                     │
      │                   4) access_token (JWT)             │
      ◀─────────────────────────────────────────────────────┤
```

Key endpoints involved:
- Authorization: `GET /oauth2/authorize`
- Token: `POST /oauth2/token`
- JWK Set: `GET /.well-known/jwks.json`

The login page is served at `GET /login`. The controller detects PKCE parameters from a saved authorization request and renders the authentication view when appropriate.

---

## API Overview

### Registration
```
POST /api/auth/register
Content-Type: application/json

{
  "username": "string",
  "email": "string",
  "password": "string"
}

200 OK
"User registered successfully"
```

### OAuth2/OIDC
- `GET /oauth2/authorize` – Authorization Code with PKCE
- `POST /oauth2/token` – Exchange code + code_verifier for tokens
- `GET /.well-known/jwks.json` – JWK Set for JWT signature validation

### Operational
- `GET /actuator/health` – Health status
- `GET /api-docs` and `GET /swagger-ui.html` – OpenAPI/Swagger UI

---

## Current Registration Status

The service is not yet adapted for full self‑service registration flows (profile, email verification, password reset). It currently exposes a simple registration endpoint for email and password (see above). Users can also register/sign‑in via Google OAuth2/OIDC.

---

## Configuration

Environment variables and application settings:
```
# Database
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/auth_db
SPRING_DATASOURCE_USERNAME=app_user
SPRING_DATASOURCE_PASSWORD=app_pass

# External Providers
GOOGLE_CLIENT_ID=...
GOOGLE_CLIENT_SECRET=...

# URLs
frontend.base_url=http://localhost:3000
app.base_url=http://localhost:8081
auth.issuer-uri=http://localhost:8081
```

Main configuration lives in `src/main/resources/application.yaml`. Sensitive values should be provided via `.env` (loaded with `spring-dotenv`).

---

## Run Locally

Prerequisites: Java 21, PostgreSQL running with a database accessible as configured.

```
./gradlew build
./gradlew bootRun
```

Liquibase manages schema migrations on startup.

---

## How This Service Fits In

This is a separate, large-scale IAM project within the platform, acting as a centralized Authorization Server and identity provider for other services (upload, streaming, metadata, frontend). It issues signed JWTs, publishes JWKs for verification, and integrates with external IdPs (Google). Public clients use PKCE to authenticate securely without storing client secrets.

---

## Login Page (Screenshot)

<img width="1728" height="986" alt="image" src="https://github.com/user-attachments/assets/fc9dd80e-2755-4fa5-9b1f-48149a5bc919" />

---

## Security Notes
- BCrypt password hashing for local accounts
- PKCE required for public clients
- Short‑lived access tokens, refresh tokens supported
- CORS/CSRF configured per endpoint type

---

## References
- RFC 6749 (OAuth 2.0), RFC 7636 (PKCE), RFC 7519 (JWT)
- OpenID Connect Core 1.0
