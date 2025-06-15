package com.tskrypko.metadata;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = MetadataApplication.class,
    properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration"
    })
@ActiveProfiles("test")
class MetadataApplicationTests {

    @Test
    void contextLoads() {
        // This test verifies that the Spring application context loads successfully
        // without any configuration errors or missing dependencies
    }

} 