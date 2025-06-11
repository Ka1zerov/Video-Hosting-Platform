package com.tskrypko.authentication.security.token;

import com.tskrypko.authentication.config.security.token.RefreshTokenCookieAuthenticationConverter;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2RefreshTokenAuthenticationToken;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RefreshTokenCookieAuthenticationConverterTest {

    private RefreshTokenCookieAuthenticationConverter converter;
    private HttpServletRequest request;
    private SecurityContext securityContext;
    private Authentication clientPrincipal;

    @BeforeEach
    void setUp() {
        converter = new RefreshTokenCookieAuthenticationConverter();
        request = mock(HttpServletRequest.class);
        securityContext = mock(SecurityContext.class);
        clientPrincipal = mock(Authentication.class);

        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void convertShouldReturnNullWhenGrantTypeIsNotRefreshToken() {
        when(request.getParameter(OAuth2ParameterNames.GRANT_TYPE))
                .thenReturn(AuthorizationGrantType.AUTHORIZATION_CODE.getValue());

        Authentication result = converter.convert(request);

        assertNull(result);
    }

    @Test
    void convertShouldThrowExceptionWhenClientAuthenticationIsMissing() {
        when(request.getParameter(OAuth2ParameterNames.GRANT_TYPE))
                .thenReturn(AuthorizationGrantType.REFRESH_TOKEN.getValue());
        when(securityContext.getAuthentication()).thenReturn(null);

        assertThrows(OAuth2AuthenticationException.class, () -> {
            converter.convert(request);
        });
    }

    @Test
    void convertShouldThrowExceptionWhenRefreshTokenIsMissing() {
        when(request.getParameter(OAuth2ParameterNames.GRANT_TYPE))
                .thenReturn(AuthorizationGrantType.REFRESH_TOKEN.getValue());
        when(securityContext.getAuthentication()).thenReturn(clientPrincipal);
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("other_cookie", "value")});

        assertThrows(OAuth2AuthenticationException.class, () -> {
            converter.convert(request);
        });
    }

    @Test
    void convertShouldCreateTokenWhenRefreshTokenIsPresent() {
        String refreshTokenValue = "valid-refresh-token";
        Cookie refreshTokenCookie = new Cookie("refresh_token", refreshTokenValue);

        when(request.getParameter(OAuth2ParameterNames.GRANT_TYPE))
                .thenReturn(AuthorizationGrantType.REFRESH_TOKEN.getValue());
        when(securityContext.getAuthentication()).thenReturn(clientPrincipal);
        when(request.getCookies()).thenReturn(new Cookie[]{refreshTokenCookie});

        Authentication result = converter.convert(request);

        assertNotNull(result);
        assertInstanceOf(OAuth2RefreshTokenAuthenticationToken.class, result);
        OAuth2RefreshTokenAuthenticationToken token = (OAuth2RefreshTokenAuthenticationToken) result;
        assertEquals(refreshTokenValue, token.getRefreshToken());
        assertEquals(clientPrincipal, token.getPrincipal());
    }

    @Test
    void convertShouldIncludeScopesWhenProvided() {
        String refreshTokenValue = "valid-refresh-token";
        Cookie refreshTokenCookie = new Cookie("refresh_token", refreshTokenValue);
        String scope = "openid profile";

        when(request.getParameter(OAuth2ParameterNames.GRANT_TYPE))
                .thenReturn(AuthorizationGrantType.REFRESH_TOKEN.getValue());
        when(securityContext.getAuthentication()).thenReturn(clientPrincipal);
        when(request.getCookies()).thenReturn(new Cookie[]{refreshTokenCookie});
        when(request.getParameter(OAuth2ParameterNames.SCOPE)).thenReturn(scope);

        Authentication result = converter.convert(request);

        assertNotNull(result);
        assertInstanceOf(OAuth2RefreshTokenAuthenticationToken.class, result);
        OAuth2RefreshTokenAuthenticationToken token = (OAuth2RefreshTokenAuthenticationToken) result;
        assertEquals(2, token.getScopes().size());
        assertTrue(token.getScopes().contains("openid"));
        assertTrue(token.getScopes().contains("profile"));
    }
}
