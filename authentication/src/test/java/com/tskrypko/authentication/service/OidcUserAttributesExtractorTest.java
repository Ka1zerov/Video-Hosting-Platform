package com.tskrypko.authentication.service;

import com.tskrypko.authentication.config.OidcDefaults;
import com.tskrypko.authentication.model.OidcUserAttributes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OidcUserAttributesExtractorTest {

    @InjectMocks
    private OidcUserAttributesExtractor extractor;

    @Mock
    private OidcUser oidcUser;

    @Mock
    private OidcUserRequest userRequest;

    @Mock
    private ClientRegistration clientRegistration;

    @Mock
    private OidcDefaults oidcDefaults;

    @Test
    public void extractUserAttributes_shouldExtractCorrectly_whenAllAttributesPresent() {
        // Arrange
        when(oidcUser.getEmail()).thenReturn("test@example.com");
        when(oidcUser.getGivenName()).thenReturn("Test");
        when(oidcUser.getSubject()).thenReturn("user123");
        when(userRequest.getClientRegistration()).thenReturn(clientRegistration);
        when(clientRegistration.getRegistrationId()).thenReturn("google");

        // Act
        OidcUserAttributes attributes = extractor.extractUserAttributes(oidcUser, userRequest);

        // Assert
        assertNotNull(attributes);
        assertEquals("test@example.com", attributes.email());
        assertEquals("Test", attributes.name());
        assertEquals("google", attributes.provider());
        assertEquals("user123", attributes.providerId());
    }

    @Test
    public void extractUserAttributes_shouldHandleNullName() {
        // Arrange
        when(oidcUser.getEmail()).thenReturn("test@example.com");
        when(oidcUser.getGivenName()).thenReturn(null); // Name is null
        when(oidcUser.getSubject()).thenReturn("user123");
        when(userRequest.getClientRegistration()).thenReturn(clientRegistration);
        when(clientRegistration.getRegistrationId()).thenReturn("google");
        when(oidcDefaults.getDefaultName()).thenReturn("GymUser");

        // Act
        OidcUserAttributes attributes = extractor.extractUserAttributes(oidcUser, userRequest);

        // Assert
        assertNotNull(attributes);
        assertEquals("test@example.com", attributes.email());
        assertEquals("GymUser", attributes.name());
        assertEquals("google", attributes.provider());
        assertEquals("user123", attributes.providerId());
    }

    @Test
    public void extractUserAttributes_shouldThrowException_whenEmailIsNull() {
        // Arrange
        when(oidcUser.getEmail()).thenReturn(null);

        // Act & Assert
        assertThrows(OAuth2AuthenticationException.class, () -> extractor.extractUserAttributes(oidcUser, userRequest));
    }
}
