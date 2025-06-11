package com.tskrypko.authentication.config.security.client_auth;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

public class PublicClientRefreshProvider implements AuthenticationProvider {

    private final RegisteredClientRepository registeredClientRepository;

    public PublicClientRefreshProvider(RegisteredClientRepository registeredClientRepository) {
        this.registeredClientRepository = registeredClientRepository;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws
            AuthenticationException {
        PublicClientRefreshTokenAuthentication refreshTokenAuth =
                (PublicClientRefreshTokenAuthentication) authentication;

        if (!ClientAuthenticationMethod.NONE.equals(refreshTokenAuth.getClientAuthenticationMethod())) {
            return null;
        }

        String clientId = refreshTokenAuth.getPrincipal().toString();
        RegisteredClient registeredClient = registeredClientRepository.findByClientId(clientId);

        if (registeredClient == null) {
            throw new OAuth2AuthenticationException(new OAuth2Error(
                    OAuth2ErrorCodes.INVALID_CLIENT,
                    "client is not valid",
                    null));
        }

        if (!registeredClient.getClientAuthenticationMethods().contains(
                refreshTokenAuth.getClientAuthenticationMethod())) {
            throw new OAuth2AuthenticationException(new OAuth2Error(
                    OAuth2ErrorCodes.INVALID_CLIENT,
                    "authentication_method is not registered with client",
                    null));
        }

        return new PublicClientRefreshTokenAuthentication(registeredClient);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return PublicClientRefreshTokenAuthentication.class.isAssignableFrom(authentication);
    }
}
