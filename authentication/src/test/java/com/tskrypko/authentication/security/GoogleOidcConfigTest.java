package com.tskrypko.authentication.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
    "google.client-id=test-client-id",
    "google.client-secret=test-client-secret",
    "app.base_url=http://localhost:8080"
})
public class GoogleOidcConfigTest {

    @Autowired
    private ClientRegistrationRepository clientRegistrationRepository;

    @Test
    public void clientRegistrationRepository_shouldContainGoogleRegistration() {
        // Verify Google client registration exists
        ClientRegistration googleRegistration = clientRegistrationRepository.findByRegistrationId("google");
        assertNotNull(googleRegistration, "Google client registration should exist");
    }

    @Test
    public void googleClientRegistration_shouldHaveCorrectProperties() {
        // Get the Google registration
        ClientRegistration registration = clientRegistrationRepository.findByRegistrationId("google");

        // Verify client properties
        assertEquals("test-client-id", registration.getClientId());
        assertEquals("test-client-secret", registration.getClientSecret());
        assertEquals(ClientAuthenticationMethod.CLIENT_SECRET_BASIC, registration.getClientAuthenticationMethod());
        assertEquals(AuthorizationGrantType.AUTHORIZATION_CODE, registration.getAuthorizationGrantType());

        // Verify endpoints
        assertEquals("https://accounts.google.com/o/oauth2/v2/auth", registration.getProviderDetails().getAuthorizationUri());
        assertEquals("https://oauth2.googleapis.com/token", registration.getProviderDetails().getTokenUri());
        assertEquals("https://openidconnect.googleapis.com/v1/userinfo", registration.getProviderDetails().getUserInfoEndpoint().getUri());
        assertEquals("https://www.googleapis.com/oauth2/v3/certs", registration.getProviderDetails().getJwkSetUri());

        // Verify scopes
        assertTrue(registration.getScopes().contains("openid"));
        assertTrue(registration.getScopes().contains("profile"));
        assertTrue(registration.getScopes().contains("email"));

        // Verify redirect URI
        assertEquals("http://localhost:8080/login/oauth2/code/google", registration.getRedirectUri());

        // Verify user name attribute
        assertEquals(IdTokenClaimNames.SUB, registration.getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName());

        // Verify client name
        assertEquals("Google", registration.getClientName());
    }
}
