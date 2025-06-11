package com.tskrypko.authentication.service;

import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

/**
 * Service for retrieving current user information from JWT token.
 * Integrates with Spring Security to access authentication context.
 */
@Service
public class CurrentUserService {

    /**
     * Retrieves the current user's ID from JWT token.
     *
     * @return UUID of the authenticated user
     * @throws IllegalStateException if user is not authenticated or JWT token is invalid
     */
    public UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            return UUID.fromString(jwt.getClaimAsString("userId"));
        }
        throw new IllegalStateException("User is not authenticated");
    }
}
