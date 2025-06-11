package com.tskrypko.authentication.security.token;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tskrypko.authentication.config.security.token.CookieOAuth2TokenResponseHandler;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.StringWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationToken;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class CookieOAuth2TokenResponseHandlerTest {

    private CookieOAuth2TokenResponseHandler handler;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private Environment environment;
    private PrintWriter writer;
    private Authentication clientPrincipal;
    private RegisteredClient registeredClient;

    @BeforeEach
    void setUp() throws IOException {
        environment = mock(Environment.class);
        handler = new CookieOAuth2TokenResponseHandler(environment);
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        writer = mock(PrintWriter.class);
        clientPrincipal = mock(Authentication.class);
        registeredClient = mock(RegisteredClient.class);

        when(response.getWriter()).thenReturn(writer);
    }

    @Test
    void onAuthenticationSuccessShouldAddRefreshTokenCookie() throws IOException {
        // Given
        Instant now = Instant.now();
        Instant expiresAt = now.plus(7, ChronoUnit.DAYS);

        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "access-token-value",
                now,
                now.plus(5, ChronoUnit.MINUTES));

        OAuth2RefreshToken refreshToken = new OAuth2RefreshToken(
                "refresh-token-value",
                now,
                expiresAt);

        Map<String, Object> additionalParams = new HashMap<>();
        additionalParams.put("id_token", "id-token-value");

        OAuth2AccessTokenAuthenticationToken authentication = new OAuth2AccessTokenAuthenticationToken(
                registeredClient,
                clientPrincipal,
                accessToken,
                refreshToken,
                additionalParams);

        when(environment.matchesProfiles("deployment")).thenReturn(false);

        StringWriter stringWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(stringWriter));

        // When
        handler.onAuthenticationSuccess(request, response, authentication);

        // Then
        verify(response).setContentType(MediaType.APPLICATION_JSON_VALUE);
        verify(response).addCookie(any(Cookie.class));

        String actualJson = stringWriter.toString();

        ObjectMapper objectMapper = new ObjectMapper();
        String expectedJson = objectMapper.writeValueAsString(Map.of(
                "access_token", "access-token-value",
                "token_type", "Bearer",
                "expires_in", 299,
                "id_token", "id-token-value"
        ));

        assertEquals(objectMapper.readTree(expectedJson), objectMapper.readTree(actualJson));
    }



    @Test
    void cookieShouldBeSecureInDeploymentEnvironment() throws IOException {
        // Given
        Instant now = Instant.now();
        Instant expiresAt = now.plus(7, ChronoUnit.DAYS);

        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "access-token-value",
                now,
                now.plus(5, ChronoUnit.MINUTES));

        OAuth2RefreshToken refreshToken = new OAuth2RefreshToken(
                "refresh-token-value",
                now,
                expiresAt);

        OAuth2AccessTokenAuthenticationToken authentication = new OAuth2AccessTokenAuthenticationToken(
                registeredClient,
                clientPrincipal,
                accessToken,
                refreshToken);

        when(environment.matchesProfiles("deployment")).thenReturn(true);

        // When
        handler.onAuthenticationSuccess(request, response, authentication);

        // Then
        verify(response).addCookie(argThat(cookie ->
                cookie.getName().equals("refresh_token") &&
                        cookie.getValue().equals("refresh-token-value") &&
                        cookie.isHttpOnly() &&
                        cookie.getSecure()
        ));
    }
}
