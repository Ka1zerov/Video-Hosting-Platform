package com.tskrypko.upload.config;

import com.tskrypko.upload.service.CurrentUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class JpaAuditConfig {

    @Autowired
    private CurrentUserService currentUserService;

    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> {
            try {
                return Optional.of(currentUserService.getCurrentUserId());
            } catch (Exception e) {
                return Optional.of("system");
            }
        };
    }
} 