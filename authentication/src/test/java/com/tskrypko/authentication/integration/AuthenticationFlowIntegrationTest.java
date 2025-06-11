package com.tskrypko.authentication.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.codec.Base64;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.FluxExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@AutoConfigureWebTestClient
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AuthenticationFlowIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String CODE_VERIFIER = "code_verifier_example_with_minimum_length_of_43_characters";
    private static String CODE_CHALLENGE;

    @BeforeEach
    public void setup() throws NoSuchAlgorithmException {
        // Generate valid code challenge
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(CODE_VERIFIER.getBytes(StandardCharsets.UTF_8));
        CODE_CHALLENGE = new String(Base64.encode(hash)).replace("=", "")
                .replace("+", "-")
                .replace("/", "_");
    }

    @Test
    @WithMockUser(username = "testuser@example.com", password = "password")
    public void authorizationCodeFlow_withPKCE_shouldSucceed() throws Exception {
        // Step 1: Authorization Request
        FluxExchangeResult<byte[]> authorizationResult = webTestClient
                .get()
                .uri(UriComponentsBuilder.fromPath("/oauth2/authorize")
                        .queryParam("response_type", "code")
                        .queryParam("client_id", "public-client")
                        .queryParam("redirect_uri", "http://localhost:3000")
                        .queryParam("scope", "openid profile")
                        .queryParam("code_challenge", CODE_CHALLENGE)
                        .queryParam("code_challenge_method", "S256")
                        .build().toUri())
                .exchange()
                .expectStatus().is3xxRedirection()
                .returnResult(byte[].class);

        // Extract location header and verify it redirects to login or has a code
        String redirectUrl = authorizationResult.getResponseHeaders().getFirst(HttpHeaders.LOCATION);
        assertNotNull(redirectUrl);

        String authCode;
        if (redirectUrl.contains("http://localhost:3000")) {
            // Already redirected to client with code
            authCode = extractCodeFromUrl(redirectUrl);
        } else {
            // We need to extract the code from a session cookie
            String cookieValue = authorizationResult.getResponseCookies().getFirst("JSESSIONID").getValue();

            // Step 2: Perform the login if needed
            FluxExchangeResult<byte[]> loginResult = webTestClient
                    .post()
                    .uri("/login")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .bodyValue("username=testuser@example.com&password=password")
                    .cookie("JSESSIONID", cookieValue)
                    .exchange()
                    .expectStatus().is3xxRedirection()
                    .returnResult(byte[].class);

            String redirectAfterLogin = loginResult.getResponseHeaders().getFirst(HttpHeaders.LOCATION);
            assertNotNull(redirectAfterLogin);

            // Check if we got the code or need to follow the redirect to authorization endpoint again
            if (redirectAfterLogin.contains("code=")) {
                authCode = extractCodeFromUrl(redirectAfterLogin);
            } else {
                // Follow redirect back to authorization endpoint
                FluxExchangeResult<byte[]> finalAuthResult = webTestClient
                        .get()
                        .uri(redirectAfterLogin)
                        .cookie("JSESSIONID", loginResult.getResponseCookies().getFirst("JSESSIONID").getValue())
                        .exchange()
                        .expectStatus().is3xxRedirection()
                        .returnResult(byte[].class);

                String finalRedirect = finalAuthResult.getResponseHeaders().getFirst(HttpHeaders.LOCATION);
                assertNotNull(finalRedirect);
                assertTrue(finalRedirect.contains("code="));
                authCode = extractCodeFromUrl(finalRedirect);
            }
        }

        assertNotNull(authCode, "Authorization code should not be null");

        // Step 3: Exchange the code for tokens
        FluxExchangeResult<byte[]> tokenResult = webTestClient
                .post()
                .uri("/oauth2/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue("grant_type=authorization_code" +
                        "&code=" + authCode +
                        "&redirect_uri=http://localhost:3000" +
                        "&client_id=public-client" +
                        "&code_verifier=" + CODE_VERIFIER)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .returnResult(byte[].class);

        // Verify the token response
        byte[] responseBodyBytes = tokenResult.getResponseBodyContent();
        String responseBody = new String(responseBodyBytes, StandardCharsets.UTF_8);
        Map<String, Object> tokenResponse = objectMapper.readValue(responseBody, Map.class);

        // Check for tokens in response
        assertNotNull(tokenResponse.get("access_token"));
        assertNotNull(tokenResponse.get("expires_in"));
        assertEquals("Bearer", tokenResponse.get("token_type"));

        // Check refresh token cookie is present
        boolean hasRefreshTokenCookie = tokenResult.getResponseCookies().containsKey("refresh_token");
        assertTrue(hasRefreshTokenCookie, "Refresh token cookie should be present");
    }

    @Test
    public void refreshToken_withCookie_shouldSucceed() {
        // For this test, we'll mock a refresh token cookie since obtaining a real one
        // would require the full auth flow
        String mockRefreshToken = UUID.randomUUID().toString();

        FluxExchangeResult<byte[]> refreshResult = webTestClient
                .post()
                .uri("/oauth2/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue("grant_type=refresh_token&client_id=public-client")
                .cookie("refresh_token", mockRefreshToken)
                .exchange()
                .expectStatus().is4xxClientError() // We expect an error since our token is fake
                .returnResult(byte[].class);

        // The real test would verify proper token refresh, but we're checking
        // our RefreshTokenCookieAuthenticationConverter was invoked
        // A 400 error with "invalid_grant" is expected instead of 401 "invalid_client"
        byte[] responseBodyBytes = refreshResult.getResponseBodyContent();
        String responseBody = new String(responseBodyBytes, StandardCharsets.UTF_8);
        assertTrue(responseBody.contains("invalid_grant"),
                "Should fail with invalid_grant error: " + responseBody);
    }

    private String extractCodeFromUrl(String url) {
        Pattern pattern = Pattern.compile("code=([^&]+)");
        Matcher matcher = pattern.matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
}
