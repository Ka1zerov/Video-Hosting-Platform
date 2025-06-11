package com.tskrypko.authentication.config.security.token;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2RefreshTokenAuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.util.StringUtils;

/**
 * Custom Authentication Converter to extract refresh_token from cookies.
 */
public final class RefreshTokenCookieAuthenticationConverter implements AuthenticationConverter {
    private static final String REFRESH_TOKEN_COOKIE_NAME = "refresh_token";

    @Nullable
    @Override
    public Authentication convert(HttpServletRequest request) {
        String grantType = request.getParameter(OAuth2ParameterNames.GRANT_TYPE);
        if (!AuthorizationGrantType.REFRESH_TOKEN.getValue().equals(grantType)) {
            return null;
        }

        Authentication clientPrincipal = SecurityContextHolder.getContext().getAuthentication();
        if (clientPrincipal == null) {
            throw new OAuth2AuthenticationException(new OAuth2Error(
                    "invalid_client", "Client authentication is missing", null
            ));
        }

        String refreshToken = extractRefreshTokenFromCookies(request);
        if (!StringUtils.hasText(refreshToken)) {
            throw new OAuth2AuthenticationException(new OAuth2Error(
                    "invalid_grant", "Refresh token is expired or missing", null
            ));
        }

        String scope = request.getParameter(OAuth2ParameterNames.SCOPE);
        Set<String> requestedScopes = new HashSet<>();
        if (StringUtils.hasText(scope)) {
            requestedScopes.addAll(Arrays.asList(scope.split(" ")));
        }

        Map<String, Object> additionalParameters = new HashMap<>();

        return new OAuth2RefreshTokenAuthenticationToken(refreshToken, clientPrincipal, requestedScopes, additionalParameters);
    }


    private String extractRefreshTokenFromCookies(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }
        return Stream.of(request.getCookies())
                .filter(cookie -> REFRESH_TOKEN_COOKIE_NAME.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }
}
