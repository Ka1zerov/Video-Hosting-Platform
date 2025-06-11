package com.tskrypko.authentication.security.token;

import com.tskrypko.authentication.config.security.token.CustomOAuth2RefreshTokenGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenContext;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CustomOAuth2RefreshTokenGeneratorTest {

    private CustomOAuth2RefreshTokenGenerator generator;
    private OAuth2TokenContext context;
    private RegisteredClient registeredClient;

    @BeforeEach
    void setUp() {
        generator = new CustomOAuth2RefreshTokenGenerator();
        context = mock(OAuth2TokenContext.class);
        registeredClient = mock(RegisteredClient.class);

        when(context.getRegisteredClient()).thenReturn(registeredClient);

        TokenSettings tokenSettings = TokenSettings.builder()
                .refreshTokenTimeToLive(Duration.ofDays(7))
                .build();
        when(registeredClient.getTokenSettings()).thenReturn(tokenSettings);
    }

    @Test
    void generateShouldReturnNullWhenTokenTypeIsNotRefreshToken() {
        when(context.getTokenType()).thenReturn(OAuth2TokenType.ACCESS_TOKEN);

        OAuth2RefreshToken token = generator.generate(context);

        assertNull(token);
    }

    @Test
    void generateShouldCreateRefreshTokenWithCorrectExpiration() {
        when(context.getTokenType()).thenReturn(OAuth2TokenType.REFRESH_TOKEN);

        OAuth2RefreshToken token = generator.generate(context);

        assertNotNull(token);
        assertNotNull(token.getTokenValue());
        assertNotNull(token.getIssuedAt());
        assertNotNull(token.getExpiresAt());

        // Token should expire in approximately 7 days
        long expiresInSeconds = Duration.between(token.getIssuedAt(), token.getExpiresAt()).getSeconds();
        assertEquals(7 * 24 * 60 * 60, expiresInSeconds, 5);// Allow 5 seconds tolerance for test execution time
    }

    @Test
    void generatedTokensShouldHaveUniqueValues() {
        when(context.getTokenType()).thenReturn(OAuth2TokenType.REFRESH_TOKEN);

        OAuth2RefreshToken token1 = generator.generate(context);
        OAuth2RefreshToken token2 = generator.generate(context);

        assertNotNull(token1);
        assertNotNull(token2);
        assertNotEquals(token1.getTokenValue(), token2.getTokenValue());
    }
}
