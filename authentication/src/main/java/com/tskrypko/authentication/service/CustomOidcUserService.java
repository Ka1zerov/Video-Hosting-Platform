package com.tskrypko.authentication.service;

import com.tskrypko.authentication.model.OidcUserAttributes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOidcUserService extends OidcUserService {

    private final OidcUserAttributesExtractor attributesExtractor;
    private final OidcUserProcessorService userProcessorService;
    private final TransactionTemplate transactionTemplate;

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);
        OidcUserAttributes attributes = attributesExtractor.extractUserAttributes(oidcUser, userRequest);

        transactionTemplate.execute(status -> userProcessorService.processUser(attributes));

        return oidcUser;
    }
}
