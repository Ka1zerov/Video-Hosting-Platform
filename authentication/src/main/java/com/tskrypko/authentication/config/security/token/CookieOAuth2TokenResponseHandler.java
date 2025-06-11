package com.tskrypko.authentication.config.security.token;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CookieOAuth2TokenResponseHandler implements AuthenticationSuccessHandler {
    private static final String REFRESH_TOKEN_COOKIE_NAME = "refresh_token";
    private static final boolean HTTP_ONLY_COOKIE = true;
    private static final String COOKIE_PATH = "/";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Environment environment;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        OAuth2AccessTokenAuthenticationToken tokenAuth =
                (OAuth2AccessTokenAuthenticationToken) authentication;

        if (tokenAuth.getRefreshToken() != null) {
            addRefreshTokenCookie(
                    response,
                    tokenAuth.getRefreshToken().getTokenValue(),
                    tokenAuth.getRefreshToken().getExpiresAt()
            );
        }

        writeTokenResponse(response, tokenAuth);
    }

    private void addRefreshTokenCookie(HttpServletResponse response, String tokenValue, Instant expiresAt) {
        Cookie refreshTokenCookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, tokenValue);
        refreshTokenCookie.setHttpOnly(HTTP_ONLY_COOKIE);
        // https is enabled only in production
        refreshTokenCookie.setSecure(isDeployment());
        refreshTokenCookie.setPath(COOKIE_PATH);
        refreshTokenCookie.setMaxAge((int) Duration.between(Instant.now(), expiresAt).getSeconds());
        response.addCookie(refreshTokenCookie);
    }

    private void writeTokenResponse(HttpServletResponse response, OAuth2AccessTokenAuthenticationToken tokenAuth) throws IOException {
        Map<String, Object> tokens = new HashMap<>();
        tokens.put("access_token", tokenAuth.getAccessToken().getTokenValue());
        tokens.put("token_type", tokenAuth.getAccessToken().getTokenType().getValue());
        tokens.put("expires_in", Duration.between(Instant.now(), tokenAuth.getAccessToken().getExpiresAt()).getSeconds());

        Optional.ofNullable(tokenAuth.getAdditionalParameters().get("id_token"))
                .ifPresent(idToken -> tokens.put("id_token", idToken));

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), tokens);
    }

    private boolean isDeployment() {
        return environment.matchesProfiles("deployment");
    }
}
