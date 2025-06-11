package com.tskrypko.authentication.security;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import com.tskrypko.authentication.config.JwkConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;

@ExtendWith(MockitoExtension.class)
class JwkConfigTest {

    @Mock
    private static UserDetailsService userDetailsService;

    private static JwkConfig jwkConfig;

    @BeforeAll
    static void setUp() {
        jwkConfig = new JwkConfig(userDetailsService);
        jwkConfig.jwkSource();
    }

    @Test
    void jwkSourceShouldNotBeNull() {
        JWKSource<SecurityContext> jwkSource = jwkConfig.jwkSource();
        assertNotNull(jwkSource);
    }

    @Test
    void jwtDecoderShouldNotBeNull() {
        JwtDecoder jwtDecoder = jwkConfig.jwtDecoder();
        assertNotNull(jwtDecoder);
    }

    @Test
    void tokenGeneratorShouldNotBeNull() {
        OAuth2TokenGenerator<?> tokenGenerator = jwkConfig.tokenGenerator();
        assertNotNull(tokenGenerator);
    }
}
