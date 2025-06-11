package com.tskrypko.authentication.config.integration;

import com.tskrypko.authentication.service.CurrentUserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import java.util.UUID;

@Configuration
@Profile("test")
public class TestCurrentUserService {

    @Bean
    @Primary
    public CurrentUserService currentUserService() {
        return new CurrentUserService() {
            @Override
            public UUID getCurrentUserId() {
                return UUID.fromString("00000000-0000-0000-0000-000000000001");
            }
        };
    }

}
