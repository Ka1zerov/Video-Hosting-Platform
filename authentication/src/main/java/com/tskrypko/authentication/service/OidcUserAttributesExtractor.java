package com.tskrypko.authentication.service;

import com.tskrypko.authentication.config.OidcDefaults;
import com.tskrypko.authentication.model.OidcUserAttributes;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OidcUserAttributesExtractor {
    private final OidcDefaults oidcDefaults;

    public OidcUserAttributes extractUserAttributes(OidcUser oidcUser, OidcUserRequest userRequest) {
        String email = oidcUser.getEmail();
        if (email == null) {
            throw new OAuth2AuthenticationException("Email not provided by OIDC provider");
        }

        String name = oidcUser.getGivenName();
        if (name == null || name.trim().isEmpty()) {
            name = oidcDefaults.getDefaultName();
        }

        String provider = userRequest.getClientRegistration().getRegistrationId();
        String providerId = oidcUser.getSubject();

        return new OidcUserAttributes(email, name, provider, providerId);
    }
}
