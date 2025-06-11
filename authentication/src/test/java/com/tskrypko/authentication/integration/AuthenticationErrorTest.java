package com.tskrypko.authentication.integration;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest
@AutoConfigureMockMvc
public class AuthenticationErrorTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void login_withInvalidCredentials_shouldRedirectWithError() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/login")
                .param("username", "invalid@example.com")
                .param("password", "wrongpassword")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error=invalid"));
    }

    @Test
    public void token_withInvalidClient_shouldReturnError() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/oauth2/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("grant_type", "authorization_code")
                .param("code", "valid-code")
                .param("redirect_uri", "http://localhost:3000")
                .param("client_id", "non-existent-client")
                .param("code_verifier", "CODE_VERIFIER"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void token_withInvalidCode_shouldReturnError() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/oauth2/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("grant_type", "authorization_code")
                .param("code", "invalid-code")
                .param("redirect_uri", "http://localhost:3000")
                .param("client_id", "public-client")
                .param("code_verifier", "CODE_VERIFIER"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void refreshToken_withMissingCookie_shouldReturnError() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/oauth2/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("grant_type", "refresh_token")
                .param("client_id", "public-client"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void refreshToken_withInvalidToken_shouldReturnError() throws Exception {
        Cookie invalidCookie = new Cookie("refresh_token", "invalid-token");

        mockMvc.perform(MockMvcRequestBuilders.post("/oauth2/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("grant_type", "refresh_token")
                .param("client_id", "public-client")
                .cookie(invalidCookie))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void invalidRedirectUri_shouldReturnError() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/oauth2/authorize")
                .param("response_type", "code")
                .param("client_id", "public-client")
                .param("redirect_uri", "http://malicious-site.com")
                .param("scope", "openid profile")
                .param("code_challenge", "CODE_CHALLENGE")
                .param("code_challenge_method", "S256"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void missingPKCE_shouldReturnError() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/oauth2/authorize")
                .param("response_type", "code")
                .param("client_id", "public-client")
                .param("redirect_uri", "http://localhost:3000")
                .param("scope", "openid profile"))
                .andExpect(status().isBadRequest());
    }
}
