package com.tskrypko.authentication.config.integration;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.*;

import static org.junit.jupiter.api.Assertions.*;

public class TestJwtToken extends BaseIntegrationTest {

    public void tearDown() {
        // not needed in this class
    }

    @Test
    public void givenCreatedTestJwt_shouldBeValid() {
        assertNotNull(jwtToken);
        assertNotNull(jwtDecoder);

        Jwt decodedJwt = jwtDecoder.decode(jwtToken);
        assertEquals(decodedJwt.getSubject(), username);
        assertEquals(decodedJwt.getClaim("userId"), userId);
        assertEquals(decodedJwt.getClaim("username"), username);
        assertEquals(decodedJwt.getClaim("email"), email);
    }
}
