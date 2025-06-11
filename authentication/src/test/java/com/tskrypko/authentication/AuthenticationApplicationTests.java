package com.tskrypko.authentication;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest("spring.liquibase.enabled=false")
@ActiveProfiles("test")
class AuthenticationApplicationTests {

    @Test
    void contextLoads() {
    }

}
