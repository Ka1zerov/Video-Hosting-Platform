package com.tskrypko.authentication.config.integration;

import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.AuditorAware;
import java.util.UUID;

import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Primary
public class TestAuditorAware implements AuditorAware<UUID> {

    private static final UUID TEST_SCOPED_UUID =
            UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Override
    public Optional<UUID> getCurrentAuditor() { return Optional.of(TEST_SCOPED_UUID);}
}
