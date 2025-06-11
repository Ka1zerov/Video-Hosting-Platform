package com.tskrypko.authentication.config.integration;

import com.tskrypko.authentication.config.JwkConfig;
import com.tskrypko.authentication.config.SingletonPostgresContainer;
import org.junit.jupiter.api.*;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.junit.jupiter.Testcontainers;
import java.time.Instant;

@Testcontainers
@ActiveProfiles("test")
@Tag("integration")
@AutoConfigureMockMvc
@AutoConfigureWebTestClient
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class BaseIntegrationTest implements BaseTestTeardownLifecycle {

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    protected WebTestClient webTestClient;

    protected static String jwtToken;
    protected static JwtDecoder jwtDecoder;
    protected static String username = "testUser";
    protected static String userId = "00000000-0000-0000-0000-000000000001";
    protected static String email = "testUser@user.com";

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", SingletonPostgresContainer.POSTGRES_CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", SingletonPostgresContainer.POSTGRES_CONTAINER::getUsername);
        registry.add("spring.datasource.password", SingletonPostgresContainer.POSTGRES_CONTAINER::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);  // Initialize mocks
    }

    @BeforeAll
    public static void setup(@Autowired JwkConfig jwkConfig, @Autowired JdbcTemplate jdbcTemplate) {

    jdbcTemplate.execute("""
                    DO $$ 
                    DECLARE r RECORD;
                    BEGIN 
                        FOR r IN (SELECT schemaname, tablename FROM pg_catalog.pg_tables WHERE schemaname IN ('public'))
                        LOOP 
                            EXECUTE 'TRUNCATE TABLE ' || r.schemaname || '.' || r.tablename || ' CASCADE'; 
                        END LOOP; 
                    END $$;
                """);

        JwtEncoder jwtEncoder = new NimbusJwtEncoder(jwkConfig.jwkSource());
        jwtDecoder = jwkConfig.jwtDecoder();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("self")
                .subject(username)
                .issuedAt(Instant.ofEpochSecond(1733490155))
                .claim("userId", userId)
                .claim("username", username)
                .claim("email", email)
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        JwtEncoderParameters parameters = JwtEncoderParameters.from(claims);
        jwtToken = jwtEncoder.encode(parameters).getTokenValue();
    }
}
