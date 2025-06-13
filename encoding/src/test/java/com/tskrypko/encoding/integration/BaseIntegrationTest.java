package com.tskrypko.encoding.integration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.testcontainers.containers.localstack.LocalStackContainer.Service.S3;

@SpringBootTest
@Testcontainers
public abstract class BaseIntegrationTest {
    private static final Logger logger = LoggerFactory.getLogger(BaseIntegrationTest.class);

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withLogConsumer(outputFrame -> logger.info("PostgreSQL: {}", outputFrame.getUtf8String()));

    @Container
    static RabbitMQContainer rabbitMQ = new RabbitMQContainer("rabbitmq:3-management-alpine")
            .withLogConsumer(outputFrame -> logger.info("RabbitMQ: {}", outputFrame.getUtf8String()));

    @Container
    static LocalStackContainer localstack = new LocalStackContainer(DockerImageName.parse("localstack/localstack:3.0"))
            .withServices(S3)
            .withLogConsumer(outputFrame -> logger.info("LocalStack: {}", outputFrame.getUtf8String()));

    static {
        postgres.start();
        rabbitMQ.start();
        localstack.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        logger.info("Configuring PostgreSQL properties: url={}, username={}", postgres.getJdbcUrl(), postgres.getUsername());
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        logger.info("Configuring RabbitMQ properties: host={}, port={}", rabbitMQ.getHost(), rabbitMQ.getAmqpPort());
        registry.add("spring.rabbitmq.host", rabbitMQ::getHost);
        registry.add("spring.rabbitmq.port", rabbitMQ::getAmqpPort);
        registry.add("spring.rabbitmq.username", rabbitMQ::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbitMQ::getAdminPassword);

        logger.info("Configuring LocalStack properties: endpoint={}", localstack.getEndpointOverride(S3));
        registry.add("aws.endpoint", () -> localstack.getEndpointOverride(S3).toString());
        registry.add("aws.region", () -> localstack.getRegion());
        registry.add("aws.access.key", () -> localstack.getAccessKey());
        registry.add("aws.secret.key", () -> localstack.getSecretKey());
    }
}
