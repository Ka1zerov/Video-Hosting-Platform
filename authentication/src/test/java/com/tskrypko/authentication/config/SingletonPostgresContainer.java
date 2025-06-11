package com.tskrypko.authentication.config;

import org.testcontainers.containers.PostgreSQLContainer;

public class SingletonPostgresContainer {

    public static final PostgreSQLContainer<?> POSTGRES_CONTAINER;

    static {
        POSTGRES_CONTAINER = new PostgreSQLContainer<>("postgres:14-alpine")
                .withDatabaseName("test")
                .withUsername("test")
                .withPassword("test");

        POSTGRES_CONTAINER.start();
    }
}
