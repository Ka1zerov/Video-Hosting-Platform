package com.tskrypko.authentication.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.http.MediaType;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RegisteredClientRepository clientRepository;

    @Test
    public void protectedEndpoints_shouldRequireAuthentication() throws Exception {
        // Test protected endpoints
        mockMvc.perform(MockMvcRequestBuilders.get("/api/protected-resource"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void corsConfiguration_shouldAllowConfiguredOrigins() throws Exception {
        MockHttpServletResponse response = mockMvc.perform(MockMvcRequestBuilders.options("/api/auth/some-endpoint")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andReturn().getResponse();

        assertTrue(response.getHeaderNames().contains("Access-Control-Allow-Origin"));
        assertEquals("http://localhost:3000", response.getHeader("Access-Control-Allow-Origin"));
    }

    @Test
    public void registeredClient_shouldBeConfiguredCorrectly() {
        // Verify registered client configuration
        var client = clientRepository.findByClientId("public-client");

        assertNotNull(client);
        assertTrue(client.getAuthorizationGrantTypes().stream()
                .anyMatch(grant -> grant.getValue().equals("authorization_code")));
        assertTrue(client.getAuthorizationGrantTypes().stream()
                .anyMatch(grant -> grant.getValue().equals("refresh_token")));
        assertTrue(client.getClientSettings().isRequireProofKey());
        assertFalse(client.getClientSettings().isRequireAuthorizationConsent());
    }

    @Test
    public void localProfile_shouldDisableHttps() throws Exception {
        MockHttpServletResponse response = mockMvc.perform(MockMvcRequestBuilders.get("/login"))
                .andReturn().getResponse();

        assertFalse(response.containsHeader("Strict-Transport-Security"));
    }

    @Test
    public void authorizationServerEndpoints_shouldBeAvailable() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/.well-known/openid-configuration"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }
}
