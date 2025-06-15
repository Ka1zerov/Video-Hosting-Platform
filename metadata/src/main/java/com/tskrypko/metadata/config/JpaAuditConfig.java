package com.tskrypko.metadata.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;

import java.util.Optional;

@Configuration
@RequiredArgsConstructor
public class JpaAuditConfig {

    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> {
            try {
                // For metadata service, we could implement user context extraction here
                // For now, return service name as auditor
                return Optional.of("metadata-service");
            } catch (Exception e) {
                return Optional.of("system");
            }
        };
    }
} 