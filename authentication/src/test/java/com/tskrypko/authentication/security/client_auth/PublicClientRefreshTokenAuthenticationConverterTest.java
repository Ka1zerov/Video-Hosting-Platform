package com.tskrypko.authentication.security.client_auth;

import com.tskrypko.authentication.config.security.client_auth.PublicClientRefreshTokenAuthentication;
import com.tskrypko.authentication.config.security.client_auth.PublicClientRefreshTokenAuthenticationConverter;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PublicClientRefreshTokenAuthenticationConverterTest {

    private PublicClientRefreshTokenAuthenticationConverter converter;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        converter = new PublicClientRefreshTokenAuthenticationConverter();
        request = mock(HttpServletRequest.class);
    }

    @Test
    void convertShouldReturnNullWhenGrantTypeIsNotRefreshToken() {
        when(request.getParameter(OAuth2ParameterNames.GRANT_TYPE))
                .thenReturn(AuthorizationGrantType.AUTHORIZATION_CODE.getValue());

        Authentication result = converter.convert(request);

        assertNull(result);
    }

    @Test
    void convertShouldReturnNullWhenClientIdIsEmpty() {
        when(request.getParameter(OAuth2ParameterNames.GRANT_TYPE))
                .thenReturn(AuthorizationGrantType.REFRESH_TOKEN.getValue());
        when(request.getParameter(OAuth2ParameterNames.CLIENT_ID))
                .thenReturn("");

        Authentication result = converter.convert(request);

        assertNull(result);
    }

    @Test
    void convertShouldReturnAuthenticationWhenValid() {
        String clientId = "test-client";

        when(request.getParameter(OAuth2ParameterNames.GRANT_TYPE))
                .thenReturn(AuthorizationGrantType.REFRESH_TOKEN.getValue());
        when(request.getParameter(OAuth2ParameterNames.CLIENT_ID))
                .thenReturn(clientId);

        Authentication result = converter.convert(request);

        assertNotNull(result);
        assertInstanceOf(PublicClientRefreshTokenAuthentication.class, result);
        assertEquals(clientId, result.getPrincipal());
    }
}
