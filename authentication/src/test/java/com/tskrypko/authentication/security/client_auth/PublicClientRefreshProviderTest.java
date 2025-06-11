package com.tskrypko.authentication.security.client_auth;

import com.tskrypko.authentication.config.security.client_auth.PublicClientRefreshProvider;
import com.tskrypko.authentication.config.security.client_auth.PublicClientRefreshTokenAuthentication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PublicClientRefreshProviderTest {

    private PublicClientRefreshProvider provider;
    private RegisteredClientRepository clientRepository;

    @BeforeEach
    void setUp() {
        clientRepository = mock(RegisteredClientRepository.class);
        provider = new PublicClientRefreshProvider(clientRepository);
    }

    @Test
    void authenticateShouldReturnNullWhenAuthMethodIsNotNone() {
        PublicClientRefreshTokenAuthentication authentication = mock(
                PublicClientRefreshTokenAuthentication.class);
        when(authentication.getClientAuthenticationMethod()).thenReturn(ClientAuthenticationMethod.CLIENT_SECRET_BASIC);

        Authentication result = provider.authenticate(authentication);

        assertNull(result);
    }

    @Test
    void authenticateShouldThrowExceptionWhenClientNotFound() {
        String clientId = "non-existent-client";
        PublicClientRefreshTokenAuthentication authentication = new PublicClientRefreshTokenAuthentication(clientId);

        when(clientRepository.findByClientId(clientId)).thenReturn(null);

        assertThrows(OAuth2AuthenticationException.class, () -> {
            provider.authenticate(authentication);
        });
    }

    @Test
    void authenticateShouldThrowExceptionWhenAuthMethodNotRegisteredWithClient() {
        String clientId = "test-client";
        PublicClientRefreshTokenAuthentication authentication = new PublicClientRefreshTokenAuthentication(clientId);

        RegisteredClient client = mock(RegisteredClient.class);
        when(clientRepository.findByClientId(clientId)).thenReturn(client);
        when(client.getClientAuthenticationMethods()).thenReturn(Set.of(ClientAuthenticationMethod.CLIENT_SECRET_BASIC));

        assertThrows(OAuth2AuthenticationException.class, () -> {
            provider.authenticate(authentication);
        });
    }

    @Test
    void authenticateShouldReturnAuthenticationWhenValid() {
        String clientId = "valid-client";
        PublicClientRefreshTokenAuthentication authentication = new PublicClientRefreshTokenAuthentication(clientId);

        RegisteredClient client = mock(RegisteredClient.class);
        when(clientRepository.findByClientId(clientId)).thenReturn(client);
        when(client.getClientAuthenticationMethods()).thenReturn(Set.of(ClientAuthenticationMethod.NONE));

        Authentication result = provider.authenticate(authentication);

        assertNotNull(result);
        assertInstanceOf(PublicClientRefreshTokenAuthentication.class, result);
    }

    @Test
    void supportsShouldReturnTrueForPublicClientRefreshTokenAuthentication() {
        assertTrue(provider.supports(PublicClientRefreshTokenAuthentication.class));
    }

    @Test
    void supportsShouldReturnFalseForOtherAuthenticationTypes() {
        assertFalse(provider.supports(Authentication.class));
    }
}
