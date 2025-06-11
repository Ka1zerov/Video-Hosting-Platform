package com.tskrypko.authentication.config;

import com.tskrypko.authentication.service.CurrentUserService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

/**
 * Configuration class for JPA auditing that provides user tracking functionality.
 * Implements custom auditor awareness to handle both authenticated users and system operations.
 */
@Configuration
@RequiredArgsConstructor
public class AuditorConfig {

    /**
     * Creates an AuditorAwareImpl bean for JPA auditing.
     *
     * @param currentUserService service to retrieve current user information
     * @return AuditorAwareImpl instance for auditing
     */
    @Bean
    public AuditorAwareImpl auditorProvider(CurrentUserService currentUserService) {
        return new AuditorAwareImpl(currentUserService);
    }

    /**
     * Implementation of AuditorAware interface that provides user IDs for JPA auditing.
     * Handles special cases where there is no user and system user ID is required.
     */
    @RequiredArgsConstructor
    public static class AuditorAwareImpl implements AuditorAware<UUID> {
        private final CurrentUserService currentUserService;
        private static final UUID SYSTEM_USER_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
        private static final List<String> systemAuditorEndpointsPaths = List.of(
                "/api/auth/register",
                "/api/auth/register/verification",
                "/api/auth/password/reset",
                "/api/auth/password/change",
                "/login/oauth2/code/google"
        );

        /**
         * Determines the user ID to be used for auditing.
         * Returns system user ID for endpoints requiring system auditor, otherwise returns current user ID.
         *
         * @return Optional containing either system user ID or current user ID
         */
        @Override
        @NonNull
        public Optional<UUID> getCurrentAuditor() {
            if (isEndpointRequiringSystemAuditor()) {
                return Optional.of(SYSTEM_USER_UUID);
            }
            return Optional.of(currentUserService.getCurrentUserId());
        }


        /**
         * Checks if the current request requires system auditor.
         *
         * @return true if current request is contained in systemAuditorEndpointsPath list, false otherwise
         */
        private boolean isEndpointRequiringSystemAuditor() {
            RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
            if (requestAttributes instanceof ServletRequestAttributes) {
                String requestUri = ((ServletRequestAttributes) requestAttributes).getRequest().getRequestURI();
                return systemAuditorEndpointsPaths.contains(requestUri);
            }
            return false;
        }
    }
}
