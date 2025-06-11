package com.tskrypko.authentication.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import com.tskrypko.authentication.config.security.token.CustomOAuth2RefreshTokenGenerator;
import com.tskrypko.authentication.dto.UserDetailsDto;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.token.DelegatingOAuth2TokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.JwtGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;

@Configuration
@RequiredArgsConstructor
public class JwkConfig {
    private final UserDetailsService userDetailsService;

    @Bean
    public JWKSource<SecurityContext> jwkSource() {
        KeyPair keyPair = generateRSAKeys();
        RSAKey rsaKey = new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                .privateKey((RSAPrivateKey) keyPair.getPrivate())
                .keyID(UUID.randomUUID().toString())
                .build();

        JWKSet jwkSet = new JWKSet(rsaKey);
        return new ImmutableJWKSet<>(jwkSet);
    }

    @Bean
    public OAuth2TokenGenerator<?> tokenGenerator() {
        JwtGenerator jwtGenerator = new JwtGenerator(new NimbusJwtEncoder(jwkSource()));
        jwtGenerator.setJwtCustomizer(tokenCustomizer());

        OAuth2TokenGenerator<OAuth2RefreshToken> refreshTokenGenerator =
                new CustomOAuth2RefreshTokenGenerator();

        return new DelegatingOAuth2TokenGenerator(jwtGenerator, refreshTokenGenerator);
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource());
    }

    /**
     * Customizes OAuth2 tokens by adding specific claims based on the token type.
     * <p>
     * - The **ID Token** includes user authorities (roles/permissions) for authorization.
     * - The **Access Token** includes the user's unique ID (`userId` as UUID) for authentication in backend services.
     * <p>
     * This setup ensures:
     * 1. Frontend applications can use the ID Token for authorization.
     * 2. Backend services can extract the user ID from the Access Token.
     *
     * @return an OAuth2TokenCustomizer that modifies JWT claims.
     */
    public OAuth2TokenCustomizer<JwtEncodingContext> tokenCustomizer() {
        return context -> {
            Authentication authentication = context.getPrincipal();
            String tokenType = context.getTokenType().getValue();

            UserDetailsDto userDetails = extractUserDetails(authentication);

            if (userDetails != null) {
                context.getClaims().claim("userId", userDetails.getId().toString());

                if (OidcParameterNames.ID_TOKEN.equals(tokenType)) {
                    Set<String> authorities = userDetails.getAuthorities().stream()
                            .map(GrantedAuthority::getAuthority)
                            .collect(Collectors.toSet());
                    context.getClaims().claim("sub", userDetails.getUsername());
                    context.getClaims().claim("authorities", authorities);
                }
            }
        };
    }

    private UserDetailsDto extractUserDetails(Authentication authentication) {
        if (authentication.getPrincipal() instanceof UserDetailsDto userDetails) {
            return userDetails;
        }
        if (authentication.getPrincipal() instanceof DefaultOidcUser defaultOidcUser) {
            String email = defaultOidcUser.getAttribute("email");
            return (UserDetailsDto) userDetailsService.loadUserByUsername(email);
        }
        return null;
    }

    private static KeyPair generateRSAKeys() {
        KeyPair keyPair;

        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            keyPair = keyPairGenerator.generateKeyPair();
        } catch (Exception exception) {
            throw new RuntimeException("Failed to create RSA keypair!", exception);
        }

        return keyPair;
    }
}
